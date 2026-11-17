package com.tlc.crm.workflow.internal.async.consumer;

import com.tlc.commons.service.Service;

import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;

import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import com.tlc.crm.workflow.internal.async.req.ActionRequestProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * A service that is responsible to trigger the actions of workflow.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ActionConsumerController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionConsumerController.class);

    private final SchedulerService schedulerService;

    private Long actionSchedulerId;
    private ActionTriggerTask actionTask;

    public ActionConsumerController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(actionSchedulerId)) {
            this.actionSchedulerId = schedulerService.schedule("automation-action-pool",
                    actionTask = new ActionTriggerTask(new ActionTaskController("automation-action-processor")));

            LOGGER.info("Workflow: Trigger action consumer service started, Scheduler ID: {}", actionSchedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(actionSchedulerId)) {
            schedulerService.removeTask(actionSchedulerId);

            if (Objects.nonNull(actionTask) && actionTask.isRunning()) {
                actionTask.stopTask();
            }
            LOGGER.info("Workflow: Trigger action consumer service stopped, Scheduler ID: {}", actionSchedulerId);
        }
    }

    /**
     * A task which is responsible to do the workflow actions.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class ActionTriggerTask extends Task {

        private static final String REQUEST_CONSUMER_NAME = "tlc-crm-workflow-automation-action-request-consumer";
        private final BackgroundConsumer actionConsumer;

        private ActionTriggerTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getActionInitiatorTopic(QueueModel.Type.KAFKA)), new ActionRequestProcessor(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
            this.actionConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Which controls the trigger task.
         */
        @Override
        public void execute(final TaskController taskController) {
            actionConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            actionConsumer.stop();
            return true;
        }
    }

    /**
     * A controller which controls action tasks of workflow.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class ActionTaskController extends TaskController {

        private ActionTaskController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return the next interval time for the task is executed in milliseconds.
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }
}
