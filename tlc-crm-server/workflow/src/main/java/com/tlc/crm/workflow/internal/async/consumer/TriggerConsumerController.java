package com.tlc.crm.workflow.internal.async.consumer;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.queue.CoreQueueTopics;

import com.tlc.crm.workflow.internal.async.req.TriggerRequestProcessor;
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
 * A service that is responsible to trigger and control the workflow triggers consumer actions.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class TriggerConsumerController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerConsumerController.class);

    private final SchedulerService schedulerService;

    private Long triggerSchedulerId;
    private TriggerTask triggerTask;
    private Executor triggerExecutor;

    public TriggerConsumerController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(triggerSchedulerId)) {
            final String triggerDelegatorPoolWithSize = configurations.getConfig("trigger.delegator.pool");
            final String[] poolNameAndSize = triggerDelegatorPoolWithSize.split(":");
            this.triggerExecutor = ExecutorsHandler.createTaskExecutor(Integer.parseInt(poolNameAndSize[1]), poolNameAndSize[0], false);
            this.triggerTask = new TriggerTask(new TriggerTaskController("automation-trigger-processor"), triggerExecutor);
            this.triggerSchedulerId = schedulerService.schedule("automation-trigger-pool", triggerTask);

            LOGGER.info("Workflow: Workflow trigger consumer service started, Scheduler ID: {}", triggerSchedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(triggerSchedulerId)) {
            schedulerService.removeTask(triggerSchedulerId);

            if (Objects.nonNull(triggerTask) && triggerTask.isRunning()) {
                triggerTask.stopTask();
            }
            LOGGER.info("Workflow: Workflow trigger consumer service stopped, Scheduler ID: {}", triggerSchedulerId);
        }

        if (Objects.nonNull(triggerExecutor)) {
            triggerExecutor.shutdown();
        }
    }

    /**
     * A task which is responsible to do the workflow trigger actions.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class TriggerTask extends Task {

        private static final String CONSUMER_GROUP = "tlc-crm-workflow-automation-trigger-request-consumer";

        private final BackgroundConsumer triggerConsumer;

        private TriggerTask(final TaskController controller, final Executor triggerExecutor) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(CONSUMER_GROUP, false,
                    List.of(CoreQueueTopics.getTriggerInitiatorTopic(QueueModel.Type.KAFKA)), new TriggerRequestProcessor(triggerExecutor),
                    new ConsumerConfig.PollConfig(10, 1000, 10), false);
            this.triggerConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Which controls the trigger task.
         */
        @Override
        public void execute(final TaskController taskController) {
            triggerConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            triggerConsumer.stop();
            return true;
        }
    }

    /**
     * A controller which controls trigger task of workflow.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class TriggerTaskController extends TaskController {

        private TriggerTaskController(final String scheduleName) {
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
