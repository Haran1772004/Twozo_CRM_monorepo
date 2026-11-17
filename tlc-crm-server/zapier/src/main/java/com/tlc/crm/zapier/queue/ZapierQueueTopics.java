package com.tlc.crm.zapier.queue;

import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;

/**
 * Utility to get the subscription topics in zapier module.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ZapierQueueTopics {

    private static final SubscriptionTopicHolder SUBSCRIPTION_TOPIC_HOLDER = SubscriptionTopicHolder.getInstance();

    private static final String ZAP_EVENT_INITIATOR = "ZAP_EVENT_INITIATOR";

    /**
     * Gets the zap event {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type.
     * @return The zap event {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getEventInitiatorTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, ZAP_EVENT_INITIATOR);
    }

}
