package com.tlc.social.common.queue;

import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;

/**
 * Utility to get the subscription topics of the social module
 *
 * @author Maharaja S
 * @version 1.0
 */
public final class SocialQueueTopics {

    private static final SubscriptionTopicHolder SUBSCRIPTION_TOPIC_HOLDER = SubscriptionTopicHolder.getInstance();

    private static final String FB_WEBHOOK_DATA_CONSUMER = "FB_WEBHOOK_DATA_CONSUMER";
    private static final String FB_WEBHOOK_RESPONSE_PROCESSOR = "FB_WEBHOOK_RESPONSE_PROCESSOR";
    private static final String INSTAGRAM_WEBHOOK_DATA_CONSUMER = "INSTAGRAM_WEBHOOK_DATA_CONSUMER";
    private static final String INSTAGRAM_WEBHOOK_RESPONSE_PROCESSOR = "INSTAGRAM_WEBHOOK_RESPONSE_PROCESSOR";
    private static final String WA_WEBHOOK_DATA_CONSUMER = "WA_WEBHOOK_DATA_CONSUMER";
    private static final String WA_WEBHOOK_RESPONSE_PROCESSOR = "WA_WEBHOOK_RESPONSE_PROCESSOR";
    private static final String WA_TEMPLATE_SYNC_PROCESSOR = "WA_TEMPLATE_SYNC_PROCESSOR";

    private SocialQueueTopics() {
    }

    /**
     * Gets the fb webhook payload consume {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the fb webhook payload process {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getFBWebhookDataConsumeTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, FB_WEBHOOK_DATA_CONSUMER);
    }

    /**
     * Gets the fb webhook response process {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the fb webhook data load {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getFBWebhookResponseProcessTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, FB_WEBHOOK_RESPONSE_PROCESSOR);
    }

    /**
     * <p>
     * Gets {@link SubscriptionTopic}.
     * </p>
     *
     * @param queueType {@link QueueModel.Type};
     * @return {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getInstagramWebhookDataConsumeTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, INSTAGRAM_WEBHOOK_DATA_CONSUMER);
    }

    /**
     * <p>
     * Gets {@link SubscriptionTopic}.
     * </p>
     *
     * @param queueType {@link QueueModel.Type};
     * @return {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getInstagramWebhookResponseProcessTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, INSTAGRAM_WEBHOOK_RESPONSE_PROCESSOR);
    }

    /**
     * Gets the WhatsApp webhook payload consume {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the WhatsApp webhook payload process {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getWAWebhookDataConsumeTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, WA_WEBHOOK_DATA_CONSUMER);
    }

    /**
     * Gets the WhatsApp webhook response process {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the WhatsApp webhook data load {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getWAWebhookResponseProcessTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, WA_WEBHOOK_RESPONSE_PROCESSOR);
    }

    /**
     * Gets the WhatsApp template sync {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the WhatsApp template sync process {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getWATemplateSyncTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, WA_TEMPLATE_SYNC_PROCESSOR);
    }
}