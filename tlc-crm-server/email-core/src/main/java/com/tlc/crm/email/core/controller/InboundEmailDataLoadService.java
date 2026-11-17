package com.tlc.crm.email.core.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.email.core.consumer.InboundEmailDataLoadActionListener;
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

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Handles the lifecycle of inbound email data load tasks.
 * Manages a scheduler to periodically execute tasks for consuming
 * inbound email load events from the queue and processing them.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InboundEmailDataLoadService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundEmailDataLoadService.class);

    private static final String THREAD_POOL_NAME = "inbound-email-load-pool";
    private static final String TASK_CONTROLLER_NAME = "inbound-email-load";

    private static final String CONSUMER_NAME = "inbound.load.consumer.name";
    private static final String CONSUMER_MAX_RECORDS = "inbound.load.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION = "inbound.load.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "inbound.load.consumer.max.queue.size";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public InboundEmailDataLoadService() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input configuration for the email data load
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(schedulerId)) {
            this.task = new InboundEmailDataLoadTask(configurations, new InboundEmailDataLoadTaskController(TASK_CONTROLLER_NAME));
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.debug("Inbound Email Data Load: Service started, Scheduler ID : {}", schedulerId);
        }
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
            LOGGER.debug("Inbound Email Data Load: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Represents the task responsible for consuming inbound email data load events
     * from the queue and delegating them to the appropriate listener.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class InboundEmailDataLoadTask extends Task {

        private final BackgroundConsumer emailDataLoadConsumer;

        private InboundEmailDataLoadTask(final Configurations configurations, final TaskController controller) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(CONSUMER_POLL_DURATION, NumericalConstant.FIVE_THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN);
            final String name = configurations.getConfig(CONSUMER_NAME);

            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(
                    name,
                    false,
                    List.of(EmailQueueTopics.getDataLoadTopic(QueueModel.Type.KAFKA, SyncJobType.SMART_BCC)),
                    new InboundEmailDataLoadActionListener(),
                    new ConsumerConfig.PollConfig(maxRecord, pollDuration, maxQueueSize)
            );
            emailDataLoadConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            emailDataLoadConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            emailDataLoadConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * Task controller responsible for managing the execution flow of
     * inbound email data load tasks. Ensures the tasks are executed in a
     * controlled manner.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class InboundEmailDataLoadTaskController extends TaskController {

        private InboundEmailDataLoadTaskController(final String taskControllerName) {
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