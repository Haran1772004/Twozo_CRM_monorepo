package com.tlc.email.server.internal.publisher;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.email.common.codec.EmailDomainRequestCodec;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.SyncStatusCodec;
import com.tlc.email.common.codec.ThreadCodec;
import com.tlc.email.common.converter.FolderIntervalConverter;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.publish.EmailKafkaProducer;
import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.common.request.EmailDomainRequest;
import com.tlc.email.common.request.SendRequest;

import com.tlc.email.server.internal.codec.SyncSettingCodec;
import com.tlc.email.server.internal.manager.SyncSettingsManager;
import com.tlc.email.server.internal.status.EmailServerErrorCodes;
import com.tlc.email.server.model.SyncSetting;
import com.tlc.email.server.model.sync.SyncContext;

import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.queue.producer.Producer;
import com.tlc.queue.service.QueueService;
import com.tlc.queue.service.provider.QueueServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Publishes the email events to the respective channels. Handle event serialization and transformation before publishing.
 * Ensure reliable and asynchronous event processing.
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class EmailEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailEventPublisher.class);

    private final SyncStatusCodec syncStatusCodec;
    private final FolderIntervalConverter folderIntervalConverter;
    private final EmailKafkaProducer emailKafkaProducer;
    private final SyncSettingCodec syncSettingCodec;

    private EmailEventPublisher() {
        this.syncStatusCodec = SyncStatusCodec.getInstance();
        this.folderIntervalConverter = FolderIntervalConverter.getInstance();
        this.emailKafkaProducer = EmailKafkaProducer.getInstance();
        this.syncSettingCodec = SyncSettingCodec.getInstance();
    }

    /**
     * <p>
     * Instance of {@link EmailEventPublisher}.
     * </p>
     */
    private static class Instance {
        private static final EmailEventPublisher INSTANCE = new EmailEventPublisher();
    }

    public static EmailEventPublisher get() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the {@link QueueService}.
     *
     * @return {@link QueueService}
     */
    public QueueService getQueueService() {
        return QueueServiceProvider.get();
    }

    /**
     * Publishes an event to trigger the sync status update process based on the provided {@link SyncAddressInfo}.
     *
     * @param syncAddressInfo Represents the {@link SyncAddressInfo}
     * @param syncStatusInfo  Represents the {@link SyncStatusInfo}
     */
    public void publishSyncStatus(final SyncAddressInfo syncAddressInfo, final SyncStatusInfo syncStatusInfo) {
        final SubscriptionTopic syncStatusTopic = EmailQueueTopics.getSyncStatusTopic(QueueModel.Type.KAFKA);
        final String emailAddress = syncAddressInfo.getEmailAddress();
        final String requestId = Base64.getEncoder().encodeToString(String.format("%s:", emailAddress).getBytes());
        final Optional<String> optionalDateConfig = SyncSettingsManager.getInstance().getDateConfig(emailAddress);

        syncStatusInfo.setLastSyncedTime(Instant.now().toEpochMilli());
        optionalDateConfig.ifPresent(dateConfig -> syncStatusInfo.setFolderIntervals(folderIntervalConverter.getFolderIntervals(dateConfig)));

        try {
            final Producer<byte[]> syncStatusProducer = emailKafkaProducer.getProducer(syncStatusTopic);

            syncStatusProducer.send(new StreamMessage<>(requestId, syncStatusTopic, syncStatusCodec.encode(syncStatusInfo)));

            LOGGER.info("Email Sync Status: Publishes the email sync status : {}, Sync ID: {}", Instant.now().toEpochMilli(), syncStatusInfo.getSyncId());
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.SYNC_STATUS_PUBLISH_ACTION_FAILED, exception);
        }
    }

    /**
     * Publishes an event to trigger to send data config process based on the provided {@link SyncContext}.
     *
     * @param sendRequest Represents the {@link SendRequest}
     * @param thread      Represents the {@link Thread}
     */
    public void publishThread(final SendRequest sendRequest, final Thread thread) {
        final Optional<Email> composeEmail = thread.getComposeEmail();

        if (composeEmail.isPresent()) {
            final Email email = composeEmail.get();
            final SubscriptionTopic threadSubscriptionTopic = EmailQueueTopics.getEmailSendTopic(QueueModel.Type.KAFKA);
            final String requestId = Base64.getEncoder().encodeToString(email.getSender().getEmailAddress().getBytes());
            final ThreadCodec threadCodec = EmailCodec.get(EmailCodec.CodecType.THREAD_CONFIG).get();
            final ThreadHolder threadHolder = new ThreadHolder();

            threadHolder.setSyncedEmailAddress(email.getSender());
            threadHolder.setThreads(List.of(thread));
            threadHolder.setStateData(sendRequest.getStateData());

            try {
                final Producer<byte[]> producer = emailKafkaProducer.getProducer(threadSubscriptionTopic);

                producer.send(new StreamMessage<>(requestId, threadSubscriptionTopic, threadCodec.encode(threadHolder)));
            } catch (Exception exception) {
                throw ErrorCode.get(EmailErrorCodes.SEND_PUBLISH_ACTION_FAILED, exception);
            }
        }
    }

    /**
     * <p>
     * Publishes an event to trigger the domain data load process based on the provided {@link EmailDomainRequest}.
     * </p>
     *
     * @param emailDomainRequest Represents the {@link EmailDomainRequest}
     */
    public void publishDomain(final EmailDomainRequest emailDomainRequest) {
        final SubscriptionTopic subscriptionTopic = EmailQueueTopics.getActionRequestedTopic(QueueModel.Type.KAFKA);
        final String requestId = Base64.getEncoder().encodeToString(emailDomainRequest.getDomain().getName().getBytes());
        final EmailDomainRequestCodec emailDomainRequestCodec = EmailCodec.get(EmailCodec.CodecType.DOMAIN).get();

        try {
            final Producer<byte[]> producer = emailKafkaProducer.getProducer(subscriptionTopic);

            producer.send(new StreamMessage<>(requestId, subscriptionTopic, emailDomainRequestCodec.encode(emailDomainRequest)));
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.SEND_PUBLISH_ACTION_FAILED, exception);
        }
    }

    /**
     * Publishes an event to trigger the automatic webhook receiver process based on the provided request payload.
     *
     * @param payload Represents the request payload
     */
    public void publishWebhookNotification(final String payload) {
        final String requestId = Base64.getEncoder().encodeToString(payload.getBytes());
        final SubscriptionTopic webhookEventTopic = EmailQueueTopics.getWebhookEventTopic(QueueModel.Type.KAFKA);

        try {
            final Producer<byte[]> autoSyncProducer = emailKafkaProducer.getProducer(webhookEventTopic);

            autoSyncProducer.send(new StreamMessage<>(requestId, webhookEventTopic, payload.getBytes()));

            LOGGER.info("Email Auto Sync: Auto sync publish action completed : {}", Instant.now().toEpochMilli());
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Email Auto Sync: Auto sync publish action failed");

            throw ErrorCode.get(EmailServerErrorCodes.AUTO_SYNC_ACTION_FAILED, exception);
        }
    }

    /**
     * Publishes an event to trigger the inbound email receive process based on the provided request payload.
     *
     * @param payload Represents the request payload
     */
    public void publishInboundEmailReceived(final String payload) {
        final String requestId = Base64.getEncoder().encodeToString(payload.getBytes());
        final SubscriptionTopic inboundEmailTopic = EmailQueueTopics.getInboundReceiveTopic(QueueModel.Type.KAFKA);

        try {
            final Producer<byte[]> inboundEmailProducer = emailKafkaProducer.getProducer(inboundEmailTopic);

            inboundEmailProducer.send(new StreamMessage<>(requestId, inboundEmailTopic, payload.getBytes()));

            LOGGER.info("Inbound Email Receiver: Inbound email publish action completed : {}", Instant.now().toEpochMilli());
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Inbound Email Receiver: Inbound email publish action failed");

            throw ErrorCode.get(EmailErrorCodes.PUBLISH_ACTION_FAILED, exception);
        }
    }

    /**
     * Publishes an event to trigger the automatic synchronization process based on the provided {@link SyncSetting}.
     *
     * @param syncSetting      Represents the {@link SyncSetting}
     * @param updateSyncStatus A {@link Consumer} that accepts a {@link SyncStatus} object to update the sync status
     *                         during the auto-sync process.
     */
    public void publishAutoSyncEvent(final SyncSetting syncSetting, final Consumer<SyncStatus> updateSyncStatus) {
        final Long syncId = syncSetting.id();
        final String requestId = Base64.getEncoder().encodeToString(String.format("%s", syncSetting.getEmailAddress().getEmailAddress()).getBytes());
        final SubscriptionTopic autoSyncTopic = EmailQueueTopics.getAutoSyncTopic(QueueModel.Type.KAFKA, EmailQueueTopics.EventAction.RETRIEVE);

        try {
            final Producer<byte[]> autoSyncProducer = emailKafkaProducer.getProducer(autoSyncTopic);

            autoSyncProducer.send(new StreamMessage<>(requestId, autoSyncTopic, syncSettingCodec.encode(syncSetting)));

            LOGGER.info("Email Auto Sync: Auto sync publish action completed : {}, Sync ID: {}", Instant.now().toEpochMilli(), syncId);
        } catch (final Exception exception) {
            updateSyncStatus.accept(SyncStatus.FAILED);
            LoggerUtil.logError(LOGGER, exception, "Email Auto Sync: Auto sync publish action failed, Sync ID: {}", syncId);

            throw ErrorCode.get(EmailServerErrorCodes.AUTO_SYNC_ACTION_FAILED, exception);
        }
    }
}