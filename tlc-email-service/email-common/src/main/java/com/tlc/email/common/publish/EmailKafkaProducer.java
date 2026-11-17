package com.tlc.email.common.publish;

import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.kafka.KafkaProducerConfig;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.provider.QueueServiceProvider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages singleton instances of Kafka producers per subscription topic.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class EmailKafkaProducer {

    private final Map<SubscriptionTopic, Producer<byte[]>> PRODUCER_REGISTRY = new ConcurrentHashMap<>();

    private EmailKafkaProducer() {
    }

    /**
     * <p>
     * Holds the single instance.
     * </p>
     */
    private static class Instance {
        private static final EmailKafkaProducer INSTANCE = new EmailKafkaProducer();
    }

    public static EmailKafkaProducer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Returns a singleton Producer instance for the given topic.
     *
     * @param subscriptionTopic The subscription topic.
     * @return The singleton producer instance.
     */
    public Producer<byte[]> getProducer(final SubscriptionTopic subscriptionTopic) {
        return PRODUCER_REGISTRY.computeIfAbsent(subscriptionTopic, topic -> {
            final KafkaProducerConfig config = new KafkaProducerConfig(topic);

            return QueueServiceProvider.get().createProducer(config);
        });
    }

    /**
     * Closes all producers gracefully.
     */
    public void close() {
        PRODUCER_REGISTRY.values().forEach(Producer::close);
        PRODUCER_REGISTRY.clear();
    }
}
