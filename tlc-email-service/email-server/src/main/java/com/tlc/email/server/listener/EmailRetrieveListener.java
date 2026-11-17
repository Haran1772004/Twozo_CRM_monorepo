package com.tlc.email.server.listener;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.codec.EmailCodec;
import com.tlc.email.common.codec.SyncRequestCodec;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.request.SyncRequest;
import com.tlc.email.server.internal.manager.SyncSettingsManager;
import com.tlc.email.server.internal.publisher.EmailEventPublisher;
import com.tlc.email.server.internal.sync.cache.SyncInfoWriteService;
import com.tlc.email.server.service.EmailActionHandler;

import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>
 * Handles the processing of {@link StreamMessage}. Responsible for consuming
 * and processing email synchronization events from a streaming source.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see StreamListener
 */
public class EmailRetrieveListener implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailRetrieveListener.class);

    private final EmailActionHandler emailActionHandler;
    private final SyncInfoWriteService syncInfoWriteService;
    private final SyncSettingsManager syncSettingsManager;
    private final EmailEventPublisher emailEventPublisher;
    private final Executor taskExecutor;

    public EmailRetrieveListener(final Executor executor) {
        this.emailActionHandler = EmailActionHandler.get();
        this.syncInfoWriteService = SyncInfoWriteService.get();
        this.syncSettingsManager = SyncSettingsManager.getInstance();
        this.emailEventPublisher = EmailEventPublisher.get();
        this.taskExecutor = executor;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param streamMessage Represents the stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {

        try {
            final JsonObject requestParameters = Json.object(streamMessage.data());
            final SyncRequestCodec syncRequestCodec = (SyncRequestCodec) EmailCodec.get(EmailCodec.CodecType.RECEIVE);
            final SyncRequest syncRequest = syncRequestCodec.decode(requestParameters.getBytes());

            if (Objects.nonNull(syncRequest)) {
                taskExecutor.execute(() -> {
                    final EmailReceiverConfiguration receiverConfiguration = syncRequest.getReceiverConfiguration();
                    final SyncAddressInfo emailAddress = receiverConfiguration.getSyncEmailAddress();
                    final String syncAddress = emailAddress.getEmailAddress();
                    final Long syncId = syncRequest.id();

                    syncSettingsManager.loadSyncSettingsIntoContainer(syncRequest);
                    LOGGER.debug("Email sync: Email sync process initiated, syncId: {}", syncId);

                    final Consumer<SyncStatus> updateSyncStatus = syncStatus -> {
                        final SyncStatusInfo syncStatusInfo = new SyncStatusInfo(syncId, syncAddress, syncStatus);

                        syncInfoWriteService.updateStatus(syncStatusInfo);

                        if (!Objects.equals(syncStatus, SyncStatus.IN_PROGRESS)) {
                            emailEventPublisher.publishSyncStatus(emailAddress, syncStatusInfo);
                        }
                    };

                    try {
                        updateSyncStatus.accept(SyncStatus.IN_PROGRESS);

                        syncRequest.setSyncJobType(SyncJobType.SYNC);
                        emailActionHandler.sync(syncRequest);
                        streamMessage.clean();
                    } catch (final Throwable throwable) {
                        updateSyncStatus.accept(SyncStatus.FAILED);

                        streamMessage.clean();
                        LoggerUtil.logError(LOGGER, throwable, "Email sync: Failure occurred while retrieving mails reason: {}, message: {}, syncId: {}", throwable, throwable.getMessage(), syncId);
                    }
                });
            } else {
                streamMessage.clean();
            }
        } catch (final Exception exception) {
            streamMessage.clean();
            LoggerUtil.logError(LOGGER, exception, "Email sync : Error occurred while decoding the request reason: {}", exception.getMessage());
        }
    }
}
