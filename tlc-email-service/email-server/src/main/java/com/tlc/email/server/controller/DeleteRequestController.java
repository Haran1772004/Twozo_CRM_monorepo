package com.tlc.email.server.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.server.listener.EmailDeleteRequestActionListener;

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
 * A service that is responsible to trigger and control the email delete request actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class DeleteRequestController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteRequestController.class);
    private static final String EMAIL_DELETE_THREAD_POOL = "provider-delete-request-pool";
    private static final String EMAIL_DELETE_REQUEST_CONSUMER_NAME = "tlc-email-server-provider-delete-request-consumer";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    private DeleteRequestController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the instance of {@link DeleteRequestController}.
     * </p>
     */
    private static final class Instance {
        private static final DeleteRequestController INSTANCE = new DeleteRequestController();
    }

    /**
     * <p>
     * Gets the {@link DeleteRequestController} Instance
     * </p>
     *
     * @return {@link DeleteRequestController}
     */
    public static DeleteRequestController getInstance() {
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
            this.task = new EmailDeleteRequestTask(new EmailDeleteRequestTaskController("email-delete-processor"));
            this.schedulerId = schedulerService.schedule(EMAIL_DELETE_THREAD_POOL, task);

            LOGGER.info("Email Delete: Service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.info("Email Delete: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Responsible for executing the task of email delete events.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class EmailDeleteRequestTask extends Task {

        private final BackgroundConsumer emailDeleteConsumer;

        private EmailDeleteRequestTask(final TaskController controller) {
            super(controller);
            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(EMAIL_DELETE_REQUEST_CONSUMER_NAME, false,
                    List.of(EmailQueueTopics.getDeleteRequestProviderTopic(QueueModel.Type.KAFKA)), new EmailDeleteRequestActionListener(),
                    new ConsumerConfig.PollConfig(10, 5000, 10));
            emailDeleteConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Represents the task controller
         */
        @Override
        public void execute(final TaskController taskController) {
            emailDeleteConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            emailDeleteConsumer.stop();

            return true;
        }
    }

    /**
     * <p>
     * Responsible for handling the task of email deleting events
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    private static class EmailDeleteRequestTaskController extends TaskController {

        private EmailDeleteRequestTaskController(final String taskControllerName) {
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

