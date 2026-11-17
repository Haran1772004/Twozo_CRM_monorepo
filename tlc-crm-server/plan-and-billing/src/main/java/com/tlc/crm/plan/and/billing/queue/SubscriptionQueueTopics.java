package com.tlc.crm.plan.and.billing.queue;

import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;

/**
 * Utility to get the subscription topics in plans and billings module.
 *
 * @author Sathish
 * @version 1.0
 */
public final class SubscriptionQueueTopics {

    private static final SubscriptionTopicHolder SUBSCRIPTION_TOPIC_HOLDER = SubscriptionTopicHolder.getInstance();

    private static final String PAYER_CREATE_EVENT = "PAYER_CREATE_EVENT";

    private SubscriptionQueueTopics() {
    }

    /**
     * Gets the payer creator {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the payer creator {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getPayerCreateEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, PAYER_CREATE_EVENT);
    }
}