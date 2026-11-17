package com.tlc.crm.common.iam.internal;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.queue.CommonQueueTopics;
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
 * A service that is responsible to trigger and control the user migration consumer actions {@link UserDataMigrationProcessor}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class UserMigrationService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserMigrationService.class);
    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-common-user-migrator-request-consumer";
    private static final String THREAD_POOL_NAME = "user-data-migrator-pool";
    private static final String TASK_SCHEDULER_NAME = "user-data-migrator";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public UserMigrationService() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Schedules an instance of user migrator task with the specified name and logs the relevant information
     * </p>
     *
     * @param configurations Represents the input of the user migrator
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new UserMigratorTask(new UserMigraterTaskController());
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.info("User data migration service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Stops the user migrator consumer service by removing the task associated with the schedulerId.
     * </p>
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("User migration consumer service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to do the user migration action
     * </p>
     *
     * @author Kavinila E
     * @version 1.0
     */
    private static class UserMigratorTask extends Task {

        private final BackgroundConsumer userMigratorConsumer;

        private UserMigratorTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CommonQueueTopics.getUserDataMigratorTopic(QueueModel.Type.KAFKA)),
                    new UserDataMigrationProcessor(), new ConsumerConfig.PollConfig(10, 1000, 10));
            userMigratorConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            userMigratorConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            userMigratorConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls the {@link UserMigratorTask}.
     * </p>
     *
     * @author Kavinila E
     * @version 1.0
     */
    private static class UserMigraterTaskController extends TaskController {

        private UserMigraterTaskController() {
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
