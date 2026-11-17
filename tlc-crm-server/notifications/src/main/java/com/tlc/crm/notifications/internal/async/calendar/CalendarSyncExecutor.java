package com.tlc.crm.notifications.internal.async.calendar;

import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.notification.app.Event;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.crm.entity.async.cache.SyncInfoHolderService;

import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.crm.notifications.calendar.api.manager.CalendarSyncManager;
import com.tlc.crm.notifications.calendar.api.model.CalendarSyncSettings;
import com.tlc.crm.notifications.calendar.internal.CalendarSyncService;
import com.tlc.crm.notifications.internal.async.SyncExecutorService;
import com.tlc.crm.notifications.internal.async.calendar.cache.CalendarSyncInfoHolder;
import com.tlc.crm.notifications.internal.async.calendar.cache.CalendarSyncInfoHolderService;

import com.tlc.scheduler.service.provider.SchedulerServiceProvider;
import com.tlc.scheduler.service.SchedulerService;
import com.tlc.scheduler.task.Task;
import com.tlc.scheduler.task.TaskController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.tlc.crm.notification.app.event.NotificationEvent.APP_NOTIFICATION_EVENT;

/**
 * <p>
 * Provides the functionality for initiating and monitoring calendar synchronization tasks.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see SyncExecutorService
 */
public class CalendarSyncExecutor implements SyncExecutorService<CalendarSyncSettings> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarSyncExecutor.class);
    private static final String CALENDAR_SYNC_THREAD_POOL = "calendar-sync-thread-pool";

    private final SyncInfoHolderService calendarSyncInfoHolderService;
    private final CalendarSyncInfoHolder calendarSyncInfoHolder;
    private final SchedulerService schedulerService;
    private final CalendarSyncManager calendarSyncManager;
    private final CalendarSyncService calendarSyncService;
    private final EventDataHandler eventDataHandler;

    public CalendarSyncExecutor() {
        this.calendarSyncInfoHolderService = CalendarSyncInfoHolderService.getInstance();
        this.calendarSyncInfoHolder = CalendarSyncInfoHolder.getInstance();
        this.schedulerService = SchedulerServiceProvider.get();
        this.calendarSyncManager = CalendarSyncManager.getInstance();
        this.calendarSyncService = CalendarSyncService.getInstance();
        this.eventDataHandler = EventDataHandler.getInstance();
    }

    private static class Instance {
        private static final CalendarSyncExecutor INSTANCE = new CalendarSyncExecutor();
    }

    /**
     * <p>
     * Gets the instance of the {@link CalendarSyncExecutor}
     * </p>
     *
     * @return {@link CalendarSyncExecutor}
     */
    public static CalendarSyncExecutor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncSettings Represents the {@link CalendarSyncSettings}
     * @return {@link SyncStatus}
     */
    @Override
    public void execute(final CalendarSyncSettings syncSettings) {

        sync(() -> {
            final long orgId = syncSettings.orgId();
            final long userId = syncSettings.getUserId();
            final Long syncId = syncSettings.id();

            calendarSyncInfoHolderService.setSyncStatus(orgId, userId, new SyncInfo(syncId, SyncStatus.IN_PROGRESS,
                    syncSettings.getCalendarSyncConfig().getConfigJsonAsString()));

            try {
                calendarSyncService.importActivityEvents(orgId, userId, syncSettings);
                calendarSyncManager.loadCalendarSyncSettingsIntoContainer(syncSettings);
                calendarSyncInfoHolderService.setSyncStatus(orgId, userId, new SyncInfo(syncId,
                        syncSettings.getLastSyncTime(), SyncStatus.SYNCED, null));
                final EventData eventData =  new SecureEventData(orgId, userId);

                eventData.setValues(List.of(syncSettings));
                eventData.setAction(Action.CREATE);

                eventDataHandler.handleEvent(APP_NOTIFICATION_EVENT, eventData, Event.Type.POST);
                LOGGER.debug("Calendar sync: Synchronization process has been successfully completed for Org ID: {}, User ID: {}, Sync ID: {}", orgId, userId, syncId);
            } catch (Throwable throwable) {
                LOGGER.debug("Calendar sync: Synchronization failed for Org ID: {}, User ID: {}, Sync ID: {}, Reason: {}",
                        orgId, userId, syncId, throwable.getMessage());
                calendarSyncInfoHolderService.setSyncStatus(orgId, userId, new SyncInfo(syncId, SyncStatus.FAILED));
            }
        });
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the synced calendar id
     * @return {@link SyncInfo}
     */
    @Override
    public SyncInfo getSyncInfo(final long orgId, final long syncId) {
        return calendarSyncInfoHolderService.get(orgId, syncId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return {@link SyncStatus}
     */
    @Override
    public SyncStatus getSyncStatus(final long orgId, final long syncId) {
        return getSyncInfo(orgId, syncId).syncStatus();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncId Represents the sync id
     */
    @Override
    public void remove(final long syncId) {
        calendarSyncInfoHolder.remove(syncId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void clear() {
        calendarSyncInfoHolder.clear();
    }

    /**
     * <p>
     * Asynchronously a task to the executor for processing the calendar sync.
     * </p>
     *
     * @param task Represents the {@link RunnableTask}
     */
    private void sync(final RunnableTask task) {
        final CalendarSyncTask calendarSyncTask = new CalendarSyncTask(new CalendarSyncTask.CalendarSyncTaskController(), task);

        schedulerService.schedule(CALENDAR_SYNC_THREAD_POOL, calendarSyncTask);
    }

    /**
     * <p>
     * A task which is responsible to do the calendar sync action.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     * @see Task
     */
    private static final class CalendarSyncTask extends Task {

        private final RunnableTask task;

        private CalendarSyncTask(final TaskController controller, final RunnableTask task) {
            super(controller, 0);
            this.task = task;
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         *
         * @param taskController Represents the {@link TaskController}
         */
        @Override
        public void execute(final TaskController taskController) {
            task.run();
        }

        /**
         * <p>
         * Provides the specialized controller for managing the execution of calendar synchronization task.
         * </p>
         *
         * @author KowsalyaSP
         * @version 1.0
         * @see TaskController
         */
        private static class CalendarSyncTaskController extends TaskController {

            private static final String CALENDAR_SYNC_TASK_NAME = "calendar-sync-processor";

            private CalendarSyncTaskController() {
                super(CALENDAR_SYNC_TASK_NAME);
            }

            /**
             * <p>
             * {@inheritDoc}
             * </p>
             *
             * @return the next interval
             */
            @Override
            public long getNextInterval() {
                return 0;
            }
        }
    }
}