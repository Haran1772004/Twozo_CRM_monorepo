package com.tlc.email.server.controller;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.util.Configurations;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.queue.EmailQueueTopics;

import com.tlc.email.server.listener.EmailRetrieveListener;
import com.tlc.email.server.service.SyncService;

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
 * <p>
 * Listens and handle email synchronization events. Responsible for starting, stopping,
 * and managing the lifecycle of the email synchronization service.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class ManualSyncService implements SyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManualSyncService.class);

    private static final String THREAD_POOL_NAME = "sync-retrieve-pool";
    private static final String TASK_CONTROLLER_NAME = "sync-retrieve";

    private static final String EXECUTOR_NAME = "sync.retrieve.thread.name";
    private static final String MAX_POOL_SIZE = "sync.retrieve.thread.pool.size";
    private static final String IS_DAEMON = "sync.retrieve.thread.daemon";

    private static final String CONSUMER_NAME = "sync.retrieve.consumer.name";
    private static final String CONSUMER_MAX_RECORDS = "sync.retrieve.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION = "sync.retrieve.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "sync.retrieve.consumer.max.queue.size";

    private final SchedulerService schedulerService;

    private Task task;
    private Long schedulerId;

    public ManualSyncService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input of email sync
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(schedulerId)) {
            final int maxPoolSize = configurations.optInt(MAX_POOL_SIZE, NumericalConstant.TEN);
            final String name = configurations.getConfig(EXECUTOR_NAME);
            final boolean isDaemon = Boolean.parseBoolean(configurations.getConfig(IS_DAEMON));

            final Executor taskExecutor = ExecutorsHandler.createTaskExecutor(maxPoolSize, name, isDaemon);
            this.task = new ManualSyncTask(configurations, new ManualSyncTaskController(TASK_CONTROLLER_NAME), taskExecutor);
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.info("Email Sync: Service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("Email Sync: Sync service stopped, Scheduler ID : {}", schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("Email Sync: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Responsible for handling the specific task of email synchronization events.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class ManualSyncTask extends Task {

        private final BackgroundConsumer emailSyncConsumer;

        private ManualSyncTask(final Configurations configurations, final TaskController controller, final Executor executor) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN);
            final int poolDuration = configurations.optInt(CONSUMER_POLL_DURATION, NumericalConstant.FIVE_THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN);
            final String name = configurations.getConfig(CONSUMER_NAME);

            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(name, false,
                    List.of(EmailQueueTopics.getDataRetrieveTopic(QueueModel.Type.KAFKA, SyncJobType.SYNC)), new EmailRetrieveListener(executor),
                    new ConsumerConfig.PollConfig(maxRecord, poolDuration, maxQueueSize), false);
            emailSyncConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            emailSyncConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            emailSyncConsumer.stop();

            return true;
        }
    }

    /**
     * <p>
     * Responsible for handling the specific task of email synchronization events. Manages the lifecycle
     * and execution flow of the email synchronization tasks and ensures that these tasks are executed in a controlled and orderly manner.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class ManualSyncTaskController extends TaskController {

        private ManualSyncTaskController(final String taskControllerName) {
            super(taskControllerName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }
}
