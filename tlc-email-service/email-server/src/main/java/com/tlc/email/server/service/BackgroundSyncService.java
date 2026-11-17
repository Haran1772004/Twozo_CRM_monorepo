package com.tlc.email.server.service;

import com.tlc.commons.util.Configurations;
import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.server.internal.manager.SyncSettingsManager;
import com.tlc.email.server.internal.publisher.EmailEventPublisher;
import com.tlc.email.server.internal.sync.cache.SyncInfoWriteService;
import com.tlc.email.server.model.SyncSetting;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * <p>
 * A service that is responsible to trigger and control the email syncing actions
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class BackgroundSyncService implements SyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackgroundSyncService.class);
    private static final String THREAD_POOL_NAME = "background-sync-pool";
    private static final String TASK_SCHEDULER_NAME = "background-sync";

    private final SchedulerService schedulerService;
    private Long schedulerId;
    private BackgroundSyncSchedulerTask backgroundSyncSchedulerTask;

    private BackgroundSyncService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link BackgroundSyncService}.
     * </p>
     */
    private static class Instance {
        private static final BackgroundSyncService INSTANCE = new BackgroundSyncService();
    }

    /**
     * <p>
     * Gets the {@link BackgroundSyncService} Instance
     * </p>
     *
     * @return {@link BackgroundSyncService}
     */
    public static BackgroundSyncService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the given configurations
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.backgroundSyncSchedulerTask = new BackgroundSyncSchedulerTask(new BackgroundSyncSchedulerTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, backgroundSyncSchedulerTask);

            LOGGER.info("Auto Sync: Sync service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(backgroundSyncSchedulerTask) && backgroundSyncSchedulerTask.isRunning()) {
                backgroundSyncSchedulerTask.stopTask();
            }
            LOGGER.info("Auto Sync: sync service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible for email syncing activity
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class BackgroundSyncSchedulerTask extends Task {

        private static final short LIMIT = 10;
        private final SyncSettingsManager syncSettingsManager;
        private final SyncInfoWriteService syncInfoWriteService;
        private final EmailEventPublisher emailEventPublisher;

        private BackgroundSyncSchedulerTask(final TaskController controller) {
            super(controller);
            this.syncSettingsManager = SyncSettingsManager.getInstance();
            this.syncInfoWriteService = SyncInfoWriteService.get();
            this.emailEventPublisher = EmailEventPublisher.get();
        }

        /**
         * <p>
         * Executes the task which is scheduled
         * </p>
         *
         * @param taskController represents the controller of the task
         */
        @Override
        public void execute(final TaskController taskController) {
            try {
                LOGGER.info("Auto Sync: Sync scheduler action starts : {}", Instant.now().toEpochMilli());
                final Collection<SyncSetting> syncSettings = syncSettingsManager.get(LIMIT);

                for (final SyncSetting syncSetting : syncSettings) {
                    final Long syncId = syncSetting.id();
                    final Consumer<SyncStatus> updateSyncStatus = getSyncStatusConsumer(syncSetting, syncId);

                    syncSetting.setSyncStatus(SyncStatus.IN_QUEUE);
                    updateSyncStatus.accept(SyncStatus.IN_QUEUE);

                    emailEventPublisher.publishAutoSyncEvent(syncSetting, updateSyncStatus);
                }
            } catch (Exception exception) {
                LOGGER.debug("Auto Sync: Syncing action failed : {}, Stack : ", exception.getMessage(), exception);
            }
        }


        private Consumer<SyncStatus> getSyncStatusConsumer(final SyncSetting syncSetting, final Long syncId) {
            final SyncAddressInfo emailAddress = syncSetting.getEmailAddress();
            final String address = emailAddress.getEmailAddress();

            return syncStatus -> {
                final SyncStatusInfo syncStatusInfo = new SyncStatusInfo(syncId, address, syncStatus);

                syncInfoWriteService.updateStatus(syncStatusInfo);
                emailEventPublisher.publishSyncStatus(emailAddress, syncStatusInfo);
            };
        }

        /**
         * {@inheritDoc}
         *
         * @return true
         */
        @Override
        public boolean stopTask() {
            return true;
        }
    }

    /**
     * A controller which controls email syncing task {@link BackgroundSyncSchedulerTask}.
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class BackgroundSyncSchedulerTaskController extends TaskController {

        public BackgroundSyncSchedulerTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return TimeUnit.SECONDS.toMillis(30);
        }
    }
}
