package com.tlc.queue;

import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.internal.SubscriptionTopicHolderImpl;

/**
 * Holder to hold the configured subscription topics
 *
 * @author sundar
 * @version 1.0
 */
public interface SubscriptionTopicHolder {

    /**
     * Gets the {@link SubscriptionTopicHolder} instance
     *
     * @return the {@link SubscriptionTopicHolder}
     */
    static SubscriptionTopicHolder getInstance() {
        return SubscriptionTopicHolderImpl.getInstance();
    }

    /**
     * Gets the {@link SubscriptionTopic} associated with the given queue model type and topic name
     *
     * @param type      Represents the queue model type
     * @param topicName Represents the topic name
     * @return the {@link SubscriptionTopic}
     */
    SubscriptionTopic getSubscriptionTopic(final QueueModel.Type type, final String topicName);
}