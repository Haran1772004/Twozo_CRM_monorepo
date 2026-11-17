package com.tlc.crm.core.workflow.publish;

import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.validator.TlcModel;

import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.crm.core.workflow.TriggerDataConverter;
import com.tlc.crm.core.workflow.model.TriggerInfo;
import com.tlc.crm.entity.fields.source.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Collection;
import java.util.Objects;

/**
 * Publishes the automation data into the consumer.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class TriggerDataPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerDataPublisher.class);

    private final TriggerDataConverter triggerDataConverter;
    private final QueueService queueService;

    private TriggerDataPublisher() {
        this.triggerDataConverter = TriggerDataConverter.getInstance();
        this.queueService = QueueServiceProvider.get();
    }

    private static final class Instance {
        private static final TriggerDataPublisher INSTANCE = new TriggerDataPublisher();
    }

    /**
     * Gets the instance of {@link TriggerDataPublisher}.
     *
     * @return {@link TriggerDataPublisher} instance.
     */
    public static TriggerDataPublisher getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Publishes the trigger event to the messaging queue with required data.
     *
     * @param orgId       Refers the organization id.
     * @param userId      Refers the user id.
     * @param models      Refers the models to be published.
     * @param triggerInfo Refers the {@link TriggerInfo} details.
     */
    public void publish(final Long orgId, final Long userId, final Collection<? extends TlcModel> models,
                        final TriggerInfo triggerInfo) {
        final Source triggerSource = triggerInfo.source();
        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:%s:%s:%s", orgId, triggerSource,
                models.stream().map(TlcModel::id).filter(Objects::nonNull).toList(), userId).getBytes());
        final SubscriptionTopic triggerInitiatorTopic = CoreQueueTopics.getTriggerInitiatorTopic(QueueModel.Type.KAFKA);
        final KafkaProducerConfig triggerInitiatorProducerConfig = new KafkaProducerConfig(triggerInitiatorTopic);

        try (final Producer<byte[]> triggerInitiatorProducer = queueService.createProducer(triggerInitiatorProducerConfig)) {
            triggerInitiatorProducer.send(new StreamMessage<>(requestId, triggerInitiatorTopic,
                    triggerDataConverter.convertToJson(orgId, models, triggerInfo).getBytes()));
        }
        LOGGER.info("Workflow Execution: Trigger published for Org ID: {}, Source: {}", orgId, triggerSource);
    }
}
