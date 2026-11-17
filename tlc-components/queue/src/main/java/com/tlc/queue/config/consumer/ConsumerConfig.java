package com.tlc.queue.config.consumer;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamListener;

import java.util.Collection;

/**
 * A config holder that holds the consumer related settings
 *
 * @author Ashok Selvakumar
 * @version 1.0
 */
public interface ConsumerConfig extends QueueModel {

    /**
     * Fetches the topics that have to be subscribed by the consumer
     *
     * @return Subscription topics
     */
    Collection<SubscriptionTopic> getSubscriptionTopics();

    /**
     * Indicates whether the consumer has to read the messages from the start or not
     *
     * @return If it's true, consumer will read the messages from the last read position
     */
    default boolean readFromLast() {
        return true;
    }

    /**
     * Fetches the consumer listener
     *
     * @return {@link StreamListener}
     */
    <T> StreamListener<T> getListener();

    /**
     * Gets the consumer poll configuration
     *
     * @return the {@link PollConfig}
     */
    PollConfig getPollConfig();

    /**
     * Checks whether the consumer can commit the offset automatically or not
     *
     * @return true if the auto commit is enabled; false otherwise
     */
    boolean isAutoCommitEnabled();

    /**
     * Represents the record polling configuration of the consumer
     *
     * @param maxRecords   the max number of records received on a batch
     * @param pollDuration specifies the frequency of record polling, with the duration given in milliseconds
     * @param maxQueueSize the maximum number of messages that can be processed and received in the queue
     */
    record PollConfig(int maxRecords, int pollDuration, int maxQueueSize) {

        public PollConfig(int maxRecords, int maxQueueSize) {
            this(maxRecords, 5000, maxQueueSize);
        }
    }
}