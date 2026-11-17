package com.tlc.email.server.internal.webhook.receiver.manager;

import com.tlc.email.common.model.address.SyncAddressInfo;

import com.tlc.email.server.internal.WritableManager;
import com.tlc.email.server.internal.webhook.receiver.EmailWebhookReceiver;

import java.util.Collection;
import java.util.Optional;

/**
 * Handles email webhook subscriptions. Used to manage subscriptions for email services.
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public interface EmailWebHookReceiverManager extends WritableManager<EmailWebhookReceiver>  {

    /**
     * Gets teh instance of {@link EmailWebHookReceiverManager}.
     *
     * @return {@link EmailWebHookReceiverManager}
     */
    static EmailWebhookReceiverManagerImpl get() {
        return EmailWebhookReceiverManagerImpl.getInstance();
    }

    /**
     * Checks if the subscription is expired.
     *
     * @param subscriptionId the ID of the subscription to check
     */
    boolean isExpired(final Long subscriptionId);

    /**
     * Gets the subscription id for the given email ID
     *
     * @param emailId the reference of emailAddress
     */
    Optional<String> getSubscriptionId(final Long emailId);

    /**
     * Checks whether the given email reference is already unsubscribed or not.
     *
     * @param emailId the reference of email address
     * @return true, if the email is unsubscribed; otherwise false
     */
    boolean isUnsubscribed(Long emailId);

    /**
     * Retrieves a {@link Collection} of {@link EmailWebhookReceiver} that are eligible for renewal.
     *
     * @param limit Refers the limit
     * @return renewable webhook receivers
     */
    Collection<EmailWebhookReceiver> getRenewableWebhookReceivers(final int limit);

    /**
     * Checks whether the emailAddress is already registered in webhook or not.
     *
     * @param emailAddress Represents the emailAddress
     * @return true if the emailAddress is already exists;otherwise false
     */
    boolean exists(final String emailAddress);

    /**
     * Gets the {@link EmailWebhookReceiver} for the given email address.
     *
     * @param emailAddress Represents the email address
     * @return {@link EmailWebhookReceiver}
     */
    Optional<EmailWebhookReceiver> get(final String emailAddress);

    /**
     * Gets the {@link EmailWebhookReceiver} for the given email address.
     *
     * @param subscriptionId Represents the subscription ID
     * @return {@link EmailWebhookReceiver}
     */
    Optional<SyncAddressInfo> getSyncAddressInfo(final String subscriptionId);
}
