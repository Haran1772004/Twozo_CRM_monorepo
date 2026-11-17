package com.tlc.crm.common.queue;

import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;

/**
 * Utility to get the subscription topics in common module
 *
 * @author sundar
 * @version 1.0
 */
public final class CommonQueueTopics {

    private static final SubscriptionTopicHolder SUBSCRIPTION_TOPIC_HOLDER = SubscriptionTopicHolder.getInstance();

    private static final String DATA_EXPORTER = "DATA_EXPORTER";
    private static final String ORG_INITIATOR = "ORG_INITIATOR";
    private static final String USER_DATA_MIGRATOR = "USER_DATA_MIGRATOR";

    private CommonQueueTopics() {}

    /**
     * Gets the org initiator {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the org initiator {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getOrgInitiatorTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, ORG_INITIATOR);
    }

    /**
     * Gets the data exporter {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the data exporter {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getDataExporterTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, DATA_EXPORTER);
    }

    /**
     * Gets the user data migrator {@link SubscriptionTopic}
     *
     * @param type Represents the queue type
     * @return the user data migrator {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getUserDataMigratorTopic(final QueueModel.Type type) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(type, USER_DATA_MIGRATOR);
    }
}