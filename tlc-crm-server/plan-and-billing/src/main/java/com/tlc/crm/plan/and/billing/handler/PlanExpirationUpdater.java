package com.tlc.crm.plan.and.billing.handler;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <p>
 * A service that is responsible to update the plan expiration.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class PlanExpirationUpdater implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlanExpirationUpdater.class);
    private static final String THREAD_POOL_NAME = "plan-expiration-pool";
    private static final String TASK_SCHEDULER_NAME = "plan-expiration-updater";

    private final SchedulerService schedulerService;

    private Long schedulerId;

    private static class Instance {
        private static final PlanExpirationUpdater PLAN_EXPIRATION_UPDATER = new PlanExpirationUpdater();
    }

    private PlanExpirationUpdater() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Gets the {@link PlanExpirationUpdater} Instance.
     * </p>
     *
     * @return {@link PlanExpirationUpdater}.
     */
    public static PlanExpirationUpdater getInstance() {
        return Instance.PLAN_EXPIRATION_UPDATER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, new ExpirationUpdateTask(new ExpirationUpdateTaskController()));

            LOGGER.info("Plan expiration update service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);
            LOGGER.info("Plan expiration update service stopped Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible to send the scheduled plan expiration update.
     * </p>
     *
     * @author Sathish
     * @version 1.0
     */
    public static class ExpirationUpdateTask extends Task {

        private final PlanSubscriptionHandler planSubscriptionHandler;
        private final ClusterService clusterService;

        public ExpirationUpdateTask(final TaskController controller) {
            super(controller);
            this.planSubscriptionHandler = PlanSubscriptionHandler.getInstance();
            this.clusterService = ClusterServiceProvider.get();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void execute(final TaskController taskController) {
            try {
                DistributedLock distributedLock = null;

                try {
                    distributedLock = clusterService.getDistributedLock("planExpiration");

                    distributedLock.acquire();
                    planSubscriptionHandler.triggerExpirationProcess();
                    LOGGER.info("Plans And Billings: Plan expiration updated successfully");
                } finally {
                    if (Objects.nonNull(distributedLock)) {
                        distributedLock.release();
                    }
                }
            } catch (Throwable e) {
                LOGGER.info("Plans And Billings: Plan expiration update failed : {}", e.getMessage());
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
     * A controller which controls scheduled plan expiration update task {@link ExpirationUpdateTaskController}.
     * </p>
     *
     * @author Sathish
     * @version 1.0
     */
    public static class ExpirationUpdateTaskController extends TaskController {

        public ExpirationUpdateTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return Interval time in ms.
         */
        @Override
        public long getNextInterval() {
            return 600000;
        }
    }
}
