package com.tlc.social.server.facebook.internal.webhook.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import com.tlc.social.common.queue.SocialQueueTopics;
import com.tlc.social.server.facebook.internal.webhook.listener.FBWebhookPayloadListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * A service responsible for starting, stopping and managing services to process fb webhook payload
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class FBWebhookPayloadProcessor implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(FBWebhookPayloadProcessor.class);
    private static final String WEBHOOK_DATA_CONSUME_THREAD_POOL = "fb-webhook-data-consume-pool";
    private static final String TASK_CONTROLLER_NAME = "fb-webhook-data-processor";

    private static final String WEBHOOK_DATA_CONSUMER_NAME = "fb-webhook-data-consumer";
    private static final String CONSUMER_MAX_RECORDS_KEY = "facebook.webhook.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION_KEY = "facebook.webhook.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE_KEY = "facebook.webhook.consumer.max.queue.size";
    private static final String CONSUMER_COUNT_KEY = "facebook.webhook.consumer.count";

    private final SchedulerService schedulerService;

    private Map<Long, Task> webhookDataLoadTasks;

    private FBWebhookPayloadProcessor() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link FBWebhookPayloadProcessor}.
     * </p>
     */
    private static final class InstanceHolder {
        private static final FBWebhookPayloadProcessor INSTANCE = new FBWebhookPayloadProcessor();
    }

    /**
     * <p>
     * Gets the instance of {@link FBWebhookPayloadProcessor}
     * </p>
     *
     * @return the instance of {@link FBWebhookPayloadProcessor}
     */
    public static FBWebhookPayloadProcessor getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the configurations
     */
    @Override
    public void start(final Configurations configurations) {
        this.webhookDataLoadTasks = new HashMap<>();
        final int consumerCount = configurations.getInt(CONSUMER_COUNT_KEY);

        for (int index = 0; index < consumerCount; index++) {
            final WebhookDataProcessTask webhookDataProcessTask = new WebhookDataProcessTask(configurations, new WebhookDataProcessTaskController(TASK_CONTROLLER_NAME));
            final Long schedulerId = schedulerService.schedule(WEBHOOK_DATA_CONSUME_THREAD_POOL, webhookDataProcessTask);

            webhookDataLoadTasks.put(schedulerId, webhookDataProcessTask);
            LOGGER.info("Facebook: FB webhook data consumer service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(webhookDataLoadTasks)) {
            for (final Map.Entry<Long, Task> taskEntry : webhookDataLoadTasks.entrySet()) {
                final Task task = taskEntry.getValue();
                final Long schedulerId = taskEntry.getKey();

                schedulerService.removeTask(schedulerId);

                if (Objects.nonNull(task) && task.isRunning()) {
                    task.stopTask();
                }
                LOGGER.info("Facebook: FB webhook data consumer service stopped, Scheduler ID : {}", schedulerId);
            }
        }
    }

    /**
     * <p>
     * A task which is responsible to handle fb webhook payload
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static class WebhookDataProcessTask extends Task {

        private final BackgroundConsumer requestConsumer;

        private WebhookDataProcessTask(final Configurations configurations, final TaskController controller) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS_KEY, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(CONSUMER_POLL_DURATION_KEY, NumericalConstant.THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE_KEY, NumericalConstant.TEN);

            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(WEBHOOK_DATA_CONSUMER_NAME, false,
                    List.of(SocialQueueTopics.getFBWebhookDataConsumeTopic(QueueModel.Type.KAFKA)), new FBWebhookPayloadListener(),
                    new ConsumerConfig.PollConfig(maxRecord, pollDuration, maxQueueSize), false);
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
     * A controller which controls the fb webhook data handling task
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static class WebhookDataProcessTaskController extends TaskController {

        private WebhookDataProcessTaskController(final String schedulerName) {
            super(schedulerName);
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
