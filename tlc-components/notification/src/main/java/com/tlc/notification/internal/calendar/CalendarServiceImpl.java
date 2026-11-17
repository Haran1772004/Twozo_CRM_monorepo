package com.tlc.notification.internal.calendar;

import com.tlc.notification.model.calendar.CalendarEvent;
import com.tlc.notification.model.calendar.CalendarSyncRequest;
import com.tlc.notification.service.calendar.CalendarService;
import com.tlc.notification.service.contact.ContactService;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.provider.CalendarProvider;
import com.tlc.sync.service.oauth.service.provider.ProviderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * <p>
 * Provides the calendar service.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class CalendarServiceImpl implements CalendarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CalendarServiceImpl.class);

    private final ProviderService providerService;
    private final GoogleCalendarService googleCalendarService;
    private final OutlookCalendarService outlookCalendarService;

    private CalendarServiceImpl() {
        providerService = ProviderService.getCalendarProviderService();
        googleCalendarService = GoogleCalendarService.getInstance();
        outlookCalendarService = OutlookCalendarService.getInstance();
    }

    private static class Instance {
        private static final CalendarServiceImpl INSTANCE = new CalendarServiceImpl();
    }

    /**
     * <p>
     * Gets the {@link CalendarServiceImpl} Instance
     * </p>
     *
     * @return {@link CalendarServiceImpl}
     */
    public static CalendarServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param calendarSyncRequest Represents the calendar sync request.
     * @return
     */
    @Override
    public Collection<CalendarEvent> retrieve(final CalendarSyncRequest calendarSyncRequest) {
        return getService(calendarSyncRequest.getEmailAddress()).retrieve(calendarSyncRequest);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param calendarEvent Represents the event to be added.
     * @param emailAddress  Represents the synced email address.
     */
    @Override
    public void addEvent(final CalendarEvent calendarEvent, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        getService(emailAddress).addEvent(calendarEvent, emailAddress);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param calendarEvent Represents the event to be updated.
     * @param emailAddress  Represents the synced email address.
     * @return
     */
    @Override
    public String updateEvent(final CalendarEvent calendarEvent, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        return getService(emailAddress).updateEvent(calendarEvent, emailAddress);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param eventId      Represents the id of the event to be deleted.
     * @param emailAddress Represents the synced email address.
     */
    @Override
    public void deleteEvent(final String eventId, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        getService(emailAddress).deleteEvent(eventId, emailAddress);
    }

    /**
     * <p>
     * Checks whether a calendar event with the given event ID exists for the specified user.
     * </p>
     *
     * @param eventId      The unique identifier of the event to check.
     * @param emailAddress The email address of the user whose calendar is being checked.
     * @return {@code true} if the event exists, otherwise {@code false}.
     */
    @Override
    public boolean eventExists(final String eventId, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        return getService(emailAddress).eventExists(eventId, emailAddress);
    }

    /**
     * <p>
     * Gets the appropriate contact service to be done.
     * </p>
     *
     * @param emailAddress Represents the {@link ContactService}
     * @return {@link ContactService} relevant to the provided contact address
     */
    private CalendarService getService(final com.tlc.notification.model.email.EmailAddress emailAddress) {
        final CalendarProvider calendarProvider = providerService.findProvider(new EmailAddress(emailAddress.getEmailId()));

        return switch (calendarProvider) {
            case GOOGLE -> googleCalendarService;
            case OUTLOOK -> outlookCalendarService;
        };
    }
}
