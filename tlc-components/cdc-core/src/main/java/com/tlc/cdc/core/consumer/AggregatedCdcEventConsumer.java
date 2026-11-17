package com.tlc.cdc.core.consumer;

import com.tlc.cdc.core.publisher.CdcQueueTopics;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.queue.QueueModel;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;

import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

/**
 * A service implementation responsible for consuming aggregated CDC (Change Data Capture) events
 * from Kafka topics and processing them according to business requirements.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AggregatedCdcEventConsumer implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatedCdcEventConsumer.class);
    private static final String CONSUMER_COUNT = "count";

    private final SchedulerService schedulerService;
    private final Map<Long, Task> cdcTaskMap;

    public AggregatedCdcEventConsumer() {
        this.schedulerService = SchedulerServiceProvider.get();
        this.cdcTaskMap = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        final int consumerCount = configurations.optInt(CONSUMER_COUNT, NumericalConstant.ONE);

        for (int index = 0; index < consumerCount; index++) {
            final AggregatedCdcTask task = new AggregatedCdcTask(new AggregatedCdcTaskController("aggregated-cdc-processor"));
            final long schedulerId = schedulerService.schedule("aggregated-cdc-pool", task);

            cdcTaskMap.put(schedulerId, task);
            LOGGER.info("CDC: Aggregated CDC consumer service started, Scheduler ID: {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (!cdcTaskMap.isEmpty()) {
            for (final Map.Entry<Long, Task> taskEntry : cdcTaskMap.entrySet()) {
                final Long taskId = taskEntry.getKey();
                final Task task = taskEntry.getValue();

                schedulerService.removeTask(taskId);

                if (Objects.nonNull(task) && task.isRunning()) {
                    task.stopTask();
                }
                LOGGER.info("CDC: Aggregated CDC consumer service stopped, Scheduler ID: {}", taskId);
            }
        }
    }

    /**
     * A task which is responsible to do the aggregated CDC actions.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class AggregatedCdcTask extends Task {

        private static final String CONSUMER_GROUP = "tlc-aggregated-cdc-request-consumer";

        private final BackgroundConsumer consumer;

        private AggregatedCdcTask(final TaskController controller) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(CONSUMER_GROUP, false,
                    List.of(CdcQueueTopics.aggregatedCdcTopic(QueueModel.Type.KAFKA)), new AggregatedCdcEventProcessor(),
                    new ConsumerConfig.PollConfig(10, 1000, 10), false);
            this.consumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Which controls the CDC task.
         */
        @Override
        public void execute(final TaskController taskController) {
            consumer.run();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            consumer.stop();
            return true;
        }
    }

    /**
     * A controller which controls aggregated CDC event.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class AggregatedCdcTaskController extends TaskController {

        private AggregatedCdcTaskController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return the next interval time for the task is executed in milliseconds.
         */
        @Override
        public long getNextInterval() {
            return 0;
        }
    }
}