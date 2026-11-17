package com.tlc.notification.service.calendar;

import com.tlc.notification.internal.calendar.CalendarServiceImpl;
import com.tlc.notification.model.calendar.CalendarEvent;
import com.tlc.notification.model.calendar.CalendarSyncRequest;
import com.tlc.notification.model.email.EmailAddress;

import java.util.Collection;

/**
 * <p>
 * Provides the services for creating, updating, deleting, retrieving events from calendar.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public interface CalendarService {

    /**
     * <p>
     * Gets the instance for {@link CalendarService}.
     * </p>
     *
     * @return {@link CalendarService}
     */
    static CalendarService getInstance() {
        return CalendarServiceImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the events from the calendar provider.
     * </p>
     *
     * @param calendarRequest Represent the calendar sync request
     * @return collection of {@link CalendarEvent}.
     */
    Collection<CalendarEvent> retrieve(final CalendarSyncRequest calendarRequest);

    /**
     * <p>
     * Creates new event in the calendar provider.
     * </p>
     *
     * @param calendarEvent Represents the event to be added.
     * @param emailAddress  Represents the synced email address.
     */
    void addEvent(final CalendarEvent calendarEvent, final EmailAddress emailAddress);

    /**
     * <p>
     * Updates the event in the calendar provider.
     * </p>
     *
     * @param calendarEvent Represents the event to be updated.
     * @param emailAddress  Represents the synced email address.
     * @return id of the updated event.
     */
    String updateEvent(final CalendarEvent calendarEvent, final EmailAddress emailAddress);

    /**
     * <p>
     * Deletes the event in the calendar provider.
     * </p>
     *
     * @param eventId      Represents the id of the event to be deleted.
     * @param emailAddress Represents the synced email address.
     */
    void deleteEvent(final String eventId, final EmailAddress emailAddress);

    /**
     * <p>
     * Checks whether a calendar event with the given event ID exists for the specified user.
     * </p>
     *
     * @param eventId      The unique identifier of the event to check.
     * @param emailAddress The email address of the user whose calendar is being checked.
     * @return {@code true} if the event exists, otherwise {@code false}.
     */
    boolean eventExists(final String eventId, final EmailAddress emailAddress);
}