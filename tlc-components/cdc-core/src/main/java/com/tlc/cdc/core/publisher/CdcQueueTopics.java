package com.tlc.cdc.core.publisher;

import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;

/**
 * Utility to get the subscription topics in CDC module.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CdcQueueTopics {

    private static final SubscriptionTopicHolder SUBSCRIPTION_TOPIC_HOLDER = SubscriptionTopicHolder.getInstance();
    private static final String AGGREGATED_CDC_EVENT = "AGGREGATED_CDC_EVENT";

    private CdcQueueTopics() {
    }

    /**
     * Gets the CDC {@link SubscriptionTopic}
     *
     * @param queueType Refers the queue type.
     * @return The CDC {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic aggregatedCdcTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, AGGREGATED_CDC_EVENT);
    }

}
