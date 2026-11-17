package com.tlc.crm.common.iam.cleaner;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.iam.api.manager.ActionLimitManager;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import java.util.Collection;
import java.util.Objects;

/**
 * <p>
 * A service that is responsible to trigger and control the action limit cleanup actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public final class ActionLimitCleaner implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionLimitCleaner.class);
    private static final String THREAD_POOL_NAME = "action-limit-cleaner-pool";
    private static final String TASK_SCHEDULER_NAME = "action-limit-cleaner";

    private Long schedulerId;

    private static class Instance {
        private static final ActionLimitCleaner INSTANCE = new ActionLimitCleaner();
    }

    private ActionLimitCleaner() {
    }

    public static ActionLimitCleaner getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        final SchedulerService schedulerService = SchedulerServiceProvider.get();

        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, new ActionLimitTask(new ActionLimitController()));
            LOGGER.info("Action limit cleaner service started, Scheduler ID : {}", schedulerId);
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
                LOGGER.info("Action limit cleaner service stopped, Scheduler ID : {}", schedulerId);
            }
        }
    }

    /**
     * <p>
     * A task which is responsible to do the action limits cleanup activity.
     * </p>
     *
     * @author Sathish
     * @version 1.0
     */
    private static class ActionLimitTask extends Task {

        private static final int BATCH_SIZE = 1000;

        private final ClusterService clusterService;
        private final ActionLimitManager actionLimitManager;

        private ActionLimitTask(final TaskController controller) {
            super(controller);
            clusterService = ClusterServiceProvider.get();
            actionLimitManager = ActionLimitManager.getInstance();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            try {
                DistributedLock distributedLock = null;

                try {
                    distributedLock = clusterService.getDistributedLock(TASK_SCHEDULER_NAME);
                    distributedLock.acquire();
                    cleanupExpiredActionLimits();
                } finally {
                    if (Objects.nonNull(distributedLock)) {
                        distributedLock.release();
                    }
                }
            } catch (Throwable e) {
                LoggerUtil.logError(LOGGER, e, "Action limit cleanup activity failed, Error: {}", e.getMessage());
            }
        }

        /**
         * Deletes the expired action limits
         */
        private void cleanupExpiredActionLimits() {
            LOGGER.info("Action limit count cleanup activity started");

            while (true) {
                final long expiredActionLimitsCount = actionLimitManager.getExpiredActionLimitsCount();

                LOGGER.info("Number of expired action limits identified : {}", expiredActionLimitsCount);

                if (expiredActionLimitsCount > 0) {
                    final Collection<Long> actionLimitIds = actionLimitManager.getExpiredActionLimitIds(BATCH_SIZE);

                    LOGGER.info("Deleting expired action limits : {}", actionLimitIds.size());
                    actionLimitManager.delete(actionLimitIds);

                    if (actionLimitIds.size() < BATCH_SIZE) {
                        break;
                    }
                } else {
                    break;
                }
            }
            LOGGER.info("Action limit cleanup activity completed");
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
     * A controller which controls the action limit cleaner task {@link ActionLimitTask}.
     *
     * @author Sathish
     * @version 1.0
     */
    private static class ActionLimitController extends TaskController {

        private ActionLimitController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getNextInterval() {
            return 300000; //5 minutes
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long getActualScheduledTime() {
            final ZonedDateTime currentDateTime = ZonedDateTime.now();

            // Calculate how many minutes need to be added to round up to the nearest 5
            final int minute = currentDateTime.getMinute();
            final int minutesToAdd = (5 - (minute % 5)) % 5;

            // Round up to the nearest 5 minutes
            final ZonedDateTime roundedDateTime = currentDateTime
                    .plusMinutes(minutesToAdd) // Add necessary minutes to round up
                    .truncatedTo(ChronoUnit.MINUTES); // Remove seconds and nanoseconds

            return roundedDateTime.toInstant().toEpochMilli();
        }
    }
}

