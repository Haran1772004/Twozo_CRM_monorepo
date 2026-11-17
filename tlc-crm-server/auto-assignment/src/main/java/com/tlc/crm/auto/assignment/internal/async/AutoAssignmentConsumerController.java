package com.tlc.crm.auto.assignment.internal.async;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.core.queue.CoreQueueTopics;

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
 * Service responsible for consuming and processing auto-assignment event messages.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AutoAssignmentConsumerController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutoAssignmentConsumerController.class);

    private final SchedulerService schedulerService;

    private Long schedulerId;
    private AutoAssignmentTask autoAssignmentTask;
    private Executor executor;

    public AutoAssignmentConsumerController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            final String eventHandlerPoolWithSize = configurations.getConfig("event.handler.pool");
            final String[] poolNameAndSize = eventHandlerPoolWithSize.split(":");
            this.executor = ExecutorsHandler.createTaskExecutor(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0], false);
            this.autoAssignmentTask = new AutoAssignmentTask(configurations.getNestedConfigs("event.consumer"), new AutoAssignmentTaskController("assignment-execution-processor"), executor);
            this.schedulerId = schedulerService.schedule("assignment-executor-pool", autoAssignmentTask);

            LOGGER.info("Auto assignment: Assignment rule consumer service started, Scheduler ID: {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(autoAssignmentTask) && autoAssignmentTask.isRunning()) {
                autoAssignmentTask.stopTask();
            }

            if (Objects.nonNull(executor)) {
                executor.shutdown();
            }
            LOGGER.info("Auto assignment: Assignment rule consumer service stopped, Scheduler ID: {}", schedulerId);
        }
    }

    /**
     * A task which is responsible to do the auto-assignment actions.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class AutoAssignmentTask extends Task {

        private final BackgroundConsumer assignmentExecutionConsumer;

        private AutoAssignmentTask(final Configurations configurations, final TaskController controller, final Executor executor) {
            super(controller);
            final int maxRecord = configurations.optInt("max.records", NumericalConstant.TEN);
            final int poolDuration = configurations.optInt("poll.duration", NumericalConstant.FIVE_THOUSAND);
            final int maxQueueSize = configurations.optInt("max.queue.size", NumericalConstant.TEN);

            final ConsumerConfig consumerConfig = new KafkaConsumerConfig("tlc-crm-auto-assignment-request-consumer", false,
                    List.of(CoreQueueTopics.getContactCreatedTopic(QueueModel.Type.KAFKA),
                            CoreQueueTopics.getCompanyCreatedTopic(QueueModel.Type.KAFKA),
                            CoreQueueTopics.getDealCreatedTopic(QueueModel.Type.KAFKA)), new AutoAssignmentRequestProcessor(executor),
                    new ConsumerConfig.PollConfig(maxRecord, poolDuration, maxQueueSize), false);
            this.assignmentExecutionConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Which controls the trigger task.
         */
        @Override
        public void execute(final TaskController taskController) {
            assignmentExecutionConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            assignmentExecutionConsumer.stop();
            return true;
        }
    }

    /**
     * A controller which controls the execution of auto-assignment.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class AutoAssignmentTaskController extends TaskController {

        private AutoAssignmentTaskController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return the next interval time for the task is executed in milliseconds.
         */
        @Override
        public long getNextInterval() {
            return 0;
        }
    }
}
