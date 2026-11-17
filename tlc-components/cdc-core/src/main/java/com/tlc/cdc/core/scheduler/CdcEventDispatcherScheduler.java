package com.tlc.cdc.core.scheduler;

import com.tlc.cdc.core.publisher.CdcAggregateEventPublisher;
import com.tlc.cdc.core.transaction.CdcTransactionProcessor;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A service implementation responsible for consuming aggregated CDC (Change Data Capture) events
 * from Kafka topics and processing them according to business requirements.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CdcEventDispatcherScheduler implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdcEventDispatcherScheduler.class);

    private final SchedulerService schedulerService;

    private CdcAggregationDispatcherTask cdcAggregationDispatcherTask;
    private Long schedulerId;

    private CdcEventDispatcherScheduler() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    private static class InstanceHolder {
        private static final CdcEventDispatcherScheduler INSTANCE = new CdcEventDispatcherScheduler();
    }

    public static CdcEventDispatcherScheduler getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule("cdc-dispatcher-pool", cdcAggregationDispatcherTask = new CdcAggregationDispatcherTask(new CdcAggregationDispatcherController("presigned-url-cleaner")));

            LOGGER.info("CDC: Aggregated data scheduler service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId)) {
            schedulerService.removeTask(schedulerId);

            if (Objects.nonNull(cdcAggregationDispatcherTask) && cdcAggregationDispatcherTask.isRunning()) {
                schedulerService.removeTask(schedulerId);
                LOGGER.info("CDC: Aggregated data scheduler service stopped, Scheduler ID : {}", schedulerId);
            }
        }

        CdcAggregateEventPublisher.closeProducer();
    }

    /**
     * A task which is responsible to dispatch the CDC aggregated data.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class CdcAggregationDispatcherTask extends Task {

        private final CdcTransactionProcessor cdcTransactionProcessor;

        private CdcAggregationDispatcherTask(final TaskController controller) {
            super(controller);
            this.cdcTransactionProcessor = CdcTransactionProcessor.getInstance();
        }

        /**
         * {@inheritDoc}
         *
         * @param taskController Which controls the trigger task.
         */
        @Override
        public void execute(final TaskController taskController) {
            LOGGER.debug("CDC: Aggregated data dispatcher started");

            try {
                cdcTransactionProcessor.aggregateAndPublish();
                LOGGER.debug("CDC: Aggregated data dispatcher finished");
            } catch (Throwable cause) {
                LOGGER.error("CDC: Aggregated data dispatcher failed, Reason: {}", cause.getMessage(), cause);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean stopTask() {
            return true;
        }
    }

    /**
     * A controller which controls the CDC aggregated data tasks.
     *
     * @author ThalaimalaiPandiyanT
     * @version 1.0
     */
    private static class CdcAggregationDispatcherController extends TaskController {

        private CdcAggregationDispatcherController(final String scheduleName) {
            super(scheduleName);
        }

        /**
         * {@inheritDoc}
         *
         * @return the next interval time for the task is executed in milliseconds.
         */
        @Override
        public long getNextInterval() {
            return TimeUnit.SECONDS.toMillis(5);
        }
    }
}
