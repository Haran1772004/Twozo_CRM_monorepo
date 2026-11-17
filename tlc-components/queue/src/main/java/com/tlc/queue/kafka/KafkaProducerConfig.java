package com.tlc.queue.kafka;

import com.tlc.queue.config.ProducerConfig;
import com.tlc.queue.config.SubscriptionTopic;

/**
 * A config holder that holds the kafka producer configurations
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public final class KafkaProducerConfig extends KafkaModel implements ProducerConfig {

    private final SubscriptionTopic topic;

    public KafkaProducerConfig(final SubscriptionTopic topic) {
        this.topic = topic;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SubscriptionTopic getTopic() {
        return topic;
    }

}
