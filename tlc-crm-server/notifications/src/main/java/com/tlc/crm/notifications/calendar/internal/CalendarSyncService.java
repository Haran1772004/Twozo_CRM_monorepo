package com.tlc.crm.notifications.calendar.internal;

import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.crm.notifications.calendar.api.model.CalendarSyncSettings;
import com.tlc.crm.notifications.calendar.internal.impl.CalendarSyncServiceImpl;

/**
 * <p>
 * Provides the service for calendar sync
 * </p>
 *
 * @author Ajith venkadesh, Kowsalya SP
 * @version 1.0
 */
public interface CalendarSyncService {

    /**
     * <p>
     * Fetches the {@link CalendarSyncService} instance
     * </p>
     *
     * @return the instance of {@link CalendarSyncService}
     */
    static CalendarSyncService getInstance() {
        return CalendarSyncServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the calendar events automatically from the given accounts.
     * </p>
     */
    void syncCalendarEvents();

    /**
     * <p>
     * Updates the calendar {@link SyncStatus} for an specified organization and user.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    void updateCalendarSyncStatus(final long orgId, final long userId, final SyncInfo syncInfo);

    /**
     * <p>
     * Gets the calendar {@link SyncInfo} for an respective organization and sync id.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the calendar sync id
     * @return {@link SyncInfo}
     */
    SyncInfo getCalendarSyncInfo(final long orgId, final long syncId);

    /**
     * <p>
     * Imports the activity events based on {@link CalendarSyncSettings}.
     * </p>
     *
     * @param orgId                Represents an organization id
     * @param userId               Represents the user id
     * @param calendarSyncSettings Represents the {@link CalendarSyncSettings}
     */
    void importActivityEvents(final long orgId, final long userId, final CalendarSyncSettings calendarSyncSettings);
}