package com.tlc.crm.core.segment.consumer;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.queue.QueueModel;
import com.tlc.queue.config.SubscriptionTopic;
import com.tlc.queue.config.consumer.ConsumerConfig;
import com.tlc.queue.config.consumer.ConsumerConfig.PollConfig;
import com.tlc.queue.consumer.BackgroundConsumer;
import com.tlc.queue.kafka.KafkaConsumerConfig;
import com.tlc.queue.service.provider.QueueServiceProvider;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import com.tlc.crm.core.queue.CoreQueueTopics;
import com.tlc.crm.core.segment.listener.SegmentRecomputeEventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Scheduled {@link Service} that starts and stops the background Kafka
 * consumer responsible for draining the segment-recompute topic and
 * dispatching each record to the {@link SegmentRecomputeEventListener}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentRecomputeConsumerService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(SegmentRecomputeConsumerService.class);

    private static final String CONSUMER_GROUP = "tlc-crm-core-segment-recompute-consumer";
    private static final String THREAD_POOL_NAME = "segment-recompute-consumer-pool";
    private static final String TASK_SCHEDULER_NAME = "segment-recompute-processor";
    private static final String WORKER_POOL_NAME = "segment-recompute-worker-pool";

    private static final String CONFIG_WORKER_POOL_SIZE = "segment.recompute.worker.pool.size";
    private static final int DEFAULT_WORKER_POOL_SIZE = 10;

    private final SchedulerService schedulerService;
    private Long schedulerId;
    private SegmentRecomputeTask segmentRecomputeTask;

    private SegmentRecomputeConsumerService() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static final class InstanceHolder {
        private static final SegmentRecomputeConsumerService INSTANCE = new SegmentRecomputeConsumerService();
    }

    public static SegmentRecomputeConsumerService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            final int workerPoolSize = resolveWorkerPoolSize(configurations);
            final Executor workerExecutor = ExecutorsHandler.createTaskExecutor(workerPoolSize, WORKER_POOL_NAME, false);

            LOGGER.info("SegmentRecomputeConsumerController: Initialized worker pool, name={}, size={}", WORKER_POOL_NAME, workerPoolSize);

            this.segmentRecomputeTask = new SegmentRecomputeTask(new SegmentRecomputeTaskController(TASK_SCHEDULER_NAME), workerExecutor);
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, segmentRecomputeTask);

            LOGGER.info("SegmentRecomputeConsumerController started, Scheduler ID: {}, ThreadPool: {}, WorkerPool: {}, WorkerPoolSize: {}",
                schedulerId, THREAD_POOL_NAME, WORKER_POOL_NAME, workerPoolSize);
        }
    }

    /**
     * <p>
     * Resolves the worker pool size from configuration.
     * </p>
     *
     * @param configurations The configurations object
     * @return The worker pool size (default: 10)
     */
    private int resolveWorkerPoolSize(final Configurations configurations) {
        if (Objects.isNull(configurations)) {
            return DEFAULT_WORKER_POOL_SIZE;
        }
        return configurations.optInt(CONFIG_WORKER_POOL_SIZE, DEFAULT_WORKER_POOL_SIZE);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(segmentRecomputeTask) && segmentRecomputeTask.isRunning()) {
                segmentRecomputeTask.stopTask();
            }

            LOGGER.info("SegmentRecomputeConsumerController stopped, Scheduler ID : {}", schedulerId);
            schedulerId = null;
            segmentRecomputeTask = null;
        }
    }

    /**
     * <p>
     * Represents the segment-recalculation background-consumer task.
     * </p>
     *
     * @version 1.0
     */
    private static final class SegmentRecomputeTask extends Task {

        private final BackgroundConsumer consumer;

        private SegmentRecomputeTask(final TaskController controller, final Executor workerExecutor) {
            super(controller);

            final SubscriptionTopic topic = CoreQueueTopics.getSegmentRecomputeTopic(QueueModel.Type.KAFKA);
            final SegmentRecomputeEventListener listener = new SegmentRecomputeEventListener(workerExecutor);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(CONSUMER_GROUP, false, List.of(topic), listener,
                    new PollConfig(10, 1000, 10), false);

            this.consumer = QueueServiceProvider.get().createBackgroundConsumer(consumerConfig);
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         */
        @Override
        public void execute(final TaskController taskController) {
            try {
                consumer.run();
            } catch (final Exception exception) {
                LOGGER.warn("Segment recompute consumer action failed, message: {}", exception.getMessage(), exception);
            }
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         */
        @Override
        public boolean stopTask() {
            try {
                consumer.stop();
            } catch (final Exception exception) {
                LOGGER.warn("Failed to stop segment recompute consumer, message: {}", exception.getMessage(), exception);
            }
            return true;
        }
    }

    /**
     * <p>
     * Represents the segment-recalculation task controller.
     * </p>
     *
     * @version 1.0
     */
    public static final class SegmentRecomputeTaskController extends TaskController {

        /**
         * <p>
         * Constructs a new controller.
         * </p>
         *
         * @param scheduleName Represents the schedule name.
         */
        public SegmentRecomputeTaskController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         */
        @Override
        public long getNextInterval() {
            return 0;
        }
    }
}