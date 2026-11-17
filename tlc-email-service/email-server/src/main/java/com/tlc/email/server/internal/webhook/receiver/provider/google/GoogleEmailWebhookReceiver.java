package com.tlc.email.server.internal.webhook.receiver.provider.google;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.server.internal.provider.EmailTokenHandler;
import com.tlc.email.server.internal.provider.gmail.GmailClient;
import com.tlc.email.server.internal.provider.gmail.model.Label.Name;
import com.tlc.email.server.internal.provider.gmail.model.Label.FilterBehaviour;
import com.tlc.email.server.internal.provider.gmail.model.WatchRequest;
import com.tlc.email.server.internal.provider.gmail.model.WatchResponse;
import com.tlc.email.server.internal.provider.gmail.service.GmailService;
import com.tlc.email.server.internal.webhook.receiver.EmailWebhookReceiver;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Handles Gmail webhook subscriptions using Google's Pub/Sub for change notifications.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class GoogleEmailWebhookReceiver extends EmailWebhookReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleEmailWebhookReceiver.class);

    private final String pubSubTopic;
    private final String currentUser;

    private BigInteger historyId;

    public GoogleEmailWebhookReceiver(final SyncAddressInfo syncAddressInfo) {
        final GmailService gmailService = GmailService.getInstance();

        this.emailAddress = syncAddressInfo;
        this.pubSubTopic = gmailService.getPubSubTopic();
        this.currentUser = gmailService.getCurrentUser();
    }

    /**
     * Returns the reference of the history id of emailAddress associated with the webhook.
     *
     * @return the history ID
     */
    public BigInteger getHistoryId() {
        return historyId;
    }

    /**
     * Set the history id.
     */
    public void setHistoryId(final BigInteger historyId) {
        this.historyId = historyId;
    }

    /**
     * Returns the expiry time of subscription.
     *
     * @return the expiry time
     */
    @Override
    public Long getExpiryTime() {
        return expiryTime;
    }

    /**
     * Returns the {@link EmailProvider}
     *
     * @return {@link EmailProvider}
     */
    @Override
    public EmailProvider getProvider() {
        return EmailProvider.GOOGLE;
    }

    /**
     * Initiates a subscription with the webhook provider.
     *
     * <p>
     * Register the callback URL with the
     * external service so that notifications can be received.
     * </p>
     */
    @Override
    public void subscribe() {
        if (Objects.isNull(emailAddress)) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_EMAIL_ADDRESS);
        }
        final String accessToken = EmailTokenHandler.getInstance().getAccessToken(new SyncAddressInfo(emailAddress.getEmailAddress()));
        final GmailClient gmailClient = new GmailClient.Builder()
                .setAccessToken(accessToken)
                .build();
        final WatchResponse watchResponse = gmailClient.watch(currentUser, WatchRequest.builder()
                .pubSubTopic(pubSubTopic)
                .labelFilterBehaviour(FilterBehaviour.INCLUDE.getValue())
                .labelIds(List.of(Name.INBOX.getValue(), Name.SENT.getValue(), Name.TRASH.getValue(), Name.DRAFT.getValue()))
                .build()
        );

        this.expiryTime = getExpiryTime(watchResponse.getExpiration());
        this.historyId = watchResponse.getHistoryId();

        LOGGER.debug("Webhook Receiver: History ID: {} with the expiry time: {}", historyId, expiryTime);

        afterSubscription();
    }

    /**
     * <p>
     * Determines the effective expiry time for the Gmail watch.
     * </p>
     *
     * @param expiryTime Expiration time returned by Gmail (epoch millis, may be null).
     * @return The effective expiry time to use (epoch millis).
     */
    protected long getExpiryTime(final Long expiryTime) {
        final long defaultExpiry = OffsetDateTime.now(ZoneOffset.UTC).plusDays(1).toInstant().toEpochMilli();

        if (Objects.isNull(expiryTime)) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_EXPIRY_TIME);
        }

        return expiryTime - System.currentTimeMillis() < Duration.ofDays(1).toMillis() ? expiryTime : defaultExpiry;
    }

    /**
     * Terminates an existing subscription with the webhook provider.
     *
     * <p>
     * Clean up any server-side or remote configurations
     * previously established during the subscription phase.
     * </p>
     */
    @Override
    public void unsubscribe() {
        if (Objects.isNull(emailAddress)) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_EMAIL_ADDRESS);
        }

        final String accessToken = EmailTokenHandler.getInstance().getAccessToken(new SyncAddressInfo(emailAddress.getEmailAddress()));
        final GmailClient gmailClient = new GmailClient.Builder()
                .setAccessToken(accessToken)
                .build();

        gmailClient.stopWatch(currentUser);

        afterUnSubscription();
    }

    /**
     * <p>
     * Returns the webhook notification URL for sending/receiving events.
     * </p>
     *
     * <p>
     * This method returns an empty string because Gmail's Pub/Sub service does not require
     * explicitly setting the notification URL in the application.
     * <br><br>
     * How Gmail Pub/Sub works:
     *     <ul>
     *         <li>First, create a Pub/Sub topic in your Google Cloud project.</li>
     *         <li>Then, create a subscription for that topic.</li>
     *         <li>Configure the push endpoint (callback URL) in the subscription settings.</li>
     *     </ul>
     *     Gmail will push notifications to the URL defined in the subscription.
     *     Hence, there is no need to return or configure the URL from this code.
     * </p>
     *
     * @return an empty string since Gmail handles the callback URL via Pub/Sub configuration
     */
    @Override
    public String getNotificationUrl() {
        return "";
    }
}