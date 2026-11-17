package com.tlc.crm.email.core.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.email.core.consumer.SyncStatusListener;

import com.tlc.email.common.queue.EmailQueueTopics;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * A service that is responsible to trigger and control the email sync status actions.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see Service
 */
public class SyncStatusController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncStatusController.class);
    private static final String THREAD_POOL_NAME = "email-sync-status-pool";
    private static final String TASK_SCHEDULER_NAME = "email-sync-status";
    private final SchedulerService schedulerService;
    private Long schedulerId;
    private SyncStatusTask syncStatusTask;

    private static class Instance {
        private static final SyncStatusController INSTANCE = new SyncStatusController();
    }

    private SyncStatusController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Gets the {@link SyncStatusController} Instance
     * </p>
     *
     * @return {@link SyncStatusController}
     */
    public static SyncStatusController getInstance() {
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
            this.syncStatusTask = new SyncStatusTask(new SyncStatusTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, syncStatusTask);

            LOGGER.debug("Email sync status service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(syncStatusTask) && syncStatusTask.isRunning()) {
                syncStatusTask.stopTask();
            }

            LOGGER.debug("Email sync status service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible for email sync status activity.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class SyncStatusTask extends Task {

        private final BackgroundConsumer emailSyncStatusConsumer;

        private SyncStatusTask(final TaskController controller) {
            super(controller);
            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig("email-thread-sync-status-consumer", false,
                    List.of(EmailQueueTopics.getSyncStatusTopic(QueueModel.Type.KAFKA)), new SyncStatusListener(),
                    new ConsumerConfig.PollConfig(10, 5000, 10));
            emailSyncStatusConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
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
            emailSyncStatusConsumer.run();
        }

        /**
         * {@inheritDoc}
         *
         * @return true
         */
        @Override
        public boolean stopTask() {
            emailSyncStatusConsumer.stop();

            return true;
        }
    }

    /**
     * A controller which controls email syncing task.
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public static class SyncStatusTaskController extends TaskController {

        public SyncStatusTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }
}
