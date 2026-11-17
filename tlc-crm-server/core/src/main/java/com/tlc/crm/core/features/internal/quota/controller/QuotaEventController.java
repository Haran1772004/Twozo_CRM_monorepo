package com.tlc.crm.core.features.internal.quota.controller;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.features.internal.quota.consumer.QuotaEventProcessor;
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
 * A service that is responsible to trigger and control the deal quota consumer actions {{@link  QuotaTask}}
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaEventController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaEventController.class);

    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-quota-deal-request-consumer";
    private static final String THREAD_POOL_NAME = "quota-event-pool";
    private static final String TASK_SCHEDULER_NAME = "quota-event-processor";
    private static final int POLL_DURATION = 1000;
    private static final int MAX_SIZE = 10;

    private Long schedulerId;
    private Executor executor;

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the {@link Configurations} object which can be used to get the configuration values if needed.
     */
    @Override
    public void start(final Configurations configurations) {
        final SchedulerService schedulerService = SchedulerServiceProvider.get();

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.executor = ExecutorsHandler.createTaskExecutor(5, "quota-event-executor", false);
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, new QuotaTask(new QuotTaskController(), executor));

            LOGGER.info("Quota deal event consumer service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {

        if (Objects.nonNull(schedulerId)) {
            final SchedulerService schedulerService = SchedulerServiceProvider.get();

            if (Objects.nonNull(schedulerService)) {
                schedulerService.removeTask(schedulerId);
                LOGGER.info("Quota deal event consumer service stopped, Scheduler ID : {}", schedulerId);
            }

            if (Objects.nonNull(executor)) {
                executor.shutdown();
            }
        }
    }

    /**
     * A task which is responsible to do the deal quota related consumer action
     *
     * @author kishore J
     * @version 1.0
     */
    private static class QuotaTask extends Task {

        private final BackgroundConsumer dataExportRequestConsumer;

        private QuotaTask(final TaskController controller, final Executor executor) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getQuotaEventTopic(QueueModel.Type.KAFKA)), new QuotaEventProcessor(executor),
                    new ConsumerConfig.PollConfig(MAX_SIZE, POLL_DURATION, MAX_SIZE), false);

            dataExportRequestConsumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        @Override
        public void execute(final TaskController taskController) {
            dataExportRequestConsumer.run();
        }

        @Override
        public boolean stopTask() {
            dataExportRequestConsumer.stop();
            return true;
        }
    }

    /**
     * A controller which controls the Deal quota task {@link QuotTaskController}.
     *
     * @author kishore
     * @version 1.0
     */
    private static class QuotTaskController extends TaskController {

        private QuotTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        @Override
        public long getNextInterval() {
            return 0;
        }
    }
}
