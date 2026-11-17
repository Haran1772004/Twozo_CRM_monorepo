package com.tlc.crm.email.core.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.email.core.consumer.EmailDomainRequestActionHandler;
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
 * Service responsible for scheduling and managing the execution of domain data load events.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailDomainRequestActionController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailDomainRequestActionController.class);

    private static final String THREAD_POOL_NAME = "email-domain-event-pool";
    private static final String TASK_CONTROLLER_NAME = "email-domain-event-task";

    private static final String CONSUMER_MAX_RECORDS = "domain-load.consumer.max.records";
    private static final String CONSUMER_POLL_DURATION = "domain-load.consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "domain-load.consumer.max.queue.size";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    /**
     * <p>
     * Constructs the DomainDataLoadService and initializes the scheduler service.
     * </p>
     */
    public EmailDomainRequestActionController() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Starts the domain data load service and schedules the domain data load task.
     * </p>
     *
     * @param configurations configuration object containing consumer settings
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {
            this.task = new DomainLoadTask(new DomainLoadTaskController(TASK_CONTROLLER_NAME), configurations);
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, task);

            LOGGER.debug("Domain Sync: Service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Stops the domain data load service and terminates the scheduled task.
     * </p>
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }

            LOGGER.debug("Domain Sync: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Internal task class responsible for executing the domain data load.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class DomainLoadTask extends Task {

        private final BackgroundConsumer domainLoadConsumer;

        /**
         * <p>
         * Constructs the DomainLoadTask with the given controller and configuration.
         * </p>
         *
         * @param controller     the task controller
         * @param configurations configuration values for consumer settings
         */
        private DomainLoadTask(final TaskController controller, final Configurations configurations) {
            super(controller);

            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(CONSUMER_POLL_DURATION, NumericalConstant.FIVE_THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN);

            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(
                    "tlc-crm-server-domain-request-action-consumer",
                    false,
                    List.of(EmailQueueTopics.getActionRequestedTopic(QueueModel.Type.KAFKA)),
                    new EmailDomainRequestActionHandler(),
                    new ConsumerConfig.PollConfig(maxRecord, pollDuration, maxQueueSize)
            );

            this.domainLoadConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * <p>
         * Executes the domain load task by fetching all domains and DNS records and running the background consumer.
         * </p>
         *
         * @param taskController the task controller
         */
        @Override
        public void execute(final TaskController taskController) {
            domainLoadConsumer.run();
        }

        /**
         * <p>
         * Stops the execution of the domain load task and the background consumer.
         * </p>
         *
         * @return {@code true} once the consumer is stopped
         */
        @Override
        public boolean stopTask() {
            domainLoadConsumer.stop();

            return true;
        }
    }

    /**
     * <p>
     * Task controller class for managing the scheduling behavior of the domain load task.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private static class DomainLoadTaskController extends TaskController {

        private DomainLoadTaskController(final String taskControllerName) {
            super(taskControllerName);
        }

        @Override
        public long getNextInterval() {
            return -1;
        }
    }
}