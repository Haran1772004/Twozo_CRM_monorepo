package com.tlc.email.server.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.email.common.queue.EmailQueueTopics;

import com.tlc.email.server.listener.TransactionalEmailEventListener;
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
 * Listens and handle transactional email send events. Responsible for starting, stopping, and managing the lifecycle of the email send service.
 *
 * @author Maharaja S
 * @version 1.0
 */
public class TransactionalMailLoadService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalMailLoadService.class);
    private static final String EMAIL_SEND_THREAD_POOL = "transactional-email-sender-pool";
    private static final String TRANSACTIONAL_EMAIL_REQUEST_CONSUMER_NAME = "tlc-email-server-transactional-email-send-consumer";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    private TransactionalMailLoadService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link TransactionalMailLoadService}.
     * </p>
     */
    private static final class Instance {
        private static final TransactionalMailLoadService INSTANCE = new TransactionalMailLoadService();
    }

    /**
     * Returns the singleton instance of this service.
     *
     * @return {@link TransactionalMailLoadService} instance
     */
    public static TransactionalMailLoadService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input of email sync
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new TransactionalEmailSendTask(
                    new TransactionalEmailSendTaskController("transactional-email-send-processor"));
            this.schedulerId = schedulerService.schedule(EMAIL_SEND_THREAD_POOL, task);

            LOGGER.info("Transactional Email Service started. Scheduler ID: {}", schedulerId);
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
            LOGGER.info("Transactional Email Service stopped. Scheduler ID: {}", schedulerId);
        }
    }

    /**
     * <p>
     * Responsible for executing the task of email sending events.
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static class TransactionalEmailSendTask extends Task {

        private final BackgroundConsumer emailSendConsumer;

        private TransactionalEmailSendTask(final TaskController controller) {
            super(controller);
            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(
                    TRANSACTIONAL_EMAIL_REQUEST_CONSUMER_NAME,
                    false,
                    List.of(EmailQueueTopics.getTransactionalMailTopic(QueueModel.Type.KAFKA)),
                    new TransactionalEmailEventListener(),
                    new ConsumerConfig.PollConfig(10, 5000, 10)
            );

            emailSendConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Represents the task controller
         */
        @Override
        public void execute(final TaskController taskController) {
            emailSendConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            emailSendConsumer.stop();

            return true;
        }
    }

    /**
     * <p>
     * Responsible for handling the task of transactional email sending events
     * </p>
     *
     * @author Maharaja S
     * @version 1.0
     */
    private static class TransactionalEmailSendTaskController extends TaskController {

        private TransactionalEmailSendTaskController(final String taskControllerName) {
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