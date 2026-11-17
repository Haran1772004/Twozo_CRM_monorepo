package com.tlc.crm.core.features.internal.quota.controller;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.core.features.internal.quota.consumer.QuotaDealEventProcessor;
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
 * A service that is responsible to trigger and control the deal quota consumer actions {{@link  QuotaDealTask}}
 *
 * @author kishore J
 * @version 1.0
 */
public class QuotaDealEventController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuotaDealEventController.class);

    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-core-quota-deal-request-consumer";
    private static final String THREAD_POOL_NAME = "quota-deal-pool";
    private static final String TASK_SCHEDULER_NAME = "quota-deal-processor";
    private static final int POLL_DURATION = 1000;
    private static final int MAX_SIZE = 10;

    private Long schedulerId;

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the {@link Configurations} object which can be used to get the configuration values if needed.
     */
    @Override
    public void start(final Configurations configurations) {
        final SchedulerService schedulerService = SchedulerServiceProvider.get();

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, new QuotaDealTask(new QuotaDealTaskController()));

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
        }
    }

    /**
     * A task which is responsible to do the deal quota related consumer action
     *
     * @author kishore J
     * @version 1.0
     */
    private static class QuotaDealTask extends Task {

        private final BackgroundConsumer dataExportRequestConsumer;

        private QuotaDealTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CoreQueueTopics.getQuotaDealEventTopic(QueueModel.Type.KAFKA)), new QuotaDealEventProcessor(),
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
     * A controller which controls the Deal quota task {@link QuotaDealTaskController}.
     *
     * @author kishore
     * @version 1.0
     */
    private static class QuotaDealTaskController extends TaskController {

        private QuotaDealTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        @Override
        public long getNextInterval() {
            return 0;
        }
    }
}
