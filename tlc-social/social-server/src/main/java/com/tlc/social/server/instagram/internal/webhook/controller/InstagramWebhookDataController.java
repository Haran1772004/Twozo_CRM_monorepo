package com.tlc.social.server.instagram.internal.webhook.controller;

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
import com.tlc.social.server.instagram.internal.webhook.listener.InstagramWebhookPayloadListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Pertains to start/stop/manage instagram webhook data related services.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class InstagramWebhookDataController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstagramWebhookDataController.class);
    private static final String WEBHOOK_DATA_CONSUME_THREAD_POOL = "instagram-webhook-data-consume-pool";
    private static final String TASK_CONTROLLER_NAME = "instagram-webhook-data-processor";

    private static final String WEBHOOK_DATA_CONSUMER_NAME = "tlc-social-server-instagram-webhook-data-consumer";
    private static final String CONSUMER_MAX_RECORDS_KEY = "instagram.webhook.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION_KEY = "instagram.webhook.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE_KEY = "instagram.webhook.consumer.max.queue.size";
    private static final String CONSUMER_COUNT_KEY = "instagram.webhook.consumer.count";

    private final SchedulerService schedulerService;

    private Map<Long, Task> webhookDataLoadTasks;

    private InstagramWebhookDataController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static final class Instance {
        private static final InstagramWebhookDataController INSTANCE = new InstagramWebhookDataController();
    }

    /**
     * <p>
     * Gets {@link InstagramWebhookDataController}.
     * </p>
     *
     * @return {@link InstagramWebhookDataController}.
     */
    public static InstagramWebhookDataController getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations {@link Configurations}.
     */
    @Override
    public void start(final Configurations configurations) {
        this.webhookDataLoadTasks = new HashMap<>();
        final int consumerCount = configurations.getInt(CONSUMER_COUNT_KEY);

        for (int index = 0; index < consumerCount; index++) {
            final WebhookDataProcessTask webhookDataProcessTask = new WebhookDataProcessTask(configurations, new WebhookDataProcessTaskController(TASK_CONTROLLER_NAME));
            final Long schedulerId = schedulerService.schedule(WEBHOOK_DATA_CONSUME_THREAD_POOL, webhookDataProcessTask);

            webhookDataLoadTasks.put(schedulerId, webhookDataProcessTask);
            LOGGER.info("Instagram: Webhook data consumer service started, Scheduler ID : {}", schedulerId);
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
                LOGGER.info("Instagram: Webhook data consumer service stopped, Scheduler ID : {}", schedulerId);
            }
        }
    }

    /**
     * <p>
     * Handles instagram webhook payload task.
     * </p>
     *
     * @author Ajay.
     * @version 1.0.
     */
    private static class WebhookDataProcessTask extends Task {

        private final BackgroundConsumer requestConsumer;

        private WebhookDataProcessTask(final Configurations configurations, final TaskController controller) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS_KEY, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(CONSUMER_POLL_DURATION_KEY, NumericalConstant.THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE_KEY, NumericalConstant.TEN);

            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(WEBHOOK_DATA_CONSUMER_NAME, false,
                    List.of(SocialQueueTopics.getInstagramWebhookDataConsumeTopic(QueueModel.Type.KAFKA)), new InstagramWebhookPayloadListener(),
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
     * Controls the insta webhook data handling task
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
