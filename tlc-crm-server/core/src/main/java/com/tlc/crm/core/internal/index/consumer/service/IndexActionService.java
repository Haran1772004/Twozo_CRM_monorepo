package com.tlc.crm.core.internal.index.consumer.service;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.internal.index.consumer.IndexActionListener;
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
 * A consumer to control the actions of indexes.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class IndexActionService implements Service {
    private static final Logger LOGGER = LoggerFactory.getLogger(IndexActionService.class);
    private static final String THREAD_POOL_NAME = "index-action-performer-pool";
    private static final String TASK_SCHEDULER_NAME = "index-action-processor";

    private static final String CONSUMER_NAME = "consumer.name";
    private static final String CONSUMER_MAX_RECORDS = "consumer.max.records";
    private static final String CONSUMER_POLL_DURATION = "consumer.poll.duration";
    private static final String CONSUMER_MAX_QUEUE_SIZE = "consumer.max.queue.size";
    private static final String CONSUMER_COUNT = "consumer.count";

    private final SchedulerService schedulerService;
    private Map<Long, Task> indexActionTaskMap;

    public IndexActionService() {
        schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * An instance of {@link IndexActionTask} is started & logged.
     * </p>
     *
     * @param configurations Represents the input.
     */
    @Override
    public void start(final Configurations configurations) {
        this.indexActionTaskMap = new HashMap<>();
        final int consumerCount = configurations.optInt(CONSUMER_COUNT, NumericalConstant.ONE);

        for (int index = NumericalConstant.ZERO; index < consumerCount; index++) {
            final IndexActionTask indexActionTask = new IndexActionTask(configurations, new IndexActionTaskController());
            final Long indexActionTaskId = schedulerService.schedule(THREAD_POOL_NAME, indexActionTask);

            indexActionTaskMap.put(indexActionTaskId, indexActionTask);
            LOGGER.info("Index action consumer service started, Scheduler ID : {}", indexActionTaskId);
        }
    }

    /**
     * <p>
     * Stops the consumer by removing the task associated with schedulerId.
     * </p>
     */
    public void stop() {

        if (Objects.nonNull(indexActionTaskMap)) {

            for (final Map.Entry<Long, Task> taskEntry : indexActionTaskMap.entrySet()) {
                final Long taskId = taskEntry.getKey();
                final Task task = taskEntry.getValue();

                schedulerService.removeTask(taskId);
                if (task.isRunning()) {
                    task.stopTask();
                }
                LOGGER.info("Index action consumer service stopped, Scheduler ID : {}", taskId);
            }
        }
    }

    /**
     * <p>
     * A task which is responsible to control consumer actions.
     * </p>
     *
     * @author Ajay
     * @version 1.0
     */
    private static class IndexActionTask extends Task {

        private final BackgroundConsumer modelRequestConsumer;

        private IndexActionTask(final Configurations configurations, final TaskController controller) {
            super(controller);
            final int maxRecord = configurations.optInt(CONSUMER_MAX_RECORDS, NumericalConstant.TEN);
            final int pollDuration = configurations.optInt(CONSUMER_POLL_DURATION, NumericalConstant.THOUSAND);
            final int maxQueueSize = configurations.optInt(CONSUMER_MAX_QUEUE_SIZE, NumericalConstant.TEN);
            final String name = configurations.getConfig(CONSUMER_NAME);

            final KafkaConsumerConfig consumerConfig = new KafkaConsumerConfig(name, false,
                    List.of(CoreQueueTopics.getIndexActionPerformerTopic(QueueModel.Type.KAFKA)), new IndexActionListener(),
                    new ConsumerConfig.PollConfig(maxRecord, pollDuration, maxQueueSize));
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
     * A controller which controls the {@link IndexActionTask}.
     * </p>
     *
     * @author Ajay
     * @version 1.0
     */
    private static class IndexActionTaskController extends TaskController {

        private IndexActionTaskController() {
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
