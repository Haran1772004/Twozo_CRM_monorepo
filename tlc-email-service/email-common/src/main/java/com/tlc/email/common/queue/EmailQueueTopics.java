package com.tlc.email.common.queue;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.model.SyncJobType;

import com.tlc.notification.internal.status.EmailErrorCodes;
import com.tlc.queue.SubscriptionTopicHolder;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.QueueModel.Type;

import java.util.Objects;

/**
 * <p>
 * Utility to get the subscription topics in email notification module
 * </p>
 *
 * @author kowsalyaSP
 * @version 1.0
 */
public final class EmailQueueTopics {

    private static final SubscriptionTopicHolder SUBSCRIPTION_TOPIC_HOLDER = SubscriptionTopicHolder.getInstance();

    private static final String EMAIL_SEND_REQUESTED = "EMAIL_SEND_REQUESTED";
    private static final String SYNC_STATUS_UPDATE_REQUEST = "SYNC_STATUS_UPDATE_REQUEST";

    private static final String EMAIL_SYNC_RETRIEVED = "EMAIL_SYNC_RETRIEVED";
    private static final String SMART_BCC_RETRIEVED = "SMART_BCC_RETRIEVED";
    private static final String INBOUND_EMAIL_RECEIVED = "INBOUND-EMAIL-RECEIVED";
    private static final String AUTO_SYNC_RETRIEVED = "AUTO_SYNC_RETRIEVED";

    private static final String SYNC_DATA_LOADER = "SYNC_DATA_LOADER";
    private static final String AUTO_SYNC_DATA_LOADER = "AUTO_SYNC_DATA_LOADER";
    private static final String SMART_BCC_DATA_LOADER = "SMART_BCC_DATA_LOADER";
    private static final String TRANSACTIONAL_MAIL_SEND_REQUESTED = "TRANSACTIONAL_MAIL_SEND_REQUESTED";
    private static final String SEND_REQUEST_TO_PROVIDER = "SEND_REQUEST_TO_PROVIDER";
    private static final String DELETE_REQUEST_TO_PROVIDER = "DELETE_REQUEST_TO_PROVIDER";
    private static final String UPDATE_REQUEST_TO_PROVIDER = "UPDATE_REQUEST_TO_PROVIDER";

    private static final String DOMAIN_REQUEST_TO_PROVIDER = "DOMAIN_REQUEST_TO_PROVIDER";
    private static final String DOMAIN_ACTION_REQUESTED = "DOMAIN_ACTION_REQUESTED";
    private static final String WEBHOOK_NOTIFICATION_RECEIVED = "WEBHOOK_NOTIFICATION_RECEIVED";

    private EmailQueueTopics() {
    }

    /**
     * Gets the email send {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the email send {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getEmailSendTopic(final Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, EMAIL_SEND_REQUESTED);
    }

    /**
     * Gets the sync status {@link SubscriptionTopic}
     *
     * @param queueType Represents the queue type
     * @return the sync status {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getSyncStatusTopic(final Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, SYNC_STATUS_UPDATE_REQUEST);
    }

    /**
     * Gets the data retrieval {@link SubscriptionTopic}.
     *
     * @param queueType   Represents the queue type.
     * @param syncJobType Represents the email background job type.
     * @return The data retrieval {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getDataRetrieveTopic(final Type queueType, final SyncJobType syncJobType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, getEvent(syncJobType, EventAction.RETRIEVE));
    }

    /**
     * <p>
     * Gets the data load {@link SubscriptionTopic}.
     * </p>
     *
     * @param queueType   Represents the queue type.
     * @param syncJobType Represents the email background job type.
     * @return The data load {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getDataLoadTopic(final Type queueType, final SyncJobType syncJobType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, getEvent(syncJobType, EventAction.LOAD));
    }

    /**
     * Gets the {@link SubscriptionTopic} for amazon ses send event
     *
     * @param queueType Represents the queue type
     * @return the {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getTransactionalMailTopic(final Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, TRANSACTIONAL_MAIL_SEND_REQUESTED);
    }

    /**
     * <p>
     * Determines the event name based on the email background job type and the event action.
     * </p>
     *
     * @param syncJobType The type of email background job (e.g., SYNC, AUTO_SYNC, SMART_BCC).
     * @param eventAction The action to be performed (RETRIEVE or LOAD).
     * @return The event name as a string.
     */
    private static String getEvent(final SyncJobType syncJobType, final EventAction eventAction) {
        return switch (syncJobType) {
            case SYNC -> EventAction.RETRIEVE == eventAction ? EMAIL_SYNC_RETRIEVED : SYNC_DATA_LOADER;
            case SMART_BCC -> EventAction.RETRIEVE == eventAction ? SMART_BCC_RETRIEVED : SMART_BCC_DATA_LOADER;
            default -> throw ErrorCode.get(EmailErrorCodes.INVALID_SYNC_JOB_TYPE);
        };
    }

    /**
     * <p>
     * Resolves the {@link SubscriptionTopic} for auto-sync operations based on
     * the queue type and the specified event action.
     * </p>
     *
     * @param queueType   The type of queue for which the subscription topic
     *                    is to be resolved.
     * @param eventAction The action associated with the auto-sync operation.
     * @return The resolved {@link SubscriptionTopic}.
     */
    public static SubscriptionTopic getAutoSyncTopic(final Type queueType, final EventAction eventAction) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(
                queueType, Objects.equals(eventAction, EventAction.RETRIEVE) ? AUTO_SYNC_RETRIEVED : AUTO_SYNC_DATA_LOADER
        );
    }

    /**
     * Gets the {@link SubscriptionTopic} for send request provider event
     *
     * @param queueType Represents the queue type
     * @return the {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getSendRequestProviderTopic(final Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, SEND_REQUEST_TO_PROVIDER);
    }

    /**
     * Gets the {@link SubscriptionTopic} for delete request provider event
     *
     * @param queueType Represents the queue type
     * @return the {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getDeleteRequestProviderTopic(final Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, DELETE_REQUEST_TO_PROVIDER);
    }

    /**
     * Gets the {@link SubscriptionTopic} for update request provider event
     *
     * @param queueType Represents the queue type
     * @return the {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getUpdateRequestProviderTopic(final Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, UPDATE_REQUEST_TO_PROVIDER);
    }

    /**
     * Gets the {@link SubscriptionTopic} for webhook retrieve event.
     *
     * @param queueType Represents the queue type
     * @return the {@link SubscriptionTopic}
     */
    public static SubscriptionTopic getWebhookEventTopic(final Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, WEBHOOK_NOTIFICATION_RECEIVED);
    }

    /**
     * <p>
     * Gets the {@link SubscriptionTopic} used specifically for inbound email received.
     * </p>
     *
     * @param queueType Represents the queue type for which the topic is required
     * @return the {@link SubscriptionTopic} associated with inbound event received
     */
    public static SubscriptionTopic getInboundReceiveTopic(final Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, INBOUND_EMAIL_RECEIVED);
    }

    /**
     * <p>
     * Returns the subscription topic for domain action requested events.
     * </p>
     *
     * @param queueType The type of queue for which the subscription topic is requested.
     * @return The {@link SubscriptionTopic} corresponding to domain action requested.
     */
    public static SubscriptionTopic getDomainRequestToProviderTopic(final Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, DOMAIN_REQUEST_TO_PROVIDER);
    }

    /**
     * <p>
     * Returns the subscription topic for domain data load events.
     * </p>
     *
     * @param queueType The type of queue for which the subscription topic is requested.
     * @return The {@link SubscriptionTopic} corresponding to domain data load requested.
     */
    public static SubscriptionTopic getActionRequestedTopic(final Type queueType) {
        return SUBSCRIPTION_TOPIC_HOLDER.getSubscriptionTopic(queueType, DOMAIN_ACTION_REQUESTED);
    }

    /**
     * <p>
     * Represents the possible actions for events, such as retrieving or loading data.
     * </p>
     *
     * @author Mohamed Yasar k
     * @version 1.0
     */
    public enum EventAction {
        RETRIEVE,
        LOAD,
        ;
    }
}