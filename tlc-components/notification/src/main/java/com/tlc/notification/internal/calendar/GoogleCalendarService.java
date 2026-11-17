package com.tlc.notification.internal.calendar;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.Configurations;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.notification.fields.SyncKeys;
import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.model.calendar.CalendarSyncRequest;
import com.tlc.notification.model.contact.Address;
import com.tlc.notification.model.calendar.CalendarEvent;
import com.tlc.notification.service.calendar.CalendarService;
import com.tlc.notification.service.calendar.CalendarTokenService;

import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.service.token.CalendarSyncTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.security.GeneralSecurityException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.HashSet;
import java.util.Optional;

/**
 * <p>
 * Provides the implementation for creating, updating, deleting, retrieving events from calendar.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class GoogleCalendarService implements CalendarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCalendarService.class);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String PRIMARY_CALENDAR_ID = "primary";

    private String clientId;
    private String clientSecret;
    private String applicationName;
    private CalendarTokenService calendarTokenService;
    private CalendarSyncTokenService calendarSyncTokenService;

    private GoogleCalendarService() {

    }

    private static class Instance {
        private static final GoogleCalendarService INSTANCE = new GoogleCalendarService();
    }

    /**
     * <p>
     * Gets the {@link GoogleCalendarService} Instance
     * </p>
     *
     * @return {@link GoogleCalendarService}
     */
    public static GoogleCalendarService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Sets the app properties based on the email configurations.
     * </p>
     *
     * @param emailAppConfigs Represents the {@link Configurations}
     */
    public void setAppProperties(final Configurations emailAppConfigs) {
        final Configurations configurations = emailAppConfigs.getNestedConfigs(com.tlc.notification.fields.NotificationKeys.GMAIL.getKey());

        this.clientId = configurations.getConfig(SyncKeys.APP_CLIENT_ID);
        this.clientSecret = configurations.getConfig(SyncKeys.APP_CLIENT_SECRET);
        this.applicationName = configurations.getConfig(SyncKeys.NAME);
        this.calendarTokenService = CalendarTokenService.getInstance();
        this.calendarSyncTokenService = CalendarSyncTokenService.getInstance();
        LOGGER.info("Gmail provider sync properties has been registered successfully");
    }

    /**
     * <p>
     * Retrieves events from Google Calendar.
     * </p>
     *
     * @param calendarRequest Represents the calendar sync request.
     * @return collection of {@link CalendarEvent}.
     */
    @Override
    public Collection<CalendarEvent> retrieve(final CalendarSyncRequest calendarRequest) {
        final Collection<CalendarEvent> calendarEvents = fetchEvents(
                buildCalendar(calendarRequest.getEmailAddress()), calendarRequest
        );

        return calendarEvents.isEmpty() ? Collections.emptyList() : calendarEvents;
    }

    /**
     * <p>
     * Fetches Google Calendar events based on the sync token or date range.
     * </p>
     *
     * @param calendar        Represents the Google Calendar client.
     * @param calendarRequest Represents the calendar sync request containing email, start, and end time.
     * @return Collection of {@link CalendarEvent}
     */
    private Collection<CalendarEvent> fetchEvents(final Calendar calendar,
                                                  final CalendarSyncRequest calendarRequest) {
        final String emailId = calendarRequest.getEmailAddress().getEmailId();
        final Optional<String> optionalToken = calendarTokenService.getSyncToken(calendarRequest.getOrgId(), emailId);

        final Collection<CalendarEvent> calendarEvents = new HashSet<>();
        final Collection<String> processedEventIds = new HashSet<>();

        String pageToken = null;
        Events events;

        try {
            if (optionalToken.isEmpty()) {
                LOGGER.debug("Calendar Sync: Performing initial sync");

                do {
                    events = calendar.events()
                            .list(GoogleCalendarService.PRIMARY_CALENDAR_ID)
                            .setTimeMin(new DateTime(calendarRequest.getStartTime()))
                            .setTimeMax(new DateTime(calendarRequest.getEndTime()))
                            .setShowDeleted(true)
                            .setSingleEvents(true)
                            .setPageToken(pageToken)
                            .execute();

                    processEventList(events, calendarEvents, processedEventIds);
                    pageToken = events.getNextPageToken();
                } while (Objects.nonNull(pageToken));

            } else {
                LOGGER.debug("Calendar Sync: Performing delta");

                do {
                    events = calendar.events()
                            .list(GoogleCalendarService.PRIMARY_CALENDAR_ID)
                            .setSyncToken(optionalToken.get())
                            .setShowDeleted(true)
                            .setPageToken(pageToken)
                            .execute();

                    processEventList(events, calendarEvents, processedEventIds);
                    pageToken = events.getNextPageToken();
                } while (Objects.nonNull(pageToken));
            }
            final String nextSyncToken = events.getNextSyncToken();

            if (Objects.nonNull(nextSyncToken)) {
                calendarTokenService.putSyncToken(calendarRequest.getOrgId(), emailId, nextSyncToken);
            }

        } catch (IOException exception) {
            LoggerUtil.logError(LOGGER, exception, "Calendar Sync: Calendar events retrieve action failed {}", exception.getMessage());

            throw ErrorCode.get(NotificationErrorCodes.FAILED_TO_RETRIEVE_CALENDAR_EVENT,
                    "i18n.notification.calendar.event.retrieve.action.failed");
        }

        return calendarEvents;
    }

    /**
     * <p>
     * Processes events retrieved from Google and converts them to {@link CalendarEvent}.
     * </p>
     *
     * @param events            Represents the list of events fetched from Google Calendar.
     * @param calendarEvents    Collection to store the converted events.
     * @param processedEventIds Tracks event IDs to avoid duplicates.
     */
    private void processEventList(final Events events,
                                  final Collection<CalendarEvent> calendarEvents,
                                  final Collection<String> processedEventIds) {
        for (final Event event : events.getItems()) {
            final String eventId = event.getId();

            if (processedEventIds.contains(eventId)) {
                continue;
            }
            final CalendarEvent calendarEvent = new CalendarEvent();

            if (!NotificationKeys.CANCELLED.getKey().equalsIgnoreCase(event.getStatus())) {
                buildCalendarEvent(event, calendarEvent);
            } else {
                calendarEvent.setDeleteEvent(true);
            }

            if (Objects.isNull(calendarEvent.getStartTime()) || Objects.isNull(calendarEvent.getEndTime())) {
                calendarEvent.setDeleteEvent(true);
            }
            calendarEvent.setEventId(eventId);
            calendarEvent.setReferenceId(event.getICalUID());
            calendarEvents.add(calendarEvent);
            processedEventIds.add(eventId);
        }
    }

    /**
     * <p>
     * Builds the {@link CalendarEvent} object from Google {@link Event}.
     * </p>
     *
     * @param event         Represents the Google event entity.
     * @param calendarEvent Represents the internal {@link CalendarEvent} entity.
     */
    private void buildCalendarEvent(final Event event, final CalendarEvent calendarEvent) {
        final EventDateTime startTime = event.getStart();
        final EventDateTime endTime = event.getEnd();
        final Collection<EventAttendee> attendees = event.getAttendees();
        final String location = event.getLocation();

        calendarEvent.setSummary(event.getSummary());
        calendarEvent.setDescription(event.getDescription());

        if (Objects.nonNull(location)) {
            final Address address = new Address();

            address.setFullAddress(location);
            calendarEvent.setAddress(address);
        }

        if (Objects.nonNull(startTime)) {

            if (Objects.isNull(startTime.getDateTime())) {
                calendarEvent.setStartTime(new com.tlc.l10n.temporal.model.DateTime(startTime.getDate().getValue()));
            } else {
                calendarEvent.setStartTime(new com.tlc.l10n.temporal.model.DateTime(startTime.getDateTime().getValue()));
            }
        }

        if (Objects.nonNull(endTime)) {

            if (Objects.isNull(endTime.getDateTime())) {
                calendarEvent.setEndTime(new com.tlc.l10n.temporal.model.DateTime(endTime.getDate().getValue()));
            } else {
                calendarEvent.setEndTime(new com.tlc.l10n.temporal.model.DateTime(endTime.getDateTime().getValue()));
            }
        }

        if (Objects.isNull(startTime) || Objects.isNull(endTime)) {
            calendarEvent.setDeleteEvent(true);
        }

        if (Objects.nonNull(attendees) && !attendees.isEmpty()) {
            calendarEvent.setEmailAddresses(
                    attendees.stream()
                            .map(attendee -> new com.tlc.notification.model.email.EmailAddress(attendee.getEmail()))
                            .toList()
            );
        }
    }

    /**
     * <p>
     * Creates new event in the google calendar.
     * </p>
     *
     * @param calendarEvent Represents the event to be added.
     * @param emailAddress  Represents the synced email address.
     */
    @Override
    public void addEvent(final CalendarEvent calendarEvent, final com.tlc.notification.model.email.EmailAddress emailAddress) {

        try {
            final Event addedEvent = buildCalendar(emailAddress).events()
                    .insert(PRIMARY_CALENDAR_ID, buildEvent(calendarEvent)).execute();

            calendarEvent.setReferenceId(addedEvent.getICalUID());
            calendarEvent.setEventId(addedEvent.getId());
        } catch (IOException exception) {
            LoggerUtil.logError(LOGGER, exception, "Calendar event create action : failed. Trace: {}", exception.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_ADD_EVENT,
                    "i18n.notification.calendar.event.add.action.failed");
        }
    }

    /**
     * <p>
     * Updates the event in the google calendar.
     * </p>
     *
     * @param calendarEvent Represents the event to be updated.
     * @param emailAddress  Represents the synced email address.
     * @return id of the updated event.
     */
    @Override
    public String updateEvent(final CalendarEvent calendarEvent, final com.tlc.notification.model.email.EmailAddress emailAddress) {

        try {
            final Event updatedEvent = buildCalendar(emailAddress).events()
                    .update(PRIMARY_CALENDAR_ID, calendarEvent.getEventId(), buildEvent(calendarEvent)).execute();

            return updatedEvent.getId();
        } catch (IOException exception) {
            LoggerUtil.logError(LOGGER, exception, "Calendar event update action : failed , Trace: {}", exception.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_UPDATE_EVENT,
                    "i18n.notification.calendar.event.update.action.failed");
        }
    }

    /**
     * <p>
     * Deletes the event in the google calendar.
     * </p>
     *
     * @param eventId      Represents the id of the event to be deleted.
     * @param emailAddress Represents the synced email address.
     */
    @Override
    public void deleteEvent(final String eventId, final com.tlc.notification.model.email.EmailAddress emailAddress) {

        try {
            buildCalendar(emailAddress).events().delete(PRIMARY_CALENDAR_ID, eventId).execute();
        } catch (IOException exception) {
            LoggerUtil.logError(LOGGER, exception, "Calendar event delete action : failed. Trace: {}", exception.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_DELETE_EVENT,
                    "i18n.notification.calendar.event.delete.action.failed");
        }
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
        try {
            return Objects.nonNull(buildCalendar(emailAddress).events().get(PRIMARY_CALENDAR_ID, eventId).execute());
        } catch (final IOException exception) {
            return false;
        }
    }

    /**
     * <p>
     * Fetches the credential to access the google calendar.
     * </p>
     *
     * @param httpTransport Represents the entity of {@link NetHttpTransport}
     * @param emailAddress  Represents the synced email address.
     * @return {@link Credential}
     */
    private Credential getCredentials(final NetHttpTransport httpTransport, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        final GoogleCredential credential = new GoogleCredential.Builder().setTransport(httpTransport).setJsonFactory(JSON_FACTORY)
                .setClientSecrets(clientId, clientSecret).build();
        final EmailAddress oauthEmailAddress = new EmailAddress(emailAddress.getEmailId());
        final EmailTokenReadRequest accessTokenReadRequest = SyncTokenRequestInstanceProvider.getCalendarTokenReadRequest(oauthEmailAddress, TokenRequest.TokenType.ACCESS);
        final EmailTokenReadRequest refreshTokenReadRequest = SyncTokenRequestInstanceProvider.getCalendarTokenReadRequest(oauthEmailAddress, TokenRequest.TokenType.REFRESH);

        credential.setAccessToken(calendarSyncTokenService.getAccessToken(accessTokenReadRequest));
        credential.setRefreshToken(calendarSyncTokenService.getRefreshToken(refreshTokenReadRequest));

        return credential;
    }

    /**
     * <p>
     * Build the {@link Event} from {@link CalendarEvent}
     * </p>
     *
     * @param calendarEvent Represents the entity of {@link CalendarEvent}
     * @return entity of {@link Event}
     */
    private Event buildEvent(final CalendarEvent calendarEvent) {
        final Collection<com.tlc.notification.model.email.EmailAddress> emailAddresses = calendarEvent.getEmailAddresses();
        final Address address = calendarEvent.getAddress();

        return new Event().setSummary(calendarEvent.getSummary()).setDescription(calendarEvent.getDescription())
                .setStart(new EventDateTime().setDateTime(new DateTime(calendarEvent.getStartTime().getTimeStamp())))
                .setEnd(new EventDateTime().setDateTime(new DateTime(calendarEvent.getEndTime().getTimeStamp())))
                .setReminders(new Event.Reminders().setUseDefault(true))
                .setLocation(Objects.isNull(address) ? null : address.getFullAddress())
                .setAttendees(Objects.nonNull(emailAddresses) ? emailAddresses.stream().map(attendee ->
                        new EventAttendee().setEmail(attendee.getEmailId())).toList() : List.of());
    }

    /**
     * <p>
     * Builds the calendar for creating, updating the events.
     * </p>
     *
     * @param emailAddress Represents the synced email address.
     * @return entity of {@link Calendar}
     */
    private Calendar buildCalendar(final com.tlc.notification.model.email.EmailAddress emailAddress) {
        try {
            final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

            return new Calendar.Builder(
                    httpTransport, JSON_FACTORY, getCredentials(httpTransport, emailAddress))
                    .setApplicationName(applicationName).build();
        } catch (GeneralSecurityException | IOException exception) {
            LoggerUtil.logError(LOGGER, exception, "Calendar get action : failed , Trace: {}", exception.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.UNABLE_TO_GET_CALENDAR);
        }
    }
}