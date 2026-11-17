package com.tlc.crm.notifications.internal.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.notifications.internal.initializer.NotificationActionInitializer;
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
 * A service that is responsible to trigger and control the notification actions {@link  NotificationActionController}
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class NotificationActionController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationActionController.class);
    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-notifications-notification-action-consumer";
    private static final String THREAD_POOL_NAME = "notification-pool";
    private static final String TASK_SCHEDULER_NAME = "notification-scheduler";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public NotificationActionController() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * An instance of {@link NotificationActionTask} is started & logged.
     * </p>
     *
     * @param configurations Represents the input.
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new NotificationActionTask(new NotificationActionTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);
            LOGGER.info("Notification action consumer service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Stops the consumer by removing the task associated with schedulerId.
     * </p>
     */
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("Notification action consumer service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to do the notification action
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class NotificationActionTask extends Task {

        private final BackgroundConsumer requestConsumer;

        private NotificationActionTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getNotificationStorageEventTopic(QueueModel.Type.KAFKA)), new NotificationActionInitializer(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
            requestConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            requestConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            requestConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls the {@link NotificationActionTask}.
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class NotificationActionTaskController extends TaskController {

        private NotificationActionTaskController() {
            super(TASK_SCHEDULER_NAME);
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
