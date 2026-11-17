package com.tlc.email.server.controller;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.server.listener.InboundEmailActionListener;
import com.tlc.email.server.service.SyncService;
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
 * {@code InboundEmailService} manages the automatic synchronization of inbound emails.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class InboundEmailService implements SyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InboundEmailService.class);

    private static final String THREAD_POOL_NAME = "inbound-retrieve-pool";
    private static final String TASK_CONTROLLER_NAME = "inbound-retrieve";

    private static final String EXECUTOR_NAME = "inbound.retrieve.thread.name";
    private static final String MAX_POOL_SIZE = "inbound.retrieve.thread.pool.size";
    private static final String IS_DAEMON = "inbound.retrieve.thread.daemon";

    private static final String CONSUMER_NAME = "inbound.retrieve.consumer.name";
    private static final String CONSUMER_MAX_RECORDS = "inbound.retrieve.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION = "inbound.retrieve.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "inbound.retrieve.consumer.max.queue.size";

    private final SchedulerService schedulerService;

    private Long schedulerId;
    private InboundEmailRetrieveTask inboundEmailRetrieveTask;
    private Executor taskExecutor;

    private InboundEmailService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link InboundEmailService}.
     * </p>
     */
    private static class Instance {
        private static final InboundEmailService INSTANCE = new InboundEmailService();
    }

    /**
     * <p>
     * Gets the {@link InboundEmailService} Instance
     * </p>
     *
     * @return {@link InboundEmailService}
     */
    public static InboundEmailService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the given configurations
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            final int maxPoolSize = configurations.optInt(MAX_POOL_SIZE, NumericalConstant.TEN);
            final String name = configurations.getConfig(EXECUTOR_NAME);
            final boolean isDaemon = Boolean.parseBoolean(configurations.getConfig(IS_DAEMON));

            taskExecutor = ExecutorsHandler.createTaskExecutor(maxPoolSize, name, isDaemon);
            this.inboundEmailRetrieveTask = new InboundEmailRetrieveTask(configurations, new InboundEmailTaskController(TASK_CONTROLLER_NAME), taskExecutor);
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, inboundEmailRetrieveTask);

            LOGGER.info("Inbound email service: Inbound email service service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(inboundEmailRetrieveTask) && inboundEmailRetrieveTask.isRunning()) {
                inboundEmailRetrieveTask.stopTask();
            }
            LOGGER.info("Inbound email service: Inbound email service stopped, Scheduler ID : {}", schedulerId);
        }

        if (Objects.nonNull(taskExecutor)) {
            taskExecutor.shutdown();
        }
    }

    /**
     * <p>
     * A task which is responsible for email syncing activity
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class InboundEmailRetrieveTask extends Task {

        private final BackgroundConsumer backgroundConsumer;

        private InboundEmailRetrieveTask(final Configurations configurations, final TaskController controller, final Executor executor) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN);
            final int poolDuration = configurations.optInt(CONSUMER_POLL_DURATION, NumericalConstant.FIVE_THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN);
            final String name = configurations.getConfig(CONSUMER_NAME);

            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(name, false,
                    List.of(EmailQueueTopics.getInboundReceiveTopic(QueueModel.Type.KAFKA)), new InboundEmailActionListener(executor),
                    new ConsumerConfig.PollConfig(maxRecord, poolDuration, maxQueueSize), false);
            this.backgroundConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * <p>
         * Executes the task which is scheduled
         * </p>
         *
         * @param taskController represents the controller of the task
         */
        @Override
        public void execute(final TaskController taskController) {
            backgroundConsumer.run();
        }

        /**
         * {@inheritDoc}
         *
         * @return true
         */
        @Override
        public boolean stopTask() {
            return true;
        }
    }

    /**
     * A controller which controls email syncing task {@link InboundEmailTaskController}.
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class InboundEmailTaskController extends TaskController {

        public InboundEmailTaskController(final String name) {
            super(name);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }
}