package com.tlc.crm.workflow.v2.internal.async.consumer;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.queue.CoreQueueTopics;

import com.tlc.crm.workflow.v2.internal.async.WorkflowV2ExecutorConfig;
import com.tlc.crm.workflow.v2.internal.async.req.WorkflowV2EmailActionListener;

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
 * The {@code WorkflowV2EmailActionController} is responsible for controlling the email action consumer.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0.0
 */
public final class WorkflowV2EmailActionController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkflowV2EmailActionController.class);

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public WorkflowV2EmailActionController() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        final String[] emailActionPool = WorkflowV2ExecutorConfig.actionControllerPool(configurations).split(":");
        final Executor executor = ExecutorsHandler.createTaskExecutor(Integer.parseInt(emailActionPool[1]), emailActionPool[0], false);

        this.task = new WorkflowV2EmailActionTask(new WorkflowV2EmailActionTaskController("workflow-v2-email-action-processor"), executor);
        this.schedulerId = schedulerService.schedule("automation-action-pool", task);

        LOGGER.info("Workflow V2: Email send action consumer service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.info("Workflow V2: Email send action consumer service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * The {@code WorkflowV2EmailActionTask} is responsible for executing the email action consumer.
     *
     * @author Thalaimalai Pandiyan T
     * @version 1.0.0
     */
    private static final class WorkflowV2EmailActionTask extends Task {

        private final BackgroundConsumer consumer;

        private WorkflowV2EmailActionTask(final TaskController controller, final Executor executor) {
            super(controller);
            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig("tlc-crm-workflow-v2-email-action-consumer", false,
                    List.of(CoreQueueTopics.getEmailActionTopic(QueueModel.Type.KAFKA)), new WorkflowV2EmailActionListener(executor),
                    new ConsumerConfig.PollConfig(10, 5000, 10));
            consumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            consumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            consumer.stop();
            return true;
        }
    }

    /**
     * The {@code WorkflowV2EmailActionTaskController} is responsible for controlling the email action task.
     *
     * @author Thalaimalai Pandiyan T
     * @version 1.0.0
     */
    private static final class WorkflowV2EmailActionTaskController extends TaskController {

        private WorkflowV2EmailActionTaskController(final String taskControllerName) {
            super(taskControllerName);
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
