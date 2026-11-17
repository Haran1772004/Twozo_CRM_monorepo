package com.tlc.email.server.controller.webhook;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.email.common.queue.EmailQueueTopics;

import com.tlc.email.server.internal.webhook.listener.WebhookNotificationListener;
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
 * Receiving and handling incoming webhook notifications from external systems.
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see SyncService
 */
public class WebhookReceiverService implements SyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebhookReceiverService.class);

    private static final String THREAD_POOL_NAME = "webhook-receiver-pool";
    private static final String TASK_CONTROLLER_NAME = "webhook-receiver";

    private static final String EXECUTOR_NAME = "webhook.receiver.thread.name";
    private static final String MAX_POOL_SIZE = "webhook.receiver.thread.pool.size";
    private static final String IS_DAEMON = "webhook.receiver.thread.daemon";

    private static final String CONSUMER_NAME = "webhook.receiver.consumer.name";
    private static final String CONSUMER_MAX_RECORDS = "webhook.receiver.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION = "webhook.receiver.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "webhook.receiver.consumer.max.queue.size";

    private final SchedulerService schedulerService;

    private Task task;
    private Long schedulerId;

    public WebhookReceiverService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link WebhookReceiverService}.
     * </p>
     */
    private static class Instance {
        private static final WebhookReceiverService INSTANCE = new WebhookReceiverService();
    }

    /**
     * <p>
     * Gets the {@link WebhookReceiverService} Instance
     * </p>
     *
     * @return {@link WebhookReceiverService}
     */

    public static WebhookReceiverService getInstance() {
        return WebhookReceiverService.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input of email webhook receiver
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(schedulerId)) {
            final int maxPoolSize = configurations.optInt(MAX_POOL_SIZE, NumericalConstant.TEN);
            final String name = configurations.getConfig(EXECUTOR_NAME);
            final boolean isDaemon = Boolean.parseBoolean(configurations.getConfig(IS_DAEMON));

            final Executor taskExecutor = ExecutorsHandler.createTaskExecutor(maxPoolSize, name, isDaemon);
            this.task = new WebhookReceiverTask(configurations, new WebhookReceiverTaskController(TASK_CONTROLLER_NAME), taskExecutor);
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.info("Webhook Receiver: Service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.info("Webhook Receiver: Webhook receiver service stopped, Scheduler ID : {}", schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("Webhook Receiver: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Responsible for handling the specific task of email webhook receiving events.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class WebhookReceiverTask extends Task {

        private final BackgroundConsumer backgroundConsumer;

        private WebhookReceiverTask(final Configurations configurations, final TaskController controller, final Executor executor) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN);
            final int poolDuration = configurations.optInt(CONSUMER_POLL_DURATION, NumericalConstant.FIVE_THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN);
            final String name = configurations.getConfig(CONSUMER_NAME);

            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(name, false,
                    List.of(EmailQueueTopics.getWebhookEventTopic(QueueModel.Type.KAFKA)), new WebhookNotificationListener(executor),
                    new ConsumerConfig.PollConfig(maxRecord, poolDuration, maxQueueSize), false);
            backgroundConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            backgroundConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            backgroundConsumer.stop();

            return true;
        }
    }

    /**
     * <p>
     * Responsible for handling the specific task of email webhook receiver events. Manages the lifecycle
     * and execution flow of the email webhook receiver tasks and ensures that these tasks are executed in a controlled and orderly manner.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class WebhookReceiverTaskController extends TaskController {

        private WebhookReceiverTaskController(final String taskControllerName) {
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