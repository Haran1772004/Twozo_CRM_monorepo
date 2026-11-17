package com.tlc.social.server.whatsapp.internal.webhook.controller;

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
import com.tlc.social.server.whatsapp.internal.webhook.listener.WATemplateSyncListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * A service responsible for starting, stopping and managing services to process WhatsApp template payload
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public class WATemplateDataProcessService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(WATemplateDataProcessService.class);
    private static final String TEMPLATE_DATA_CONSUME_THREAD_POOL = "wa-template-sync-pool";
    private static final String TASK_CONTROLLER_NAME = "wa-template-data-processor";

    private static final String TEMPLATE_DATA_CONSUMER_NAME = "tlc-social-server-wa-template-data-consumer";
    private static final String CONSUMER_MAX_RECORDS_KEY = "whatsapp.template.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION_KEY = "whatsapp.template.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE_KEY = "whatsapp.template.consumer.max.queue.size";

    private final SchedulerService schedulerService;

    private Task task;
    private Long schedulerId;

    private WATemplateDataProcessService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link WAWebhookDataProcessService}.
     * </p>
     */
    private static final class InstanceHolder {
        private static final WATemplateDataProcessService INSTANCE = new WATemplateDataProcessService();
    }

    /**
     * <p>
     * Gets the instance of {@link WATemplateDataProcessService}
     * </p>
     *
     * @return the instance of {@link WATemplateDataProcessService}
     */
    public static WATemplateDataProcessService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the configurations
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new TemplateDataProcessTask(configurations, new TemplateDataProcessTaskController(TASK_CONTROLLER_NAME));
            this.schedulerId = schedulerService.schedule(TEMPLATE_DATA_CONSUME_THREAD_POOL, task);

            LOGGER.info("Whatsapp: WhatsApp template data consumer service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.info("Whatsapp: WhatsApp template data consumer service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to handle WhatsApp template payload
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static class TemplateDataProcessTask extends Task {

        private final BackgroundConsumer requestConsumer;

        private TemplateDataProcessTask(final Configurations configurations, final TaskController controller) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS_KEY, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(CONSUMER_POLL_DURATION_KEY, NumericalConstant.THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE_KEY, NumericalConstant.TEN);

            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(TEMPLATE_DATA_CONSUMER_NAME, false,
                    List.of(SocialQueueTopics.getWATemplateSyncTopic(QueueModel.Type.KAFKA)), new WATemplateSyncListener(),
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
     * A controller which controls the WhatsApp template data handling task
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static class TemplateDataProcessTaskController extends TaskController {

        private TemplateDataProcessTaskController(final String schedulerName) {
            super(schedulerName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getNextInterval() {
            return 5000;
        }
    }
}