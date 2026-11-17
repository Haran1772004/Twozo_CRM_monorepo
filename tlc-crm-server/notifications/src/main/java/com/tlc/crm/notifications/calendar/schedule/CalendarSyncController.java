package com.tlc.crm.notifications.calendar.schedule;

import com.tlc.cluster.DistributedLock;

import com.tlc.cluster.service.ClusterService;
import com.tlc.cluster.service.provider.ClusterServiceProvider;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.service.Service;

import com.tlc.commons.util.Configurations;
import com.tlc.crm.notifications.calendar.internal.status.CalendarErrorCodes;
import com.tlc.crm.notifications.calendar.internal.CalendarSyncService;

import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <p>
 * A service that is responsible to trigger and control the calendar syncing actions
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class CalendarSyncController implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarSyncController.class);
    private static final String THREAD_POOL_NAME = "calendar-events-sync-pool";
    private static final String TASK_SCHEDULER_NAME = "calendar-events-sync-processor";

    private final SchedulerService schedulerService;

    private Long syncSchedulerId;
    private CalendarSyncTask calendarSyncTask;

    private static class Instance {
        private static final CalendarSyncController INSTANCE = new CalendarSyncController();
    }

    private CalendarSyncController() {
        this.schedulerService = SchedulerServiceProvider.get();
    }

    /**
     * <p>
     * Gets the {@link CalendarSyncController} Instance
     * </p>
     *
     * @return {@link CalendarSyncController}
     */
    public static CalendarSyncController getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final Configurations configurations) {
        if (Objects.isNull(syncSchedulerId) && Objects.nonNull(schedulerService)) {
            this.calendarSyncTask = new CalendarSyncTask(new CalendarSyncTaskController());
            this.syncSchedulerId = schedulerService.schedule(THREAD_POOL_NAME, calendarSyncTask);
            LOGGER.debug("Scheduled calendar sync service : started. Scheduler ID : {}", syncSchedulerId);

            try {
                LOGGER.debug("Calendar Sync: Calendar sync service initialized successfully");
            } catch (Throwable e) {
                LOGGER.debug("Calendar Sync: Calendar sync service initialization failed");
                throw ErrorCode.get(CalendarErrorCodes.CALENDAR_SYNC_SERVICE_NOT_INITIALIZED);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        if (Objects.nonNull(syncSchedulerId) && Objects.nonNull(schedulerService)) {
            schedulerService.removeTask(syncSchedulerId);
            calendarSyncTask.stopTask();

            LOGGER.debug("Scheduled calendar sync service : stopped. Scheduler ID : {}", syncSchedulerId);
        }
    }

    /**
     * <p>
     * A task which is responsible for calendar syncing activity
     * </p>
     *
     * @author Ajith venkadesh
     * @version 1.0
     */
    private static class CalendarSyncTask extends Task {

        private final CalendarSyncService calendarSyncService;
        private final ClusterService clusterService;

        private CalendarSyncTask(final TaskController controller) {
            super(controller);
            this.calendarSyncService = CalendarSyncService.getInstance();
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
                DistributedLock distributedLock = null;                   //Todo need to change the distributed lock logic.

                try {
                    distributedLock = clusterService.getDistributedLock("calendar-sync");
                    distributedLock.acquire();
                    calendarSyncService.syncCalendarEvents();
                } finally {

                    if (Objects.nonNull(distributedLock)) {
                        distributedLock.release();
                    }
                }
            } catch (Throwable e) {
                LOGGER.debug("Calendar syncing action : failed. Stack : {}", e.getMessage());
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
     * A controller which controls calendar syncing task {@link CalendarSyncTask}.
     *
     * @author Ajith venkadesh
     * @version 1.0
     */
    private static class CalendarSyncTaskController extends TaskController {

        public CalendarSyncTaskController() {
            super(TASK_SCHEDULER_NAME);
        }

        /**
         * {@inheritDoc}
         *
         * @return long of interval time
         */
        @Override
        public long getNextInterval() {
            return 5000;
        }
    }
}