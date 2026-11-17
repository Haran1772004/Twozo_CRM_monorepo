package com.tlc.email.server.internal.webhook.receiver.provider.outlook;

import com.microsoft.graph.models.Subscription;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.SubscriptionCollectionPage;
import com.microsoft.graph.requests.SubscriptionCollectionRequestBuilder;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.server.internal.provider.EmailTokenHandler;
import com.tlc.email.server.internal.provider.outlook.GraphClient;
import com.tlc.email.server.internal.status.EmailServerErrorCodes;
import com.tlc.email.server.internal.status.EmailWebhookErrorCodes;
import com.tlc.email.server.internal.webhook.receiver.EmailWebhookReceiver;

import com.tlc.sync.service.oauth.model.provider.EmailProvider;

import com.tlc.web.HttpUtil;
import okhttp3.Request;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import java.nio.charset.StandardCharsets;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.util.Collection;
import java.util.Objects;

/**
 * Handles webhook-related operations for Outlook email accounts,
 * including managing subscription data and processing webhook notifications.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see EmailWebhookReceiver
 */
public final class OutlookEmailWebhookReceiver extends EmailWebhookReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookEmailWebhookReceiver.class);

    private final Configurations configurations;
    private final String changeType;
    private final String notificationUrl;
    private final String resource;
    private final String latestSupportedTlsVersion;
    private final String encryptionCertificateId;
    private final String publicKeyPath;

    private OffsetDateTime expiryDateTime;

    private final EmailTokenHandler emailTokenHandler;

    /**
     * Returns the subscription ID of the webhook.
     */
    private String subscriptionId;

    public OutlookEmailWebhookReceiver(final SyncAddressInfo emailAddress) {
        this.emailAddress = emailAddress;
        this.configurations = ConfHolder.getInstance().getProperties("sync-service").getNestedConfigs("outlook")
                .getNestedConfigs("webhook");
        this.changeType = configurations.getConfig("changeType");
        this.notificationUrl = HttpUtil.buildAbsoluteUrl(configurations.getConfig("notificationUrl"));
        final Configurations certificate = configurations.getNestedConfigs("certificate");

        this.encryptionCertificateId = certificate.getConfig("id");
        this.publicKeyPath = certificate.getConfig("key.public");
        this.resource = configurations.getConfig("resource");
        this.latestSupportedTlsVersion = configurations.getConfig("latestSupportedTlsVersion");
        this.emailTokenHandler = EmailTokenHandler.getInstance();
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(final String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void subscribe() {
        final String address = emailAddress.getEmailAddress();

        try {
            final String accessToken = emailTokenHandler.getAccessToken(emailAddress);
            final GraphServiceClient<Request> graph = GraphClient.build(accessToken);

            validateExistingSubscription(graph);

            expiryDateTime = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1);
            final Subscription subscription = create(graph);

            this.subscriptionId = subscription.id;
            this.expiryTime = expiryDateTime.toInstant().toEpochMilli();
            LOGGER.debug("Webhook Receiver: Subscription ID: {}", subscriptionId);

            afterSubscription();
        } catch (Exception e) {
            LoggerUtil.logError(LOGGER, e, "Webhook Receiver: Subscription create action failed. Stack: ", e);
            throw ErrorCode.get(EmailWebhookErrorCodes.SUBSCRIPTION_ACTION_FAILED, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unsubscribe() {

        try {
            final String accessToken = emailTokenHandler.getAccessToken(emailAddress);
            final GraphServiceClient<Request> graph = GraphClient.build(accessToken);

            graph.subscriptions(subscriptionId).buildRequest().delete();

            LOGGER.debug("Webhook Receiver: Unsubscribe the webhook for ID: {}", subscriptionId);

            afterUnSubscription();
        } catch (Exception e) {
            LoggerUtil.logError(LOGGER, e, "Webhook Receiver: Unsubscription action failed. Stack: ", e);
            throw ErrorCode.get(EmailWebhookErrorCodes.SUBSCRIPTION_ACTION_FAILED, e);
        }

    }

    /**
     * {@inheritDoc}
     *
     * @return the notification url
     */
    @Override
    public String getNotificationUrl() {
        return notificationUrl;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link EmailProvider}
     */
    @Override
    public EmailProvider getProvider() {
        return EmailProvider.OUTLOOK;
    }

    /**
     * Creates the graph subscription.
     *
     * @param graph Represents the {@link GraphServiceClient}
     * @return {@link Subscription}
     */
    private Subscription create(final GraphServiceClient<Request> graph) {
        final Subscription subscription = new Subscription();

        subscription.changeType = changeType;
        subscription.notificationUrl = notificationUrl;
        subscription.resource = resource;
        subscription.includeResourceData = true;
        subscription.expirationDateTime = expiryDateTime;
        subscription.encryptionCertificate = getEncryptionCertificate();
        subscription.encryptionCertificateId = encryptionCertificateId;
        subscription.latestSupportedTlsVersion = latestSupportedTlsVersion;

        return graph.subscriptions().buildRequest().post(subscription);
    }

    /**
     * Validates the existing subscription.
     *
     * @param graph Represents the {@link GraphServiceClient}
     */
    private void validateExistingSubscription(final GraphServiceClient<Request> graph) {

        try {
            SubscriptionCollectionPage currentPage = graph.subscriptions().buildRequest().get();
            boolean isNothingDeleted = true;
            int totalSubscriptions = 0;

            while (Objects.nonNull(currentPage)) {
                final Collection<Subscription> subscriptions = currentPage.getCurrentPage();
                totalSubscriptions += subscriptions.size();

                for (final Subscription subscription : subscriptions) {
                    final String subscriptionId = subscription.id;

                    if (Objects.nonNull(subscriptionId) && notificationUrl.equals(subscription.notificationUrl)) {
                        LOGGER.info("Webhook Receiver: Deleting existing subscription {}, State {}", subscriptionId, subscription.clientState);

                        graph.subscriptions(subscriptionId).buildRequest().delete();

                        isNothingDeleted = false;
                    }
                }
                final SubscriptionCollectionRequestBuilder nextPage = currentPage.getNextPage();

                currentPage = Objects.isNull(nextPage) ? null : nextPage.buildRequest().get();
            }

            if (isNothingDeleted) {
                LOGGER.info("Webhook Receiver: No matching subscriptions found for URL '{}'. Checked {} total subscriptions.", configurations, totalSubscriptions);
            }
        } catch (Exception e) {
            LoggerUtil.logError(LOGGER, e, "Webhook Receiver: Error while validating or deleting existing subscriptions: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets the encrypted certificate.
     *
     * @return encrypted certificate
     */
    private String getEncryptionCertificate() {

        try (final InputStream inputStream = getClass().getResourceAsStream(publicKeyPath)) {

            if (Objects.isNull(inputStream)) {
                LOGGER.error("Webhook Receiver: Encryption certificate public key not found");
                throw ErrorCode.get(EmailServerErrorCodes.PUBLIC_KEY_NOT_FOUND);
            } else {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            LoggerUtil.logError(LOGGER, e, "Webhook Receiver: Public key encryption file fetch failed, Message:{}", e.getMessage(), e);
            throw ErrorCode.get(EmailServerErrorCodes.PUBLIC_KEY_ENCRYPTION_FAILED, e);
        }
    }
}