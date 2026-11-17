package com.tlc.crm.common.internal.export;

import com.tlc.commons.executor.Executor;
import com.tlc.commons.executor.ExecutorsHandler;
import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.queue.CommonQueueTopics;
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
 * A service that is responsible to trigger and control the data export consumer actions {{@link  DataExportProcessor}}
 *
 * @author Selvakumar
 * @version 1.0
 */
public final class DataExportConsumerController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataExportConsumerController.class);

    private static final String REQUEST_CONSUMER_NAME = "tlc-crm-common-data-export-request-consumer";
    private static final String THREAD_POOL_NAME = "data-export-pool";
    private static final String TASK_SCHEDULER_NAME = "data-export-processor";
    private static final int MAX_POOL_SIZE = 5;
    private static final String EXECUTOR_NAME = "data-export-executor";
    private static final int POLL_DURATION = 1000;
    private static final int MAX_SIZE = 10;

    private Long schedulerId;

    @Override
    public void start(final Configurations configurations) {
        final SchedulerService schedulerService = SchedulerServiceProvider.get();

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            final Executor taskExecutor = ExecutorsHandler.createTaskExecutor(MAX_POOL_SIZE, EXECUTOR_NAME, false);
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, new DataExportTask(new DataExportTaskController(), taskExecutor));
            LOGGER.info("Data export consumer service started, Scheduler ID : {}", schedulerId);
        }

    }

    @Override
    public void stop() {

        if (Objects.nonNull(schedulerId)) {
            final SchedulerService schedulerService = SchedulerServiceProvider.get();

            if (Objects.nonNull(schedulerService)) {
                schedulerService.removeTask(schedulerId);
                LOGGER.info("Data export consumer service stopped, Scheduler ID : {}", schedulerId);
            }
        }

    }

    /**
     * A task which is responsible to do the export action
     *
     * @author Selvakumar
     * @version 1.0
     */
    private static class DataExportTask extends Task {

        private final BackgroundConsumer dataExportRequestConsumer;

        private DataExportTask(final TaskController controller, final Executor executor) {
            super(controller);
            final ConsumerConfig consumerConfig = new KafkaConsumerConfig(REQUEST_CONSUMER_NAME, false,
                    List.of(CommonQueueTopics.getDataExporterTopic(QueueModel.Type.KAFKA)), new DataExportProcessor(executor),
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
     * A controller which controls the data export task {@link DataExportTask}.
     *
     * @author Selvakumar
     * @version 1.0
     */
    private static class DataExportTaskController extends TaskController {

        private DataExportTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        @Override
        public long getNextInterval() {
            return 0;
        }
    }

}
