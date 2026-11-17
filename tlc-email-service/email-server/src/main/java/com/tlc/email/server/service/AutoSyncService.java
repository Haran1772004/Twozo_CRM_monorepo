package com.tlc.email.server.service;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.server.listener.AutoSyncListener;
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
 * A service that is responsible to trigger and control the email syncing actions
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class AutoSyncService implements SyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoSyncService.class);

    private static final String THREAD_POOL_NAME = "auto-sync-retrieve-pool";
    private static final String TASK_CONTROLLER_NAME = "auto-sync-retrieve";
    private static final String EXECUTOR_NAME = "auto-sync.retrieve.thread.name";
    private static final String MAX_POOL_SIZE = "auto-sync.retrieve.thread.pool.size";
    private static final String IS_DAEMON = "auto-sync.retrieve.thread.daemon";

    private static final String CONSUMER_NAME = "auto-sync.retrieve.consumer.name";
    private static final String CONSUMER_MAX_RECORDS = "auto-sync.retrieve.consumer.max.records";
    private static final String CONSUMER_POOL_DURATION = "auto-sync.retrieve.consumer.pool.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "auto-sync.retrieve.consumer.max.queue.size";

    private final SchedulerService schedulerService;
    private Long schedulerId;
    private AutoSyncTask autoSyncTask;
    private Executor taskExecutor;

    private AutoSyncService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link AutoSyncService}.
     * </p>
     */
    private static class Instance {
        private static final AutoSyncService INSTANCE = new AutoSyncService();
    }

    /**
     * <p>
     * Gets the {@link AutoSyncService} Instance
     * </p>
     *
     * @return {@link AutoSyncService}
     */
    public static AutoSyncService getInstance() {
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
            taskExecutor = ExecutorsHandler.createTaskExecutor(
                    configurations.optInt(MAX_POOL_SIZE, NumericalConstant.TEN),
                    configurations.getConfig(EXECUTOR_NAME),
                    Boolean.parseBoolean(configurations.getConfig(IS_DAEMON))
            );
            this.schedulerId = schedulerService.schedule(
                    THREAD_POOL_NAME,
                    new AutoSyncTask(configurations, new AutoSyncTaskController(TASK_CONTROLLER_NAME), taskExecutor)
            );

            LOGGER.info("Email Auto Sync: Auto syncing service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(autoSyncTask) && autoSyncTask.isRunning()) {
                autoSyncTask.stopTask();
            }
            LOGGER.info("Email Auto Sync: Auto sync service stopped, Scheduler ID : {}", schedulerId);
        }

        if (Objects.nonNull(taskExecutor)) {
            taskExecutor.shutdown();
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
    private static class AutoSyncTask extends Task {

        private final BackgroundConsumer backgroundConsumer;

        private AutoSyncTask(final Configurations configurations, final TaskController controller, final Executor executor) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN);
            final int poolDuration = configurations.optInt(CONSUMER_POOL_DURATION, NumericalConstant.FIVE_THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN);

            final String name = configurations.getConfig(CONSUMER_NAME);
            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(name, false,
                    List.of(EmailQueueTopics.getAutoSyncTopic(QueueModel.Type.KAFKA, EmailQueueTopics.EventAction.RETRIEVE)), new AutoSyncListener(executor),
                    new ConsumerConfig.PollConfig(maxRecord, poolDuration, maxQueueSize), false);
            this.backgroundConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
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
            backgroundConsumer.run();
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
     * A controller which controls email syncing task {@link AutoSyncTask}.
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class AutoSyncTaskController extends TaskController {

        public AutoSyncTaskController(final String scheduleName) {
            super(scheduleName);
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
