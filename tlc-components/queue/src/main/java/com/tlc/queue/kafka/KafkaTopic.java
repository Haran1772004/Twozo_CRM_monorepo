package com.tlc.queue.kafka;

import com.tlc.queue.config.SubscriptionTopic;

/**
 * Represents kafka topic
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public final class KafkaTopic extends KafkaModel implements SubscriptionTopic {

    private final String topicName;

    public KafkaTopic(final String name) {
        this.topicName = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.topicName;
    }
}
