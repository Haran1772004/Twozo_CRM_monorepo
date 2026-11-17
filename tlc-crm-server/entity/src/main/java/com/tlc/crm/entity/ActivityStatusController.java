package com.tlc.crm.entity;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <p>
 * A service that is responsible to trigger and control the activity status actions.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class ActivityStatusController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityStatusController.class);
    private static final String THREAD_POOL_NAME = "scheduled-activity-status-pool";
    private static final String TASK_SCHEDULER_NAME = "scheduled-activity-status";

    private final SchedulerService schedulerService;

    private Long schedulerId;

    private static class Instance {
        private static final ActivityStatusController INSTANCE = new ActivityStatusController();
    }

    private ActivityStatusController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Gets the {@link ActivityStatusController} Instance.
     * </p>
     *
     * @return {@link ActivityStatusController}.
     */
    public static ActivityStatusController getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, new StatusTask(new StatusTaskController()));

            LOGGER.info("Activity status update service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);
            LOGGER.info("Activity status update service stopped Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to send the scheduled activity status.
     * </p>
     *
     * @author Sathish
     * @version 1.0
     */
    public static class StatusTask extends Task {

        private final ActivityManager activityManager;
        private final ClusterService clusterService;

        public StatusTask(final TaskController controller) {
            super(controller);
            this.activityManager = ActivityManager.getInstance();
            clusterService = ClusterServiceProvider.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            try {
                DistributedLock distributedLock = null;

                try {
                    distributedLock = clusterService.getDistributedLock("activityStatus");

                    distributedLock.acquire();
                    activityManager.updateStatusToOverdue();
                    LOGGER.info("Activity over due status updated");
                } finally {
                    if (Objects.nonNull(distributedLock)) {
                        distributedLock.release();
                    }
                }
            } catch (Throwable e) {
                LOGGER.info("Activity status update action failed : {}", e.getMessage());
                LOGGER.debug("Stack", e);
            }

        }

        /**
         * {@inheritDoc}
         *
         * @return true
         */
        @Override
        public boolean stopTask() {
            return true;
        }
    }

    /**
     * <p>
     * A controller which controls scheduled activity status task {@link StatusTaskController}.
     * </p>
     *
     * @author Sathish
     * @version 1.0
     */
    public static class StatusTaskController extends TaskController {

        public StatusTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return Interval time in ms.
         */
        @Override
        public long getNextInterval() {
            return 60000;
        }
    }
}
