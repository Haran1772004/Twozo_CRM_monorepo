package com.tlc.crm.core.internal.event.email;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.queue.CoreQueueTopics;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.tlc.queue.QueueModel.Type.KAFKA;

/**
 * <p>
 * Service to control the email alert event
 * </p>
 *
 * @author  Mohammed Asif J
 * @version 1.0
 */
public class EmailAlertEventService implements Service {

    private static final Logger LOGGER =  LoggerFactory.getLogger(EmailAlertEventService.class);
    private static final String SCHEDULER_NAME = "email-alert";
    private static final String THREAD_POOL_NAME = "email-alert-event-pool";
    private static final String CONSUMER_NAME = "consumer.name";
    private static final String CONSUMER_COUNT = "consumer.count";
    private static final String CONSUMER_MAX_RECORDS = "consumer.max.records";
    private static final String CONSUMER_POLL_DURATION = "consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "consumer.max.queue.size";

    private final SchedulerService schedulerService;
    private Map<Long, Task> emailAlertEventTaskMap;

    public EmailAlertEventService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        this.emailAlertEventTaskMap = new HashMap<>();
        final int consumerCount = configurations.optInt(CONSUMER_COUNT, NumericalConstant.ONE);

        for (int index = NumericalConstant.ONE; index <= consumerCount; index++) {
            final EmailAlertEventTask emailAlertEventTask = new EmailAlertEventTask(configurations, new EmailAlertEventTaskController());
            final Long schedulerId = schedulerService.schedule(THREAD_POOL_NAME, emailAlertEventTask);

            emailAlertEventTaskMap.put(schedulerId, emailAlertEventTask);
            LOGGER.info("Email alert event service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(emailAlertEventTaskMap)) {
            emailAlertEventTaskMap.forEach((schedulerId, task) -> {
                schedulerService.removeTask(schedulerId);

                if (task.isRunning()) {
                    task.stopTask();
                }
                LOGGER.info("Email alert event service stopped, Scheduler ID : {}", schedulerId);
            });
        }
    }

    /**
     * <p>
     * Task which runs the email alert event consumer in background
     * </p>
     *
     * @author  Mohammed Asif J
     * @version 1.0
     */
    private static class EmailAlertEventTask extends Task {

        private final BackgroundConsumer emailAlertEventConsumer;

        private EmailAlertEventTask(final Configurations configurations, final TaskController taskController) {
            super(taskController);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(
                    configurations.getConfig(CONSUMER_NAME), false,
                    List.of(CoreQueueTopics.getEmailAlertTopic(KAFKA)), new EmailAlertEventListener(),
                    new ConsumerConfig.PollConfig(
                            configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN),
                            configurations.optInt(CONSUMER_POLL_DURATION, NumericalConstant.THOUSAND),
                            configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN)
                    )
            );
            emailAlertEventConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            emailAlertEventConsumer.run();
        }
    }

    /**
     * <p>
     * Controller which controls the {@link EmailAlertEventTask}
     * </p>
     *
     * @author  Mohammed Asif J
     * @version 1.0
     */
    private static class EmailAlertEventTaskController extends TaskController {

        private EmailAlertEventTaskController(){
            super(SCHEDULER_NAME);
        }

        /**
         * @return
         */
        @Override
        public long getNextInterval() {
            return NumericalConstant.NEGATIVE_ONE;
        }
    }
}