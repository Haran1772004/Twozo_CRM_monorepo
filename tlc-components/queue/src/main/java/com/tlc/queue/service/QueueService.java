package com.tlc.queue.service;

import com.tlc.commons.service.Service;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.config.ProducerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.consumer.Consumer;
import com.tlc.queue.internal.TopicConfig;
import com.tlc.queue.producer.Producer;

/**
 * @author Abishek
 * @version 1.0
 */
public interface QueueService extends Service {

    /**
     * Creates a new producer
     *
     * @param producerConfig Configuration to create a new producer
     * @return A new producer
     */
    <T> Producer<T> createProducer(final ProducerConfig producerConfig);

    /**
     * Creates a new consumer
     *
     * @param consumerConfig Configuration to create a new consumer
     * @return A new consumer
     */
    Consumer createConsumer(final ConsumerConfig consumerConfig);

    /**
     * Creates a background consumer that handles frequent polling automatically.
     * The consumer object is created internally, running in the background by default to manage polling efficiently without requiring manual intervention.
     *
     * @param consumerConfig Configuration to create a new background consumer
     * @return A {@link BackgroundConsumer}
     */
    BackgroundConsumer createBackgroundConsumer(final ConsumerConfig consumerConfig);

    /**
     * Creates a topic if it doesn't already exist.
     *
     * @param topicConfig Represents the configuration of the topic.
     */
    void createTopicIfNotExist(final TopicConfig topicConfig);
}
