package com.tlc.crm.email.core.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.email.core.consumer.EmailSendActionListener;
import com.tlc.email.common.queue.EmailQueueTopics;
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
 * Listens and handle email send events. Responsible for starting, stopping, and managing the lifecycle of the email send service.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailSendDataLoaderService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSendDataLoaderService.class);

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public EmailSendDataLoaderService() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the input of email sync
     */
    @Override
    public void start(final Configurations configurations) {

        if (Objects.isNull(schedulerId)) {
            this.task = new EmailSendTask(new EmailSendTaskController("email-send-processor"));
            this.schedulerId = schedulerService.schedule("email-send-thread-pool", task);

            LOGGER.debug("Email Sync: Service started, Scheduler ID : {}", schedulerId);
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
            LOGGER.debug("Email Sync: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Responsible for handling the specific task of email sending events.
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class EmailSendTask extends Task {

        private final BackgroundConsumer emailSendConsumer;

        private EmailSendTask(final TaskController controller) {
            super(controller);
            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig("tlc-crm-email-core-email-send-data-load-consumer", false,
                    List.of(EmailQueueTopics.getEmailSendTopic(QueueModel.Type.KAFKA)), new EmailSendActionListener(),
                    new ConsumerConfig.PollConfig(10, 5000, 10));
            emailSendConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
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
     * Responsible for handling the specific task of email sending events
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class EmailSendTaskController extends TaskController {

        private EmailSendTaskController(final String taskControllerName) {
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
