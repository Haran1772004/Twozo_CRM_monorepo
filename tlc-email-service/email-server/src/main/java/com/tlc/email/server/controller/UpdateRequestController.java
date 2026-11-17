package com.tlc.email.server.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.email.common.queue.EmailQueueTopics;

import com.tlc.email.server.listener.EmailUpdateRequestActionListener;

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
 * A service that is responsible to trigger and control the email update actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class UpdateRequestController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateRequestController.class);
    private static final String EMAIL_UPDATE_THREAD_POOL = "provider-update-request-pool";
    private static final String EMAIL_UPDATE_REQUEST_CONSUMER_NAME = "tlc-email-server-provider-update-request-consumer";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    private UpdateRequestController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link UpdateRequestController}.
     * </p>
     */
    private static final class Instance {
        private static final UpdateRequestController INSTANCE = new UpdateRequestController();
    }

    /**
     * <p>
     * Gets the {@link UpdateRequestController} Instance
     * </p>
     *
     * @return {@link UpdateRequestController}
     */
    public static UpdateRequestController getInstance() {
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
            this.task = new EmailUpdateRequestTask(new EmailUpdateRequestTaskController("email-update-processor"));
            this.schedulerId = schedulerService.schedule(EMAIL_UPDATE_THREAD_POOL, task);

            LOGGER.info("Email Update: Service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.info("Email Update: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Responsible for executing the task of email updating events.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class EmailUpdateRequestTask extends Task {

        private final BackgroundConsumer emailUpdateConsumer;

        private EmailUpdateRequestTask(final TaskController controller) {
            super(controller);
            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(EMAIL_UPDATE_REQUEST_CONSUMER_NAME, false,
                    List.of(EmailQueueTopics.getUpdateRequestProviderTopic(QueueModel.Type.KAFKA)), new EmailUpdateRequestActionListener(),
                    new ConsumerConfig.PollConfig(10, 5000, 10));
            emailUpdateConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Represents the task controller
         */
        @Override
        public void execute(final TaskController taskController) {
            emailUpdateConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            emailUpdateConsumer.stop();

            return true;
        }
    }

    /**
     * <p>
     * Responsible for handling the task of email updating events
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class EmailUpdateRequestTaskController extends TaskController {

        private EmailUpdateRequestTaskController(final String taskControllerName) {
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

