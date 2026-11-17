package com.tlc.crm.core.notification.email.internal.controller;

import com.tlc.cluster.DistributedLock;
import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;

import com.tlc.commons.service.Service;
import com.tlc.commons.util.Configurations;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import com.tlc.crm.core.notification.email.internal.ScheduledEmailService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * A service that is responsible to trigger and control the email trash related actions
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailTrashController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTrashController.class);
    private static final String THREAD_POOL_NAME = "email-trash-pool";
    private static final String TASK_SCHEDULER_NAME = "email-trash";
    private final SchedulerService schedulerService;
    private Long schedulerId;

    private static class Instance {
        private static final EmailTrashController INSTANCE = new EmailTrashController();
    }

    private EmailTrashController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Gets the {@link EmailTrashController} Instance
     * </p>
     *
     * @return {@link EmailTrashController}
     */
    public static EmailTrashController getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(schedulerId) && Objects.nonNull(schedulerService)) {
            this.schedulerId = schedulerService.schedule(THREAD_POOL_NAME, new EmailTrashTask(new EmailTrashTaskController()));

            LOGGER.debug("Trash service started, Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(schedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(schedulerId);

            LOGGER.debug("Trash service stopped Scheduler ID : {}", schedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible for email trash activity
     * </p>
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    private static class EmailTrashTask extends Task {

        private final ScheduledEmailService scheduledEmailService;
        private final ClusterService clusterService;

        private EmailTrashTask(final TaskController controller) {
            super(controller);
            scheduledEmailService = ScheduledEmailService.getInstance();
            clusterService = ClusterServiceProvider.get();
        }

        /**
         * <p>
         * Executes the task which is scheduled
         * </p>
         *
         * @param taskController represents the controller of the task
         */
        @Override
        public void execute(final TaskController taskController) {
            try {
                DistributedLock distributedLock = null;

                try {
                    distributedLock = clusterService.getDistributedLock("trash-mail");

                    distributedLock.acquire();
                    scheduledEmailService.deleteScheduledMails();
                } finally {
                    if (Objects.nonNull(distributedLock)) {
                        distributedLock.release();
                    }
                }
            } catch (Throwable e) {
                LOGGER.debug("Email Delete action failed : {}", e.getMessage());
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
     * A controller which controls trash task {@link EmailTrashTask}.
     *
     * @author NandiniRakAS
     * @version 1.0
     */
    public static class EmailTrashTaskController extends TaskController {

        public EmailTrashTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return TimeUnit.MINUTES.toMillis(60);
        }
    }
}