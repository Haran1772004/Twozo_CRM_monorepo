package com.tlc.crm.email.core.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.email.core.consumer.EmailSyncActionListener;
import com.tlc.email.common.model.SyncJobType;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Listens and handle email synchronization events. Responsible for starting, stopping, and managing the lifecycle of the email synchronization service.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class SyncDataLoadService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncDataLoadService.class);

    private static final String THREAD_POOL_NAME = "sync-load-pool";
    private static final String TASK_CONTROLLER_NAME = "sync-load";

    private static final String CONSUMER_NAME = "sync.load.consumer.name";
    private static final String CONSUMER_MAX_RECORDS = "sync.load.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION = "sync.load.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "sync.load.consumer.max.queue.size";
    private static final String CONSUMER_COUNT = "sync.load.consumer.count";

    private final SchedulerService schedulerService;
    private Map<Long, Task> syncDataLoadTasks;

    public SyncDataLoadService() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input of email sync
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(syncDataLoadTasks)) {
            this.syncDataLoadTasks = new HashMap<>();
            final int consumerCount = configurations.optInt(CONSUMER_COUNT, 1);

            for (int index = 1; index <= consumerCount; index++) {
                final EmailRetrieveTask emailRetrieveTask = new EmailRetrieveTask(configurations, new EmailRetrieveTaskController(TASK_CONTROLLER_NAME));
                final Long emailRetrieveTaskId = schedulerService.schedule(THREAD_POOL_NAME, emailRetrieveTask);

                syncDataLoadTasks.put(emailRetrieveTaskId, emailRetrieveTask);
                LOGGER.debug("Email Sync: Service started, Scheduler ID : {}", emailRetrieveTaskId);
            }

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(syncDataLoadTasks)) {

            for (final Map.Entry<Long, Task> taskEntry : syncDataLoadTasks.entrySet()) {
                final Long taskId = taskEntry.getKey();
                final Task task = taskEntry.getValue();

                schedulerService.removeTask(taskId);
                if (task.isRunning()) {
                    task.stopTask();
                }
                LOGGER.debug("Email Sync: Service stopped, Scheduler ID : {}", taskId);
            }
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
    private static class EmailRetrieveTask extends Task {

        private final BackgroundConsumer emailSyncConsumer;

        private EmailRetrieveTask(final Configurations configurations, final TaskController controller) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(CONSUMER_POLL_DURATION, NumericalConstant.FIVE_THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN);
            final String name = configurations.getConfig(CONSUMER_NAME);

            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(name, false,
                    List.of(EmailQueueTopics.getDataLoadTopic(QueueModel.Type.KAFKA, SyncJobType.SYNC)), new EmailSyncActionListener(),
                    new ConsumerConfig.PollConfig(maxRecord, pollDuration, maxQueueSize));
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
    private static class EmailRetrieveTaskController extends TaskController {

        private EmailRetrieveTaskController(final String taskControllerName) {
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
