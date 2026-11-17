package com.tlc.queue.internal.kafka.producer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.executor.context.Context;
import com.tlc.commons.executor.context.ContextHolder;
import com.tlc.commons.executor.context.registry.ContextCodecRegistry;
import com.tlc.commons.executor.context.registry.ContextHolderRegistry;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.internal.ResourceManager;
import com.tlc.queue.internal.resource.QueueErrorCodes;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.internal.kafka.KafkaService;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.provider.QueueServiceProvider;

import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A producer that publishes message to the given topic
 *
 * @author Nithin
 * @version 1.0
 */
public class KafkaProducer implements Producer<byte[]> {

    private static final String DATA_KEY = "data";
    private static final String CONTEXTS_KEY = "contexts";
    private static final String TYPE_KEY = "type";
    private static final String VALUE_KEY = "value";

    private final KafkaProducerConfig kafkaProducerConfig;
    private final org.apache.kafka.clients.producer.KafkaProducer<String, byte[]> producer;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public KafkaProducer(final KafkaProducerConfig kafkaProducerConfig) {
        final KafkaService kafkaService = (KafkaService) QueueServiceProvider.get();

        this.kafkaProducerConfig = kafkaProducerConfig;
        this.producer = new org.apache.kafka.clients.producer.KafkaProducer<>(kafkaService.getClientInput());
        ResourceManager.getInstance().add(producer);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final StreamMessage<byte[]> streamMessage) {
        final JsonObject dataJson = Json.object();
        final ContextHolderRegistry contextHolderRegistry = ContextHolderRegistry.getInstance();

        dataJson.put(DATA_KEY, Arrays.toString(streamMessage.data()));

        if (!contextHolderRegistry.isEmpty()) {
            final ContextCodecRegistry contextCodecRegistry = ContextCodecRegistry.getInstance();

            if (!contextCodecRegistry.isEmpty()) {
                final JsonArray contextArray = Json.array();

                for (final ContextHolder<?> contextHolder : contextHolderRegistry.getContextHolders()) {

                    contextHolder.getContext().ifPresent(context -> {
                        final JsonObject contextJson = Json.object();
                        final Context.Type contextType = context.getType();

                        contextJson.put(TYPE_KEY, contextType.getName());
                        contextJson.put(VALUE_KEY, Arrays.toString(contextCodecRegistry.get(contextType).encode(context)));
                        contextArray.put(contextJson);
                    });
                }

                if (contextArray.size() > 0) {
                    dataJson.put(CONTEXTS_KEY, contextArray);
                }
            }
        }

        //TODO: need to complete the acknowledgement get
        try {
            producer.send(new ProducerRecord<>(kafkaProducerConfig.getTopic().getName(), streamMessage.id(), dataJson.getBytes())).get();
        } catch (InterruptedException | ExecutionException e) {
            throw ErrorCode.get(QueueErrorCodes.RECORD_PUBLISH_ACTION_FAILED, e);
        } finally {
            streamMessage.clean();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendWithoutContext(final StreamMessage<byte[]> streamMessage) {
        try {
            producer.send(new ProducerRecord<>(kafkaProducerConfig.getTopic().getName(), streamMessage.id(), streamMessage.data())).get();
        } catch (InterruptedException | ExecutionException e) {
            throw ErrorCode.get(QueueErrorCodes.RECORD_PUBLISH_ACTION_FAILED, e);
        } finally {
            streamMessage.clean();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        final Lock writeLock = lock.writeLock();

        try {
            if (!isClosed.get()) {
                writeLock.lock();

                if (!isClosed.get()) {
                    producer.close();
                    ResourceManager.getInstance().remove(producer);
                    isClosed.set(true);
                }
            }
        } finally {
            writeLock.unlock();
        }
    }
}