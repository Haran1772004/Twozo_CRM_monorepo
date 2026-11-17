package com.tlc.email.server.internal.webhook.receiver;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.webhook.receiver.WebhookReceiver;

import com.tlc.email.common.model.EmailModel;
import com.tlc.email.common.model.address.SyncAddressInfo;

import com.tlc.email.server.internal.status.EmailWebhookErrorCodes;
import com.tlc.email.server.internal.webhook.receiver.manager.EmailWebHookReceiverManager;
import com.tlc.email.server.internal.webhook.receiver.manager.EmailWebhookReceiverManagerImpl;
import com.tlc.email.server.internal.webhook.receiver.provider.google.GoogleEmailWebhookReceiver;
import com.tlc.email.server.internal.webhook.receiver.provider.outlook.OutlookEmailWebhookReceiver;

import com.tlc.sync.service.oauth.model.provider.EmailProvider;

import com.tlc.sync.service.oauth.service.provider.ProviderService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Handles the email webhook operations across different providers.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see WebhookReceiver
 */
public abstract class EmailWebhookReceiver implements WebhookReceiver, EmailModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailWebhookReceiver.class);

    private final EmailWebhookReceiverManagerImpl emailWebHookReceiverManager;

    /**
     * <p>
     * Gets the appropriate service based on the email address
     * </p>
     *
     * @param syncAddressInfo Represents the {@link SyncAddressInfo}
     * @return {@link EmailWebhookReceiver} relevant to the provided email address
     */
    public static EmailWebhookReceiver getService(final SyncAddressInfo syncAddressInfo) {
        final EmailProvider emailProvider = ProviderService.getEmailProviderService()
                .findProvider(new com.tlc.sync.service.oauth.model.EmailAddress(syncAddressInfo.getEmailAddress()));

        return switch (emailProvider) {
            case GOOGLE -> new GoogleEmailWebhookReceiver(syncAddressInfo);
            case OUTLOOK -> new OutlookEmailWebhookReceiver(syncAddressInfo);
            default -> null;
        };
    }

    /**
     * Returns the reference of the webhook.
     */
    private Long id;

    /**
     * Returns the expiry time of the webhook.
     */
    protected Long expiryTime;
    protected SyncAddressInfo emailAddress;

    protected EmailWebhookReceiver() {
        this.emailWebHookReceiverManager = EmailWebHookReceiverManager.get();
    }

    public EmailWebhookReceiverManagerImpl getEmailWebHookReceiverManager() {
        return emailWebHookReceiverManager;
    }

    public Long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(final Long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public SyncAddressInfo getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(final SyncAddressInfo emailAddress) {
        this.emailAddress = emailAddress;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Returns the pk value of the webhook.
     *
     * @return the email ID
     */
    @Override
    public Long id() {
        return id;
    }

    /**
     * Checks whether the given email reference is already unsubscribed or not.
     *
     * @param emailId the reference of email address
     * @return true, if the email is unsubscribed; otherwise false
     */
    public boolean isUnsubscribed(final Long emailId) {
        return emailWebHookReceiverManager.isUnsubscribed(emailId);
    }

    /**
     * Returns the {@link EmailProvider}
     *
     * @return {@link EmailProvider}
     */
    public abstract EmailProvider getProvider();

    /**
     * Triggers webhook subscription creation for this email account.
     * Delegates the subscription logic to the EmailWebHookReceiverManager.
     */
    protected void afterSubscription() {
        final String address = emailAddress.getEmailAddress();

        LOGGER.debug("Webhook Receiver: Triggered after subscription creation");

        try {

            if (emailWebHookReceiverManager.exists(address)) {
                final Optional<EmailWebhookReceiver> emailWebhookReceiver = emailWebHookReceiverManager.get(address);

                if (emailWebhookReceiver.isPresent()) {
                    final EmailWebhookReceiver webhookReceiver = emailWebhookReceiver.get();

                    this.id = webhookReceiver.id();
                    emailWebHookReceiverManager.update(this);
                }
            } else {
                emailWebHookReceiverManager.create(this);
            }
            LOGGER.debug("Webhook Receiver: Subscription process successfully completed");
        } catch (Exception e) {
            LoggerUtil.logError(LOGGER, e, "Webhook Receiver: DB load action failed for subscription", e);

            throw ErrorCode.get(EmailWebhookErrorCodes.SUBSCRIPTION_ACTION_FAILED, e);
        }
    }

    /**
     * Triggers webhook un-subscription for this email account.
     * Delegates the removal logic to the EmailWebHookReceiverManager using the email ID.
     */
    protected void afterUnSubscription() {
        emailWebHookReceiverManager.delete(emailId());
    }

    /**
     * Gets the reference of email.
     *
     * @return reference of email
     */
    private Long emailId() {
        return emailAddress.id();
    }
}
