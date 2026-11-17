package com.tlc.crm.core.internal.event.index.publisher;

import com.tlc.crm.common.action.search.annotation.Searchable;
import com.tlc.crm.common.models.SearchIndex;
import com.tlc.crm.core.action.parser.DataParser;
import com.tlc.crm.common.fields.ActionFields;
import com.tlc.crm.common.models.Action;
import com.tlc.commons.util.JavaUtil;
import com.tlc.crm.core.action.parser.impl.NativeParser;

import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * <p>
 * Facilitates to publish index data into consumer.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public final class IndexDataPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IndexDataPublisher.class);

    private final DataParser dataParser;
    private final QueueService queueService;
    private static final int BATCH_SIZE = 5;
    private static final long MAX_BYTES = 1_048_576L;

    private IndexDataPublisher() {
        dataParser = NativeParser.getInstance();
        queueService = QueueServiceProvider.get();
    }

    private static class Instance {
        private static final IndexDataPublisher INSTANCE = new IndexDataPublisher();
    }

    /**
     * <p>
     * Gets the {@link IndexDataPublisher} Instance
     * </p>
     *
     * @return {@link IndexDataPublisher}
     */
    public static IndexDataPublisher getInstance() {
        return IndexDataPublisher.Instance.INSTANCE;
    }

    /**
     * <p>
     * Publishes index related data via {@link StreamMessage}.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param models Holds {@link Collection} of {@link TlcModel}.
     * @param action Dictates the course of action.
     */
    public void publishIndexData(final Long orgId, final Collection<? extends TlcModel> models, final Action action) {
        publishIndexData(orgId, models, action, null);
    }

    /**
     * <p>
     * Publishes index related data via {@link StreamMessage}.
     * </p>
     *
     * @param orgId         Represents the organisation id.
     * @param models        Holds {@link Collection} of {@link TlcModel}.
     * @param action        Dictates the course of action.
     * @param duplicateIds  Duplicate entity ids for {@link Action#MERGE}.
     */
    public void publishIndexData(final Long orgId, final Collection<? extends TlcModel> models, final Action action,
                                 final Collection<Long> duplicateIds) {

        if (Objects.nonNull(models)) {
            models.stream().findFirst().ifPresent(tlcModel -> {
                final Class<? extends TlcModel> tlcModelClass = tlcModel.getClass();

                if (tlcModelClass.isAnnotationPresent(Searchable.class)) {
                    final Searchable searchable = tlcModelClass.getAnnotation(Searchable.class);
                    final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s", orgId, tlcModel.id()).getBytes());
                    final SubscriptionTopic indexActionPerformerTopic = CoreQueueTopics.getIndexActionPerformerTopic(QueueModel.Type.KAFKA);
                    final KafkaProducerConfig indexActionPerformerProducerConfig = new KafkaProducerConfig(indexActionPerformerTopic);

                    try (final Producer<byte[]> indexActionPerformerProducer = queueService.createProducer(indexActionPerformerProducerConfig)) {
                        final SearchIndex searchIndex = searchable.index();
                        final Map<String, Object> payload = new HashMap<>();

                        payload.put(ActionFields.MODELS, models);
                        payload.put(ActionFields.ACTION, action);
                        payload.put(ActionFields.INDEX, searchIndex);

                        if (Objects.nonNull(duplicateIds)) {
                            payload.put(ActionFields.DUPLICATE_IDS, duplicateIds);
                        }
                        LOGGER.info("Indexing Record, Publishing: {} index, {} action, ID: {}, Org ID: {}", searchIndex.getName(), action.name(), tlcModel.id(), orgId);
                        indexActionPerformerProducer.send(new StreamMessage<>(requestId, indexActionPerformerTopic,
                                dataParser.serialize(payload)));
                    }
                }
            });
        }
    }

    /**
     * <p>
     * Publishes index data in batches of {@value BATCH_SIZE} via {@link StreamMessage}.
     * Falls back to per-record publishing if batch exceeds {@value MAX_BYTES} bytes.
     * </p>
     *
     * @param orgId  Represents the organisation id.
     * @param models Holds {@link Collection} of {@link TlcModel}.
     * @param action Dictates the course of action.
     */
    public void publishIndexDataInBatches(final Long orgId, final Collection<? extends TlcModel> models, final Action action) {
        if (Objects.nonNull(models)) {
            models.stream().findFirst().ifPresent(tlcModel -> {
                final Class<? extends TlcModel> tlcModelClass = tlcModel.getClass();

                if (tlcModelClass.isAnnotationPresent(Searchable.class)) {
                    final Searchable searchable = tlcModelClass.getAnnotation(Searchable.class);
                    final SearchIndex searchIndex = searchable.index();
                    final SubscriptionTopic topic = CoreQueueTopics.getIndexActionPerformerTopic(QueueModel.Type.KAFKA);

                    try (final Producer<byte[]> producer = queueService.createProducer(new KafkaProducerConfig(topic))) {
                        for (final Collection<? extends TlcModel> batch : JavaUtil.splitCollection(models, BATCH_SIZE)) {
                            final HashMap<String, Object> batchMap = new HashMap<>();

                            batchMap.put(ActionFields.MODELS, new ArrayList<>(batch));
                            batchMap.put(ActionFields.ACTION, action);
                            batchMap.put(ActionFields.INDEX, searchIndex);
                            final byte[] batchPayload = dataParser.serialize(batchMap);

                            if (MAX_BYTES >= batchPayload.length) {
                                final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s", orgId, batch.iterator().next().id()).getBytes());


                                try {
                                    LOGGER.info("Indexing Record by Bytes: Publishing: {} index, {} action, ID: {}, Org ID: {}", searchIndex.getName(), action.name(), batch.iterator().next().id(), orgId);
                                    producer.send(new StreamMessage<>(requestId, topic, batchPayload));
                                } catch (final Exception exception) {
                                    LOGGER.error("Indexing Record by Bytes: Batch send failed. Org ID: {}", orgId, exception);
                                    exception.printStackTrace();
                                }

                            } else {
                                for (final TlcModel record : batch) {
                                    final HashMap<String, Object> recordMap = new HashMap<>();

                                    recordMap.put(ActionFields.MODELS, new ArrayList<>(Collections.singletonList(record)));
                                    recordMap.put(ActionFields.ACTION, action);
                                    recordMap.put(ActionFields.INDEX, searchIndex);
                                    final byte[] recordPayload = dataParser.serialize(recordMap);

                                    if (MAX_BYTES < recordPayload.length) {
                                        LOGGER.warn("Indexing Record by Bytes: Record size exceeds 1 MB, Record ID : {} Size : {} bytes, skipping", record.id(), recordPayload.length);
                                        continue;
                                    }

                                    final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s", orgId, record.id()).getBytes());

                                    try {
                                        LOGGER.info(" Indexing Record by Bytes: Publishing: {} index, {} action, ID: {}, Org ID: {}", searchIndex.getName(), action.name(), record.id(), orgId);
                                        producer.send(new StreamMessage<>(requestId, topic, recordPayload));
                                    } catch (final Exception exception) {
                                        LOGGER.error("Indexing Record by Bytes: Record send failed. Org ID: {} Record ID: {}", orgId, record.id(), exception);
                                        exception.printStackTrace();
                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    }
}
