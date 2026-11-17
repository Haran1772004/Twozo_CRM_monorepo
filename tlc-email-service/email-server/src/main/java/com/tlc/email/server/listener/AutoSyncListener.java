package com.tlc.email.server.listener;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.server.internal.codec.SyncSettingCodec;
import com.tlc.email.server.internal.handler.AbstractAutoSyncActionHandler;
import com.tlc.email.server.internal.manager.EmailManager;
import com.tlc.email.server.internal.sync.cache.SyncInfoWriteService;
import com.tlc.email.server.internal.sync.interval.SyncIntervalHandler;
import com.tlc.email.server.model.SyncSetting;
import com.tlc.email.common.model.config.FolderInterval;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * <p>
 * Listens the processing of {@link StreamMessage}. Responsible for consuming
 * and processing email auto sync events from a streaming source.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class AutoSyncListener extends AbstractAutoSyncActionHandler implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoSyncListener.class);

    private final Executor taskExecutor;
    private final SyncInfoWriteService syncInfoWriteService;
    private final SyncIntervalHandler syncIntervalHandler;
    private final SyncSettingCodec syncSettingCodec;
    private final EmailManager emailManager;

    public AutoSyncListener(final Executor executor) {
        super();
        this.taskExecutor = executor;
        this.syncInfoWriteService = SyncInfoWriteService.get();
        this.syncIntervalHandler = SyncIntervalHandler.getInstance();
        this.syncSettingCodec = SyncSettingCodec.getInstance();
        this.emailManager = EmailManager.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param streamMessage stream message
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        try {
            final SyncSetting syncSetting = syncSettingCodec.decode(streamMessage.data());
            final Long syncId = syncSetting.id();
            final EmailAddress syncAddressInfo = emailManager.getOrInitEmail(syncSetting.getEmailAddress().getEmailAddress());
            final String emailAddress = syncAddressInfo.getEmailAddress();
            final Consumer<SyncStatus> updateSyncStatus = syncStatus ->
                    syncInfoWriteService.updateStatus(new SyncStatusInfo(syncId, emailAddress, syncStatus));

            taskExecutor.execute(() -> {
                try {
                    updateSyncStatus.accept(SyncStatus.IN_PROGRESS);
                    final Collection<FolderInterval> unSyncedIntervals = syncIntervalHandler.fetchAutoSyncIntervals(syncAddressInfo);

                    handle(syncAddressInfo, unSyncedIntervals);
                    updateSyncStatus.accept(SyncStatus.SYNCED);
                    streamMessage.clean();
                } catch (final Throwable throwable) {
                    updateSyncStatus.accept(SyncStatus.FAILED);
                    streamMessage.clean();

                    LoggerUtil.logError(LOGGER, throwable, "Email Auto Sync: Failure occurred while attempting initialization for Sync ID: {}, Stack: {}",
                            syncId, throwable.getMessage());
                }
            });
        } catch (final Throwable throwable) {
            streamMessage.clean();
            LoggerUtil.logError(LOGGER, throwable, "Email Auto Sync: Failure occurred before handover to the next thread");
        }
    }
}