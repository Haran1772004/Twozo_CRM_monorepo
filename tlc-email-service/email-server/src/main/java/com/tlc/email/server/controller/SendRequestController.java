package com.tlc.email.server.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.server.listener.EmailSendRequestActionListener;

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
 * A service that is responsible to trigger and control the email sync status actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class SendRequestController implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendRequestController.class);
    private static final String EMAIL_SEND_THREAD_POOL = "provider-send-request-pool";
    private static final String EMAIL_SEND_REQUEST_CONSUMER_NAME = "tlc-email-server-provider-send-request-consumer";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    private SendRequestController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link SendRequestController}.
     * </p>
     */
    private static final class Instance {
        private static final SendRequestController INSTANCE = new SendRequestController();
    }

    /**
     * <p>
     * Gets the {@link SendRequestController} Instance
     * </p>
     *
     * @return {@link SendRequestController}
     */
    public static SendRequestController getInstance() {
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
            this.task = new EmailSendRequestTask(new EmailSendRequestTaskController("email-send-processor"));
            this.schedulerId = schedulerService.schedule(EMAIL_SEND_THREAD_POOL, task);

            LOGGER.info("Email Send: Service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.info("Email send: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Responsible for executing the task of email sending events.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class EmailSendRequestTask extends Task {

        private final BackgroundConsumer emailSendConsumer;

        private EmailSendRequestTask(final TaskController controller) {
            super(controller);
            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(EMAIL_SEND_REQUEST_CONSUMER_NAME, false,
                    List.of(EmailQueueTopics.getSendRequestProviderTopic(QueueModel.Type.KAFKA)), new EmailSendRequestActionListener(),
                    new ConsumerConfig.PollConfig(10, 5000, 10));
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
     * Responsible for handling the task of email sending events
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class EmailSendRequestTaskController extends TaskController {

        private EmailSendRequestTaskController(final String taskControllerName) {
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
