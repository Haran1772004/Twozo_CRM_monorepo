package com.tlc.crm.email.core.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.email.core.consumer.EmailAutoSyncEventListener;
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
 * <p>
 * Service responsible for scheduling and managing the execution of the email event listener.
 * </p>
 *
 * @author Mohamed Yasar
 * @version 1.0
 */
public class AutoSyncDataLoadService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoSyncDataLoadService.class);

    private static final String THREAD_POOL_NAME = "email-auto-sync-event-pool";
    private static final String TASK_CONTROLLER_NAME = "email-auto-sync-event";

    private static final String CONSUMER_NAME = "auto-sync.consumer.name";
    private static final String CONSUMER_MAX_RECORDS = "auto-sync.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION = "auto-sync.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "auto-sync.consumer.max.queue.size";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    /**
     * Initializes the EmailWebhookEventService with the required scheduler service.
     */
    public AutoSyncDataLoadService() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Starts the email webhook event service and schedules the consumer task
     * to listen for incoming webhook messages.
     * </p>
     *
     * @param configurations the configuration object containing consumer settings
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new EmailWebhookEventTask(new EmailWebhookEventController(TASK_CONTROLLER_NAME), configurations);
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.debug("Email Sync: Service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Stops the email webhook event service and terminates the scheduled consumer task.
     * </p>
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }

            LOGGER.debug("Email Sync: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Internal task class responsible for executing the webhook event consumer.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class EmailWebhookEventTask extends Task {

        private final BackgroundConsumer emailWebhookEventConsumer;

        /**
         * Constructs the EmailWebhookEventTask with the given controller and configuration settings.
         *
         * @param controller     the task controller
         * @param configurations configuration values for consumer settings
         */
        private EmailWebhookEventTask(final TaskController controller, final Configurations configurations) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(CONSUMER_POLL_DURATION, NumericalConstant.FIVE_THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN);
            final String consumerName = configurations.getConfig(CONSUMER_NAME);

            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(
                    consumerName,
                    false,
                    List.of(EmailQueueTopics.getAutoSyncTopic(QueueModel.Type.KAFKA, EmailQueueTopics.EventAction.LOAD)),
                    new EmailAutoSyncEventListener(),
                    new ConsumerConfig.PollConfig(maxRecord, pollDuration, maxQueueSize)
            );

            emailWebhookEventConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * <p>
         * Executes the task by running the background consumer.
         * </p>
         *
         * @param taskController the task controller
         */
        @Override
        public void execute(final TaskController taskController) {
            emailWebhookEventConsumer.run();
        }

        /**
         * <p>
         * Stops the execution of the webhook consumer.
         * </p>
         *
         * @return {@code true} once the consumer is stopped
         */
        @Override
        public boolean stopTask() {
            emailWebhookEventConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * Controller class for managing the scheduling behavior of the webhook task.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class EmailWebhookEventController extends TaskController {

        private EmailWebhookEventController(final String taskControllerName) {
            super(taskControllerName);
        }

        @Override
        public long getNextInterval() {
            return -1;
        }
    }
}