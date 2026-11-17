package com.tlc.crm.core.internal.event.onboard.service;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.internal.event.onboard.consumer.OrgOnboardingEventListener;
import com.tlc.crm.core.internal.event.onboard.consumer.OrgOnboardingPerformer;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Controls the Kafka consumer for org onboarding event.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class OrgOnboardingService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrgOnboardingService.class);
    private static final String THREAD_POOL_NAME = "org-onboarding-pool";
    private static final String TASK_SCHEDULER_NAME = "org-onboarding-processor";

    private static final String CONSUMER_NAME = "consumer.name";
    private static final String CONSUMER_MAX_RECORDS = "consumer.max.records";
    private static final String CONSUMER_POLL_DURATION = "consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "consumer.max.queue.size";
    private static final String CONSUMER_COUNT = "consumer.count";

    private final SchedulerService schedulerService;

    private Map<Long, Task> taskMap;

    public OrgOnboardingService() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Starts the org onboard consumer task.
     * </p>
     *
     * @param configurations Represents the input configurations.
     */
    @Override
    public void start(final Configurations configurations) {
        this.taskMap = new HashMap<>();
        final int consumerCount = configurations.optInt(CONSUMER_COUNT, NumericalConstant.ONE);

        for (int index = NumericalConstant.ZERO; index < consumerCount; index++) {
            final OrgOnboardActionTask task = new OrgOnboardActionTask(configurations, new OrgOnboardActionTaskController());
            final Long taskId = schedulerService.schedule(THREAD_POOL_NAME, task);

            taskMap.put(taskId, task);
            LOGGER.info("Org onboard action consumer service started, Scheduler ID: {}", taskId);
        }
    }

    /**
     * <p>
     * Stops the org onboard consumer.
     * </p>
     */
    public void stop() {

        if (Objects.nonNull(taskMap)) {

            for (final Map.Entry<Long, Task> taskEntry : taskMap.entrySet()) {
                final Long taskId = taskEntry.getKey();
                final Task task = taskEntry.getValue();

                schedulerService.removeTask(taskId);

                if (task.isRunning()) {
                    task.stopTask();
                }

                LOGGER.info("Org onboard action consumer service stopped, Scheduler ID: {}", taskId);
            }
        }
    }

    /**
     * <p>
     * A task responsible for running the org onboard consumer.
     * </p>
     *
     * @author Ajay.
     * @version 1.0.
     */
    private static class OrgOnboardActionTask extends Task {

        private final BackgroundConsumer modelRequestConsumer;

        private OrgOnboardActionTask(final Configurations configurations, final TaskController controller) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(CONSUMER_POLL_DURATION, NumericalConstant.THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN);
            final String name = configurations.getConfig(CONSUMER_NAME);
            final OrgOnboardingPerformer orgOnboardingPerformer = new OrgOnboardingPerformer(configurations);

            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(name, false,
                    List.of(CoreQueueTopics.getOrgOnboardingEventTopic(QueueModel.Type.KAFKA)),
                    new OrgOnboardingEventListener(orgOnboardingPerformer), new ConsumerConfig.PollConfig(maxRecord, pollDuration, maxQueueSize), false);
            modelRequestConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            modelRequestConsumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            modelRequestConsumer.stop();
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls the {@link OrgOnboardActionTask}.
     * </p>
     *
     * @author Ajay.
     * @version 1.0.
     */
    private static class OrgOnboardActionTaskController extends TaskController {

        private OrgOnboardActionTaskController() {
            super(TASK_SCHEDULER_NAME);
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

