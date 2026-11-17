package com.tlc.email.server.internal.webhook.receiver;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailDeleteEvent;
import com.tlc.email.common.model.EmailUpdateEvent;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.publish.EmailEventRequestPublisher;
import com.tlc.email.common.request.EmailEventRequest;
import com.tlc.email.common.request.SyncRequest;

import com.tlc.email.server.internal.manager.SyncSettingsManager;
import com.tlc.email.server.internal.manager.ThreadManager;
import com.tlc.email.server.internal.sync.AbstractSyncActionHandler;
import com.tlc.email.server.internal.webhook.receiver.manager.EmailWebhookReceiverManagerImpl;
import com.tlc.email.server.service.WebhookNotificationHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

/**
 * Handles incoming webhook notifications from external services and triggering corresponding sync actions.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see AbstractSyncActionHandler
 * @see WebhookNotificationHandler
 */
public abstract class AbstractWebhookNotificationHandler extends AbstractSyncActionHandler implements WebhookNotificationHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebhookNotificationHandler.class);

    protected final EmailWebhookReceiverManagerImpl webhookReceiverManager;

    protected final SyncSettingsManager syncSettingsManager;
    private final ThreadManager threadManager;
    private final EmailEventRequestPublisher emailEventRequestPublisher;

    protected AbstractWebhookNotificationHandler() {
        this.syncSettingsManager = SyncSettingsManager.getInstance();
        this.webhookReceiverManager = EmailWebhookReceiverManagerImpl.getInstance();
        this.threadManager = ThreadManager.getInstance();
        this.emailEventRequestPublisher = EmailEventRequestPublisher.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param payload the notification content
     */
    @Override
    public void handle(final JsonObject payload) {

        try {
            final Collection<Email> emails = getEmails(payload);

            if (Objects.nonNull(emails) && !emails.isEmpty()) {
                final Optional<SyncAddressInfo> syncAddressInfo = getSyncAddressInfo(payload);

                if (syncAddressInfo.isPresent()) {
                    final SyncAddressInfo syncAddress = syncAddressInfo.get();
                    final String emailAddress = syncAddress.getEmailAddress();

                    LOGGER.info("Webhook Receiver: Handles the incoming webhook request, EmailAddress:{}", emailAddress);
                    loadMails(emails, emailAddress);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Webhook Receiver: Failed to retrieve data associated with the synced email, Message:{}", e.getMessage());
            throw ErrorCode.get(EmailErrorCodes.SYNC_ACTION_FAILED, e);
        }
    }

    /**
     * <p>
     * Gets the {@link Email} from the request payload.
     * </p>
     *
     * @param payload Represents the request payload
     * @return {@link Email}
     */
    protected abstract Collection<Email> getEmails(final JsonObject payload);

    /**
     * <p>
     * Gets the {@link SyncAddressInfo}.
     * </p>
     *
     * @return {@link SyncAddressInfo}
     */
    protected abstract Optional<SyncAddressInfo> getSyncAddressInfo(final JsonObject payload);

    /**
     * <p>
     * Loads emails for the given email address and prepares the sync request configuration.
     * </p>
     *
     * @param emails       The collection of {@link Email} instances to process.
     * @param emailAddress The email address for which emails are to be loaded.
     */
    private void loadMails(final Collection<Email> emails, final String emailAddress) {
        final Optional<Long> optionalEmailId = syncSettingsManager.getId(emailAddress);

        optionalEmailId.ifPresent(emailId -> {
            final EmailReceiverConfiguration emailReceiverConfiguration = new EmailReceiverConfiguration();
            final SyncRequest syncRequest = new SyncRequest(emailReceiverConfiguration);
            final SyncAddressInfo syncAddressInfo = new SyncAddressInfo(emailAddress);

            emailReceiverConfiguration.setSyncEmailAddress(syncAddressInfo);
            syncRequest.setId(syncSettingsManager.getSyncInfo(emailAddress).getSyncId());

            handleSync(syncRequest, emails);
        });
    }

    /**
     * <p>
     * Processes the modified mails by handling deleted message IDs and updated threads.
     * Deletes messages and updates thread labels accordingly, then publishes the changes.
     * </p>
     *
     * @param syncAddressInfo The {@link SyncAddressInfo} containing sync-related email information
     * @param modifiedThreads The collection of modified {@link Thread} instances
     */
    protected void handleModifiedMails(final SyncAddressInfo syncAddressInfo,
                                       final Collection<Thread> modifiedThreads, final Collection<Long> deletedMessageIds) {
        if (Objects.nonNull(modifiedThreads) && !modifiedThreads.isEmpty()) {
            threadManager.updateThreadLabels(modifiedThreads);
        }

        if (Objects.nonNull(deletedMessageIds) && !deletedMessageIds.isEmpty()) {
            threadManager.deleteMessages(deletedMessageIds);
        }
        publishModifiedMails(syncAddressInfo, modifiedThreads, deletedMessageIds);
    }

    /**
     * <p>
     * Processes the modified mails by handling updated threads only.
     * </p>
     *
     * @param syncAddressInfo The {@link SyncAddressInfo} containing sync-related email information
     * @param modifiedThreads The collection of modified {@link Thread} instances
     */
    protected void handleModifiedMails(final SyncAddressInfo syncAddressInfo,
                                       final Collection<Thread> modifiedThreads) {
        handleModifiedMails(syncAddressInfo, modifiedThreads, Collections.emptyList());
    }

    /**
     * <p>
     * Publishes modified mail data, including deleted message IDs and updated threads,
     * to the appropriate Kafka topic for downstream processing.
     * </p>
     *
     * @param syncAddressInfo The {@link SyncAddressInfo} containing sync-related email information
     * @param threads         The collection of updated {@link Thread} instances
     */
    private void publishModifiedMails(final SyncAddressInfo syncAddressInfo,
                                      final Collection<Thread> threads,
                                      final Collection<Long> deletedMessageIds) {

        final boolean hasToBeUpdated = Objects.nonNull(threads) && !threads.isEmpty();
        final boolean hasToBeDeleted = Objects.nonNull(deletedMessageIds) && !deletedMessageIds.isEmpty();

        if (!hasToBeUpdated && !hasToBeDeleted) {
            return;
        }
        final EmailEventRequest emailEventRequest = new EmailEventRequest();
        final EmailReceiverConfiguration emailReceiverConfiguration = new EmailReceiverConfiguration();

        emailReceiverConfiguration.setSyncEmailAddress(syncAddressInfo);
        emailEventRequest.setReceiverConfiguration(emailReceiverConfiguration);

        if (hasToBeUpdated) {
            final ThreadHolder threadHolder = new ThreadHolder();

            threadHolder.setSyncedEmailAddress(syncAddressInfo);
            threadHolder.setThreads(threads);

            emailEventRequest.setUpdateEvent(new EmailUpdateEvent(threadHolder));
        }

        if (hasToBeDeleted) {
            emailEventRequest.setDeleteEvent(new EmailDeleteEvent(deletedMessageIds));
        }
        emailEventRequestPublisher.publish(emailEventRequest);
    }
}