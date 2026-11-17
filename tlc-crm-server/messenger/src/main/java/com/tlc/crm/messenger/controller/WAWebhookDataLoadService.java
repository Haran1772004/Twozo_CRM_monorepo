package com.tlc.crm.messenger.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.messenger.listener.WAWebhookResponseListener;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * A service responsible for starting, stopping and managing services to load Whatsapp webhook payload
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WAWebhookDataLoadService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(WAWebhookDataLoadService.class);

    private final String webhookDataLoadThreadPool = "wa-webhook-data-load-pool";
    private final String taskControllerName = "wa-webhook-data-loader";
    private final String webhookResponseProcessConsumer = "tlc-crm-messager-wa-webhook-response-process-consumer";
    private final String consumerMaxRecordsKey = "whatsapp.webhook.consumer.max.records";
    private final String consumerPollDurationKey = "whatsapp.webhook.consumer.poll.duration";
    private final String consumerMaxQueueSizeKey = "whatsapp.webhook.consumer.max.queue.size";
    private final String consumerCountKey = "whatsapp.webhook.consumer.count";

    private final SchedulerService schedulerService;

    private Map<Long, Task> webhookDataLoadTasks;

    private WAWebhookDataLoadService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link WAWebhookDataLoadService}.
     * </p>
     */
    private static final class InstanceHolder {
        private static final WAWebhookDataLoadService INSTANCE = new WAWebhookDataLoadService();
    }

    /**
     * <p>
     * Gets the instance of {@link WAWebhookDataLoadService}
     * </p>
     *
     * @return the instance of {@link WAWebhookDataLoadService}
     */
    public static WAWebhookDataLoadService getInstance() {
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
        final int consumerCount = configurations.getInt(consumerCountKey);

        for (int index = 0; index < consumerCount; index++) {
            final WebhookDataLoadTask webhookDataLoadTask = new WebhookDataLoadTask(configurations, new WebhookDataLoadTaskController(taskControllerName));
            final Long schedulerId = schedulerService.schedule(webhookDataLoadThreadPool, webhookDataLoadTask);

            webhookDataLoadTasks.put(schedulerId, webhookDataLoadTask);
            LOGGER.info("Whatsapp: Webhook data load consumer service started, Scheduler ID : {}", schedulerId);
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
                LOGGER.info("Whatsapp: Webhook data load consumer service stopped, Scheduler ID : {}", schedulerId);
            }
        }
    }

    /**
     * <p>
     * A task which is responsible to load WhatsApp webhook payload
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private class WebhookDataLoadTask extends Task {

        private final BackgroundConsumer requestConsumer;

        private WebhookDataLoadTask(final Configurations configurations, final TaskController controller) {
            super(controller);
            final int maxRecord = configurations.optInt(consumerMaxRecordsKey, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(consumerPollDurationKey, NumericalConstant.THOUSAND);
            final int maxQueueSize = configurations.optInt(consumerMaxQueueSizeKey, NumericalConstant.TEN);

            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(webhookResponseProcessConsumer, false,
                    List.of(SocialQueueTopics.getWAWebhookResponseProcessTopic(QueueModel.Type.KAFKA)), new WAWebhookResponseListener(),
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
     * A controller which controls the WhatsApp webhook data loading task
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static class WebhookDataLoadTaskController extends TaskController {

        private WebhookDataLoadTaskController(final String schedulerName) {
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
