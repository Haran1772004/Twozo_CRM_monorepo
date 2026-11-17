package com.tlc.email.server.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.email.common.queue.EmailQueueTopics;
import com.tlc.email.server.listener.EmailDomainRequestActionListener;

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
 * Service controller responsible for handling email domain-related asynchronous requests.
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class DomainRequestController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(DomainRequestController.class);

    private static final String DOMAIN_REQUEST_THREAD_POOL = "provider-domain-request-pool";
    private static final String DOMAIN_REQUEST_CONSUMER_NAME =
            "tlc-email-server-provider-domain-request-consumer";

    private final SchedulerService schedulerService;
    private Task task;
    private Long schedulerId;

    /**
     * <p>
     * Private constructor to enforce singleton usage.
     * </p>
     */
    private DomainRequestController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Holds the singleton instance of {@link DomainRequestController}.
     * </p>
     */
    private static final class Instance {
        private static final DomainRequestController INSTANCE =
                new DomainRequestController();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link DomainRequestController}.
     * </p>
     *
     * @return DomainRequestController instance
     */
    public static DomainRequestController getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Starts the background scheduler and registers
     * the domain request consumer task.
     * </p>
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId)) {

            this.task = new DomainRequestTask(new DomainRequestTaskController("email-domain-request-processor"));
            this.schedulerId = schedulerService.schedule(DOMAIN_REQUEST_THREAD_POOL, task);

            LOGGER.info("Domain Request: Service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Stops the scheduler and shuts down the consumer safely.
     * </p>
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {

            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(task) && task.isRunning()) {
                task.stopTask();
            }

            LOGGER.info("Domain Request: Service stopped, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * Task responsible for consuming domain request events
     * from the queue and delegating processing.
     * </p>
     */
    private static class DomainRequestTask extends Task {

        private final BackgroundConsumer domainRequestConsumer;

        private DomainRequestTask(final TaskController controller) {
            super(controller);

            final KafkaConsumerConfig consumerConfig =
                    new KafkaConsumerConfig(
                            DOMAIN_REQUEST_CONSUMER_NAME, false,
                            List.of(EmailQueueTopics.getDomainRequestToProviderTopic(QueueModel.Type.KAFKA)),
                            new EmailDomainRequestActionListener(),
                            new ConsumerConfig.PollConfig(10, 5000, 10)
                    );

            this.domainRequestConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * Continuously polls the queue for domain request messages.
         * </p>
         */
        @Override
        public void execute(final TaskController taskController) {
            domainRequestConsumer.run();
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * Stops the domain request consumer gracefully.
         * </p>
         */
        @Override
        public boolean stopTask() {
            domainRequestConsumer.stop();

            return true;
        }
    }

    /**
     * <p>
     * Controls scheduling behavior for the domain request task.
     * </p>
     */
    private static class DomainRequestTaskController extends TaskController {

        private DomainRequestTaskController(final String taskControllerName) {
            super(taskControllerName);
        }

        /**
         * {@inheritDoc}
         *
         * <p>
         * Returns zero to indicate continuous execution.
         * </p>
         */
        @Override
        public long getNextInterval() {
            return -1;
        }
    }
}