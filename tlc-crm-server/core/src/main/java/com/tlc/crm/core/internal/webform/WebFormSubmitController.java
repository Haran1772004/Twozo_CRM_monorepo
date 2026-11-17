package com.tlc.crm.core.internal.webform;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.core.queue.CoreQueueTopics;
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
 * A service that is responsible to trigger and control the webform submit action
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class WebFormSubmitController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebFormSubmitController.class);

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    public WebFormSubmitController() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * An instance of {@link WebformSubmitTask} is started & logged.
     * </p>
     *
     * @param configurations Represents the input.
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new WebformSubmitTask(new WebformSubmitTaskController("webform-submission-request-handler"));
            this.schedulerId = schedulerService.schedule("webform-submission-request-handler-pool", task);

            LOGGER.info("Webform action consumer service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Stops the consumer by removing the task associated with schedulerId.
     * </p>
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }
            LOGGER.info("Webform action consumer service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to do the webform submit action
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class WebformSubmitTask extends Task {

        private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-webform-submission-request-consumer";

        private final BackgroundConsumer requestConsumer;

        private WebformSubmitTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getWebFormSubmitEventTopic(QueueModel.Type.KAFKA)), new WebFormSubmitListener(),
                    new ConsumerConfig.PollConfig(10, 1000, 10));
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
     * A controller which controls the {@link WebformSubmitTask}.
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class WebformSubmitTaskController extends TaskController {

        private WebformSubmitTaskController(final String schedulerName) {
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
