package com.tlc.crm.core.queue;

import com.tlc.queue.QueueModel;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;

/**
 * Utility to get the subscription topics in core module
 *
 * @author sundar
 * @version 1.0
 */
public final class CoreQueueTopics {

    private static final SubscriptionTopicHolder SUBSCRIPTION_TOPIC_HOLDER = SubscriptionTopicHolder.getInstance();

    private static final String DATA_IMPORTER = "DATA_IMPORTER";
    private static final String DATA_REVERT = "DATA_REVERT";
    private static final String CONTACT_CREATE_EVENT = "CONTACT_CREATE_EVENT";
    private static final String CONTACT_DELETE_EVENT = "CONTACT_DELETE_EVENT";
    private static final String CONTACT_PUSH_EVENT = "CONTACT_PUSH_EVENT";
    private static final String CONTACT_STOP_SYNC_EVENT = "CONTACT_STOP_SYNC_EVENT";
    private static final String CONTACT_UPDATE_EVENT = "CONTACT_UPDATE_EVENT";
    private static final String INDEX_ACTION_PERFORMER = "INDEX_ACTION_PERFORMER";
    private static final String WEBFORM_SUBMIT_EVENT = "WEBFORM_SUBMIT_EVENT";
    private static final String NOTIFICATION_STORAGE_EVENT = "NOTIFICATION_STORAGE_EVENT";
    private static final String ACTION_INITIATOR = "ACTION_INITIATOR";
    private static final String TRIGGER_INITIATOR = "TRIGGER_INITIATOR";
    private static final String ANALYTICS_EXPORT = "ANALYTICS_EXPORT";
    private static final String CONTACT_CREATED = "CONTACT_CREATED";
    private static final String COMPANY_CREATED = "COMPANY_CREATED";
    private static final String DEAL_CREATED = "DEAL_CREATED";
    private static final String EMAIL_ALERT_EVENT = "EMAIL_ALERT_EVENT";
    private static final String QUOTA_DEAL_EVENT = "QUOTA_DEAL_EVENT";
    private static final String QUOTA_EVENT = "QUOTA_EVENT";
    private static final String ORG_ONBOARDING_EVENT = "ORG_ONBOARDING_EVENT";
    private static final String MARKETING_LIST_CONTACT_TRANSFER_EVENT = "MARKETING_LIST_CONTACT_TRANSFER_EVENT";
    private static final String SEGMENT_RECOMPUTE_REQUESTED = "SEGMENT_RECOMPUTE_REQUESTED";
    private static final String EMAIL_ACTION = "EMAIL_ACTION";

    private CoreQueueTopics() {
    }

    /**
     * Gets the data importer {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the data importer {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getDataImporterTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, DATA_IMPORTER);
    }

    /**
     * Gets the data revert {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the data revert {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getDataRevertTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, DATA_REVERT);
    }

    /**
     * Gets the contact create event {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the contact create event {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getContactCreateEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, CONTACT_CREATE_EVENT);
    }

    /**
     * Gets the contact delete event {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the contact delete event {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getContactDeleteEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, CONTACT_DELETE_EVENT);
    }

    /**
     * Gets the contact push event {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the contact push event {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getContactPushEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, CONTACT_PUSH_EVENT);
    }

    /**
     * Gets the contact stop sync event {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the contact stop sync event {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getContactStopSyncEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, CONTACT_STOP_SYNC_EVENT);
    }

    /**
     * Gets the contact update event {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the contact update event {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getContactUpdateEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, CONTACT_UPDATE_EVENT);
    }

    /**
     * Gets the index action performer {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the index action performer {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getIndexActionPerformerTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, INDEX_ACTION_PERFORMER);
    }

    /**
     * Gets the web form submit event {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the web form submit event {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getWebFormSubmitEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, WEBFORM_SUBMIT_EVENT);
    }

    /**
     * Gets the notification storage event {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the notification storage event {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getNotificationStorageEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, NOTIFICATION_STORAGE_EVENT);
    }

    /**
     * Gets the action initiator {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the action initiator {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getActionInitiatorTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, ACTION_INITIATOR);
    }

    /**
     * Gets the trigger initiator {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the trigger initiator {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getTriggerInitiatorTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, TRIGGER_INITIATOR);
    }

    public static SubscriptionTopic getAnalyticsExporterTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, ANALYTICS_EXPORT);
    }

    /**
     * Gets the contact created {@link SubscriptionTopic}.
     *
     * @param queueType Represents the queue type.
     * @return The contact created {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getContactCreatedTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, CONTACT_CREATED);
    }

    /**
     * Gets the company created {@link SubscriptionTopic}.
     *
     * @param queueType Represents the queue type.
     * @return The company created {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getCompanyCreatedTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, COMPANY_CREATED);
    }

    /**
     * Gets the deal created {@link SubscriptionTopic}.
     *
     * @param queueType Represents the queue type.
     * @return The deal created {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getDealCreatedTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, DEAL_CREATED);
    }

    /**
     * <p>
     * Gets the email alert {@link SubscriptionTopic}
     * </p>
     *
     * @param queueType Represents the queue type.
     * @return the email alert {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getEmailAlertTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, EMAIL_ALERT_EVENT);
    }

    /**
     * Gets the quota deal event {@link SubscriptionTopic}.
     *
     * @param queueType Represents the queue type.
     * @return The {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getQuotaDealEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, QUOTA_DEAL_EVENT);
    }

    /**
     * Gets the quota event {@link SubscriptionTopic}.
     *
     * @param queueType Represents the queue type.
     * @return The {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getQuotaEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, QUOTA_EVENT);
    }

    /**
     * <p>
     * Gets the org onboard event's {@link SubscriptionTopic}.
     * </p>
     *
     * @param queueType Represents the queue type.
     * @return {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getOrgOnboardingEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, ORG_ONBOARDING_EVENT);
    }

    /**
     * Gets the marketing list contact transfer event {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the marketing list contact transfer event {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getMarketingListContactTransferEventTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, MARKETING_LIST_CONTACT_TRANSFER_EVENT);
    }

    /**
     * <p>
     * Gets the segment recompute {@link SubscriptionTopic}.
     * </p>
     *
     * @param queueType Represents the queue type
     * @return the segment recompute {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getSegmentRecomputeTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, SEGMENT_RECOMPUTE_REQUESTED);
    }

    /**
     * Gets the email action {@link SubscriptionTopic}.
     *
     * @param queueType Represents the queue type.
     * @return The email action {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getEmailActionTopic(final QueueModel.Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, EMAIL_ACTION);
    }
}