package com.tlc.crm.workflow.v2.internal.async.consumer;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.queue.CoreQueueTopics;

import com.tlc.crm.workflow.v2.internal.async.WorkflowV2ExecutorConfig;
import com.tlc.crm.workflow.v2.internal.async.req.TriggerRequestProcessorV2;

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
 * V2 trigger Kafka consumer controller (own consumer group, same trigger topic as V1).
 *
 * @author Thalaimalai Pandiyan T
 * @version 2.0
 */
public final class TriggerConsumerControllerV2 implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(TriggerConsumerControllerV2.class);

    private final SchedulerService schedulerService;
    private final Map<Long, Task> triggerTaskMap;

    private Executor triggerExecutor;
    private Executor waitMatcherExecutor;

    public TriggerConsumerControllerV2() {
        this.schedulerService = SchedulerServiceProvider.get();
        this.triggerTaskMap = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the configurations.
     */
    @Override
    public void start(final Configurations configurations) {
        final int consumerCount = WorkflowV2ExecutorConfig.consumerCount(configurations);
        final String[] triggerPool = WorkflowV2ExecutorConfig.triggerDelegatorPool(configurations).split(":");
        final String[] waitMatcherPool = WorkflowV2ExecutorConfig.waitMatcherPool(configurations).split(":");

        this.triggerExecutor = ExecutorsHandler.createTaskExecutor(Integer.parseInt(triggerPool[1]), triggerPool[0], false);
        this.waitMatcherExecutor = ExecutorsHandler.createTaskExecutor(Integer.parseInt(waitMatcherPool[1]), waitMatcherPool[0], false);
        final int waitMatcherBatchSize = WorkflowV2ExecutorConfig.waitMatcherBatchSize(configurations);

        for (int index = 0; index < consumerCount; index++) {
            final TriggerTaskV2 task = new TriggerTaskV2(new TriggerTaskControllerV2("automation-v2-trigger-processor"), triggerExecutor, waitMatcherExecutor, waitMatcherBatchSize);
            final Long schedulerId = schedulerService.schedule("automation-v2-trigger-pool", task);

            triggerTaskMap.put(schedulerId, task);
            LOGGER.info("Workflow V2: Trigger consumer service started, Scheduler ID: {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (!triggerTaskMap.isEmpty()) {
            for (final Map.Entry<Long, Task> taskEntry : triggerTaskMap.entrySet()) {
                final Long taskId = taskEntry.getKey();
                final Task task = taskEntry.getValue();

                schedulerService.removeTask(taskId);

                if (Objects.nonNull(task) && task.isRunning()) {
                    task.stopTask();
                }
                LOGGER.info("Workflow V2: Trigger consumer service stopped, Scheduler ID: {}", taskId);
            }
        }

        if (Objects.nonNull(triggerExecutor)) {
            triggerExecutor.shutdown();
        }

        if (Objects.nonNull(waitMatcherExecutor)) {
            waitMatcherExecutor.shutdown();
        }
    }

    /**
     * Trigger task for the workflow V2.
     *
     * @author Thalaimalai Pandiyan T
     * @version 2.0
     */
    private static class TriggerTaskV2 extends Task {

        private static final String CONSUMER_GROUP = "tlc-crm-workflow-v2-trigger-request-consumer";

        private final BackgroundConsumer triggerConsumer;

        private TriggerTaskV2(final TaskController controller, final Executor triggerExecutor,
                              final Executor waitMatcherExecutor, final int waitMatcherBatchSize) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(CONSUMER_GROUP, false,
                    List.of(CoreQueueTopics.getTriggerInitiatorTopic(QueueModel.Type.KAFKA)),
                    new TriggerRequestProcessorV2(triggerExecutor, waitMatcherExecutor, waitMatcherBatchSize),
                    new ConsumerConfig.PollConfig(10, 1000, 10), false);
            this.triggerConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
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
     * Trigger task controller for the workflow V2.
     *
     * @author Thalaimalai Pandiyan T
     * @version 2.0
     */
    private static class TriggerTaskControllerV2 extends TaskController {

        private TriggerTaskControllerV2(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getNextInterval() {
            return 0;
        }
    }
}
