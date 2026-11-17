package com.tlc.queue.producer;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Manages a store of producers mapped to subscription topics
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class ProducerStore {

    private final Map<SubscriptionTopic, Producer<byte[]>> kafkaProducerMap;

    private ProducerStore() {
        kafkaProducerMap = new ConcurrentHashMap<>();
    }

    private static class InstanceHolder {
        private static final ProducerStore INSTANCE = new ProducerStore();
    }

    /**
     * <p>
     * Gets the {@link ProducerStore} instance
     * </p>
     *
     * @return The {@link ProducerStore} instance
     */
    public static ProducerStore getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Fetches a producer for the given subscription topic.
     * </p>
     *
     * @param subscriptionTopic The subscription topic.
     * @return The singleton producer instance.
     */
    public Optional<Producer<byte[]>> getProducer(final SubscriptionTopic subscriptionTopic) {

        if (QueueModel.Type.KAFKA == subscriptionTopic.getType()) {
            final Producer<byte[]> producer = kafkaProducerMap.computeIfAbsent(subscriptionTopic, topic -> {
                final KafkaProducerConfig config = new KafkaProducerConfig(topic);

                return QueueServiceProvider.get().createProducer(config);
            });
            return Optional.ofNullable(producer);
        }

        return Optional.empty();
    }

    /**
     * Closes all producers gracefully.
     */
    public void close() {
        kafkaProducerMap.values().forEach(Producer::close);
        kafkaProducerMap.clear();
    }
}
