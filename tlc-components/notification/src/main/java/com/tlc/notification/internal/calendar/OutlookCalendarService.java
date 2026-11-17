package com.tlc.notification.internal.calendar;

import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Attendee;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.DateTimeTimeZone;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Location;
import com.microsoft.graph.models.PhysicalAddress;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.EventCollectionRequest;
import com.microsoft.graph.requests.GraphServiceClient;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.LoggerUtil;
import com.tlc.l10n.temporal.TemporalServiceProvider;
import com.tlc.l10n.temporal.model.DateTime;

import com.tlc.notification.internal.status.NotificationErrorCodes;
import com.tlc.notification.key.NotificationKeys;
import com.tlc.notification.model.calendar.CalendarSyncRequest;
import com.tlc.notification.model.calendar.SyncData;
import com.tlc.notification.model.contact.Address;
import com.tlc.notification.model.calendar.CalendarEvent;
import com.tlc.notification.service.calendar.CalendarService;
import com.tlc.notification.service.calendar.CalendarTokenService;

import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.SyncTokenRequestInstanceProvider;
import com.tlc.sync.service.oauth.model.request.TokenRequest;
import com.tlc.sync.service.oauth.model.request.email.EmailTokenReadRequest;
import com.tlc.sync.service.oauth.service.token.CalendarSyncTokenService;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * <p>
 * Provides the implementation for creating, updating, deleting, retrieving events from outlook calendar.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class OutlookCalendarService implements CalendarService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutlookCalendarService.class);

    private static final String DATE_TIME_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSS";
    private static final int FRACTIONAL_SECONDS_COUNT = 7;
    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String REMOVED = "@removed";
    private static final String START_DATE_TIME = "startDateTime";
    private static final String END_DATE_TIME = "endDateTime";

    private final CalendarTokenService calendarTokenService;
    private final CalendarSyncTokenService calendarSyncTokenService;

    private OutlookCalendarService() {
        this.calendarTokenService = CalendarTokenService.getInstance();
        this.calendarSyncTokenService = CalendarSyncTokenService.getInstance();
    }

    private static class Instance {
        private static final OutlookCalendarService INSTANCE = new OutlookCalendarService();
    }

    /**
     * <p>
     * Gets the {@link OutlookCalendarService} Instance
     * </p>
     *
     * @return {@link OutlookCalendarService}
     */
    public static OutlookCalendarService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Retrieves the events from the outlook calendar.
     * </p>
     *
     * @param calendarRequest Represent the calendar sync request
     */
    @Override
    public Collection<CalendarEvent> retrieve(final CalendarSyncRequest calendarRequest) {
        final String emailId = calendarRequest.getEmailId();
        final EmailTokenReadRequest accessTokenReadRequest =
                SyncTokenRequestInstanceProvider.getCalendarTokenReadRequest(new EmailAddress(emailId), TokenRequest.TokenType.ACCESS);
        final String accessToken = calendarSyncTokenService.getAccessToken(accessTokenReadRequest);
        final Optional<String> optionalSyncToken = calendarTokenService.getSyncToken(calendarRequest.getOrgId(), emailId);
        final GraphServiceClient<Request> graphServiceClient = getGraphClient(accessToken);

        try {
            return optionalSyncToken.map(syncToken ->
                            fetchEvents(calendarRequest, accessToken, graphServiceClient, syncToken))
                    .orElseGet(() -> fetchEvents(calendarRequest, accessToken, graphServiceClient));
        } catch (final Throwable throwable) {
            LOGGER.debug("Calendar Sync: Outlook calendar events retrieve action failed for orgId: {}, error: {}",
                    calendarRequest.getOrgId(), throwable.getMessage(), throwable);

            throw ErrorCode.get(NotificationErrorCodes.FAILED_TO_RETRIEVE_CALENDAR_EVENT,
                    "i18n.notification.calendar.event.retrieve.action.failed");
        }
    }

    /**
     * <p>
     * Performs an initial synchronization to fetch all calendar events within the specified time range.
     * This method is executed when no delta token is available for the user.
     * </p>
     *
     * @param calendarRequest    The calendar request containing organization and user details.
     * @param accessToken        The OAuth access token used for authentication.
     * @param graphServiceClient The Microsoft Graph API client.
     * @return A collection of {@link CalendarEvent} objects retrieved during the initial sync.
     */
    private Collection<CalendarEvent> fetchEvents(final CalendarSyncRequest calendarRequest,
                                                  final String accessToken,
                                                  final GraphServiceClient<Request> graphServiceClient) {
        final String emailId = calendarRequest.getEmailId();
        final Map<String, CalendarEvent> processedEvents = new HashMap<>();
        final LinkedList<Option> queryOptions = new LinkedList<>();

        queryOptions.add(new QueryOption(START_DATE_TIME, Instant.ofEpochMilli(calendarRequest.getStartTime())
                .atOffset(ZoneOffset.UTC).toString()));
        queryOptions.add(new QueryOption(END_DATE_TIME, Instant.ofEpochMilli(calendarRequest.getEndTime())
                .atOffset(ZoneOffset.UTC).toString()));

        final String deltaUrl = graphServiceClient.me()
                .calendarView()
                .delta()
                .buildRequest(queryOptions)
                .getRequestUrl()
                .toString();
        final SyncData syncData = fetchAllCalendarEventsWithPagination(deltaUrl, accessToken);
        final JsonArray events = syncData.getEvents();
        final int size = events.size();

        for (int index = 0; index < size; index++) {
            final JsonObject jsonObject = events.getJsonObject(index);
            final String type = jsonObject.optString(TYPE, null);

            if (EventType.isOccurrenceOrException(type)) {
                continue;
            }
            processedEvents.put(jsonObject.getString(ID), buildCalendarEvent(jsonObject));
        }
        final String token = syncData.getDeltaToken();

        if (Objects.nonNull(token)) {
            calendarTokenService.putSyncToken(calendarRequest.getOrgId(), emailId, extractDeltaToken(token));
        }

        return processedEvents.values();
    }

    /**
     * <p>
     * Performs a delta synchronization to fetch only updated, newly created, or deleted events since the last sync.
     * </p>
     *
     * @param calendarRequest    The calendar request containing organization and user details.
     * @param accessToken        The OAuth access token used for authentication.
     * @param graphServiceClient The Microsoft Graph API client.
     * @param syncToken          The delta token from the previous synchronization.
     * @return A collection of {@link CalendarEvent} objects including updates and deletions.
     */
    private Collection<CalendarEvent> fetchEvents(final CalendarSyncRequest calendarRequest,
                                                  final String accessToken,
                                                  final GraphServiceClient<Request> graphServiceClient,
                                                  final String syncToken) {
        final String emailId = calendarRequest.getEmailId();
        final Collection<CalendarEvent> calendarEvents = new HashSet<>();
        final Collection<String> removedEventIds = new HashSet<>();
        final String deltaUrl = graphServiceClient.me()
                .calendarView()
                .delta()
                .buildRequest()
                .deltaToken(syncToken)
                .getRequestUrl()
                .toString();

        final SyncData syncData = fetchAllCalendarEventsWithPagination(deltaUrl, accessToken);
        final JsonArray jsonArray = syncData.getEvents();
        final int size = jsonArray.size();

        for (int index = 0; index < size; index++) {
            final JsonObject jsonObject = jsonArray.getJsonObject(index);

            if (jsonObject.containsKey(REMOVED)) {
                removedEventIds.add(jsonObject.getString(ID));
                continue;
            }
            final String type = jsonObject.optString(TYPE, null);

            if (EventType.isOccurrenceOrException(type)) {
                continue;
            }

            calendarEvents.add(buildCalendarEvent(jsonObject));
        }
        final String token = syncData.getDeltaToken();

        if (Objects.nonNull(token)) {
            calendarTokenService.putSyncToken(calendarRequest.getOrgId(), emailId, extractDeltaToken(token));
        }
        calendarEvents.addAll(handleRemovedEvents(graphServiceClient, removedEventIds));

        return calendarEvents;
    }

    /**
     * <p>
     * Handles events marked as removed during delta synchronization.
     * If the event no longer exists in the provider, it is marked as deleted.
     * </p>
     *
     * @param graphServiceClient The Microsoft Graph API client.
     * @param removedEventIds    A collection of event IDs that were removed.
     * @return A collection of {@link CalendarEvent} objects representing removed events.
     */
    private Collection<CalendarEvent> handleRemovedEvents(final GraphServiceClient<Request> graphServiceClient,
                                                          final Collection<String> removedEventIds) {
        final Collection<CalendarEvent> removedEvents = new HashSet<>();

        if (removedEventIds.isEmpty()) {
            return removedEvents;
        }
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER);

        for (final String eventId : removedEventIds) {
            final Event event = retrieveEventFromProvider(graphServiceClient, eventId);
            final CalendarEvent calendarEvent = new CalendarEvent();

            calendarEvent.setEventId(eventId);

            if (Objects.nonNull(event)) {
                loadCalendarEventDetails(calendarEvent, event, dateTimeFormatter);
            } else {
                calendarEvent.setDeleteEvent(true);
            }
            removedEvents.add(calendarEvent);
        }

        return removedEvents;
    }

    /**
     * <p>
     * Populates a {@link CalendarEvent} instance with details from the provider event.
     * </p>
     *
     * @param calendarEvent     The calendar event to populate.
     * @param event             The provider event from Microsoft Graph.
     * @param dateTimeFormatter The formatter for date-time parsing.
     */
    private void loadCalendarEventDetails(final CalendarEvent calendarEvent,
                                          final Event event,
                                          final DateTimeFormatter dateTimeFormatter) {
        calendarEvent.setSummary(event.subject);
        calendarEvent.setDescription(event.bodyPreview);
        calendarEvent.setReferenceId(event.iCalUId);

        if (Objects.nonNull(event.location) && Objects.nonNull(event.location.address)) {
            final Address address = new Address();

            address.setFullAddress(constructFullAddress(event.location.address));
            calendarEvent.setAddress(address);
        }

        if (Objects.nonNull(event.attendees) && !event.attendees.isEmpty()) {
            final Collection<com.tlc.notification.model.email.EmailAddress> addresses = new HashSet<>();

            for (final Attendee attendee : event.attendees) {
                final com.microsoft.graph.models.EmailAddress emailAddress = attendee.emailAddress;

                if (Objects.nonNull(emailAddress)) {
                    addresses.add(new com.tlc.notification.model.email.EmailAddress(emailAddress.address));
                }
            }
            calendarEvent.setEmailAddresses(addresses);
        }

        if (Objects.nonNull(event.start) && Objects.nonNull(event.start.dateTime)) {
            calendarEvent.setStartTime(new DateTime(event.start.dateTime, dateTimeFormatter));
        }

        if (Objects.nonNull(event.end) && Objects.nonNull(event.end.dateTime)) {
            calendarEvent.setEndTime(new DateTime(event.end.dateTime, dateTimeFormatter));
        }

        if (Objects.isNull(calendarEvent.getStartTime()) && Objects.isNull(calendarEvent.getEndTime())) {
            calendarEvent.setDeleteEvent(true);
        }
        calendarEvent.setEventId(calendarEvent.getEventId());
    }

    /**
     * <p>
     * Creates new event in the Outlook calendar.
     * </p
     *
     * @param calendarEvent Represents the event to be added.
     * @param emailAddress  Represents the synced email address.
     */
    @Override
    public void addEvent(final CalendarEvent calendarEvent, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        final EmailTokenReadRequest accessTokenReadRequest = SyncTokenRequestInstanceProvider.getCalendarTokenReadRequest(new EmailAddress(emailAddress.getEmailId()), TokenRequest.TokenType.ACCESS);
        final EventCollectionRequest request = getGraphClient(calendarSyncTokenService.getAccessToken(accessTokenReadRequest)).me().events().buildRequest();
        final Event event = request.post(buildEvent(calendarEvent));

        calendarEvent.setEventId(event.id);
        calendarEvent.setReferenceId(event.iCalUId);
    }

    /**
     * <p>
     * Updates the event in the outlook calendar.
     * </p>
     *
     * @param calendarEvent Represents the event to be updated.
     * @param emailAddress  Represents the synced email address.
     * @return id of the updated event.
     */
    @Override
    public String updateEvent(final CalendarEvent calendarEvent, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        final EmailTokenReadRequest accessTokenReadRequest = SyncTokenRequestInstanceProvider.getCalendarTokenReadRequest(new EmailAddress(emailAddress.getEmailId()), TokenRequest.TokenType.ACCESS);
        final GraphServiceClient<Request> graphClient = getGraphClient(calendarSyncTokenService.getAccessToken(accessTokenReadRequest));

        return graphClient.me().events(calendarEvent.getEventId()).buildRequest().patch(buildEvent(calendarEvent)).id;
    }

    /**
     * <p>
     * Deletes the event in the outlook calendar.
     * </p>
     *
     * @param eventId      Represents the id of the event to be deleted.
     * @param emailAddress Represents the synced email address.
     */
    @Override
    public void deleteEvent(final String eventId, final com.tlc.notification.model.email.EmailAddress emailAddress) {
        final EmailTokenReadRequest accessTokenReadRequest = SyncTokenRequestInstanceProvider.getCalendarTokenReadRequest(new EmailAddress(emailAddress.getEmailId()), TokenRequest.TokenType.ACCESS);
        final GraphServiceClient<Request> graphClient = getGraphClient(calendarSyncTokenService.getAccessToken(accessTokenReadRequest));

        try {
            graphClient.me().events(eventId).buildRequest().delete();
        } catch (GraphServiceException exception) {
            LOGGER.debug("Event delete action : failed , Trace: {}", exception.getMessage());
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
        final EmailTokenReadRequest accessTokenReadRequest =
                SyncTokenRequestInstanceProvider.getCalendarTokenReadRequest(
                        new EmailAddress(emailAddress.getEmailId()), TokenRequest.TokenType.ACCESS);
        final GraphServiceClient<Request> graphClient =
                getGraphClient(calendarSyncTokenService.getAccessToken(accessTokenReadRequest));

        try {
            return Objects.nonNull(graphClient.me().events(eventId).buildRequest().get());
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * <p>
     * Builds the graph client using access token.
     * </p>
     *
     * @param accessToken Represents the access token.
     * @return {@link GraphServiceClient}
     */
    private GraphServiceClient<Request> getGraphClient(final String accessToken) {
        return GraphServiceClient.builder().authenticationProvider((token) -> CompletableFuture.completedFuture(accessToken))
                .buildClient();
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
        final DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        final Event event = new Event();
        final String utcKey = NotificationKeys.UTC.getKey();

        event.subject = calendarEvent.getSummary();
        event.start = new DateTimeTimeZone();
        event.start.dateTime = Instant.ofEpochMilli(calendarEvent.getStartTime().getTimeStamp()).atZone(ZoneId.of(utcKey)).format(formatter);
        event.start.timeZone = utcKey;
        event.end = new DateTimeTimeZone();
        event.end.dateTime = Instant.ofEpochMilli(calendarEvent.getEndTime().getTimeStamp()).atZone(ZoneId.of(utcKey)).format(formatter);
        event.end.timeZone = utcKey;
        event.body = new ItemBody();
        event.body.contentType = BodyType.TEXT;
        event.body.content = calendarEvent.getDescription();
        final List<Attendee> attendees = new ArrayList<>();
        final Collection<com.tlc.notification.model.email.EmailAddress> emailAddresses = calendarEvent.getEmailAddresses();

        if (Objects.nonNull(emailAddresses) && !emailAddresses.isEmpty()) {

            for (final com.tlc.notification.model.email.EmailAddress emailAddress : emailAddresses) {
                final Attendee attendee = new Attendee();

                attendee.emailAddress = new com.microsoft.graph.models.EmailAddress();
                attendee.emailAddress.address = emailAddress.getEmailId();
                attendees.add(attendee);
            }
            event.attendees = attendees;
        }
        final Address address = calendarEvent.getAddress();

        if (Objects.nonNull(address)) {
            event.location = new Location();
            event.location.address = convertPhysicalAddress(address);
            event.location.displayName = address.getFullAddress();
        }

        return event;
    }

    /**
     * <p>
     * Retrieves json values from the specified URI through an HTTP GET request.
     * </p>
     *
     * @param uri         Represents the URI for the request.
     * @param accessToken Represents the access token.
     * @return {@link JsonObject}
     */
    private JsonObject fetchCalendarEventsWithoutDeltaToken(final String uri, final String accessToken) {
        try {
            final Map<String, String> headers = new HashMap<>();

            headers.put(NotificationKeys.AUTHORIZATION.getKey(), String.format("%s%s", NotificationKeys.BEARER.getKey(), accessToken));
            headers.put(NotificationKeys.ACCEPT.getKey(), NotificationKeys.APPLICATION_JSON.getKey());
            headers.put(NotificationKeys.CONTENT_TYPE.getKey(), NotificationKeys.APPLICATION_JSON.getKey());

            return HttpUtil.makeRequest(new HttpRequest(new URL(uri), Collections.emptyMap(), HttpMethod.GET, headers));
        } catch (Exception exception) {
            LOGGER.debug("Events retrieve action : failed , Trace: {}", exception.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.INVALID_URL, "i18n.notification.calendar.invalid.url");
        }
    }

    /**
     * <p>
     * Gets the output for required request
     * </p>
     *
     * @param initialUrl  Represents the URI for the request.
     * @param accessToken Represents the access token.
     * @return {@link Map}, where the key is the delta link, and the value is a json array of values.
     */
    private SyncData fetchAllCalendarEventsWithPagination(final String initialUrl, final String accessToken) {
        final Map<String, String> headers = new HashMap<>();

        headers.put(NotificationKeys.AUTHORIZATION.getKey(), String.format("%s%s", NotificationKeys.BEARER.getKey(), accessToken));
        headers.put(NotificationKeys.ACCEPT.getKey(), NotificationKeys.APPLICATION_JSON.getKey());
        headers.put(NotificationKeys.CONTENT_TYPE.getKey(), NotificationKeys.APPLICATION_JSON.getKey());
        final JsonArray data = Json.array();

        String deltaToken = null;
        String nextLink = initialUrl;

        try {
            while (Objects.nonNull(nextLink)) {
                final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(nextLink), Collections.emptyMap(), HttpMethod.GET, headers));
                final JsonArray currentPageEvents = response.optJsonArray(NotificationKeys.VALUE.getKey());

                if (Objects.nonNull(currentPageEvents)) {

                    for (int index = 0; index < currentPageEvents.size(); index++) {
                        data.put(currentPageEvents.getJsonObject(index));
                    }
                }
                nextLink = response.optString(NotificationKeys.NEXT_LINK.getKey(), null);
                deltaToken = response.optString(NotificationKeys.DELTA_LINK.getKey(), null);
            }

            return new SyncData(data, deltaToken);
        } catch (Exception exception) {
            LOGGER.debug("Calendar Delta Sync: failed to retrieve events, Trace: {}", exception.getMessage());

            throw ErrorCode.get(NotificationErrorCodes.INVALID_URL, "i18n.notification.calendar.invalid.url");
        }
    }

    /**
     * <p>
     * Extracts the token from the given url
     * </p>
     *
     * @param url Represents the url
     * @return the delta token
     */
    private String extractDeltaToken(final String url) {
        try {
            final Map<String, String> queryParams = new HashMap<>();
            final String query = new URL(url).getQuery();

            if (Objects.nonNull(query)) {
                final String[] pairs = query.split("&");

                for (final String pair : pairs) {
                    final String[] keyValue = pair.split("=");

                    if (2 == keyValue.length) {
                        queryParams.put(keyValue[0], keyValue[1]);
                    }
                }
            }
            final String deltaToken = queryParams.get(NotificationKeys.DELTA_TOKEN.getKey());

            return Objects.isNull(deltaToken) ? null : deltaToken;
        } catch (Exception exception) {
            LOGGER.debug("Delta token fetch action : failed , Trace: {}", exception.getMessage());
            throw ErrorCode.get(NotificationErrorCodes.INVALID_URL,
                    "i18n.notification.calendar.invalid.url");
        }
    }

    /**
     * Retrieves the event with the specified id from the provider.
     *
     * @param graphServiceClient Represents the entity of {@link GraphServiceClient}.
     * @param eventId            The id of the event to be retrieved.
     * @return the retrieved event or null if the event is not found.
     */
    private Event retrieveEventFromProvider(final GraphServiceClient graphServiceClient, final String eventId) {

        try {
            return graphServiceClient.me().events(eventId).buildRequest().get();
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Builds the {@link CalendarEvent} from a json object representing an event.
     *
     * @param jsonObject Represents the entity of {@link JsonObject}.
     * @return entity of {@link CalendarEvent}.
     */
    private CalendarEvent buildCalendarEvent(final JsonObject jsonObject) {
        final CalendarEvent calendarEvent = new CalendarEvent();
        final String dateTimeKey = NotificationKeys.DATE_TIME.getKey();
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMATTER);
        final String startTime = jsonObject.getJsonObject(NotificationKeys.START.getKey()).getString(dateTimeKey);
        final String endTime = jsonObject.getJsonObject(NotificationKeys.END.getKey()).getString(dateTimeKey);
        final String lastModifiedTime = jsonObject.getString(NotificationKeys.LAST_MODIFIED_DATE_TIME.getKey());
        final String location = jsonObject.getJsonObject(NotificationKeys.LOCATION.getKey()).getString(NotificationKeys.DISPLAY_NAME.getKey());
        final JsonArray attendees = jsonObject.getJsonArray(NotificationKeys.ATTENDEES.getKey());
        final String emailAddressKey = NotificationKeys.EMAIL_ADDRESS.getKey();
        final String addressKey = NotificationKeys.ADDRESS.getKey();

        calendarEvent.setSummary(jsonObject.optString(NotificationKeys.SUBJECT.getKey(), null));
        calendarEvent.setDescription(jsonObject.optString(NotificationKeys.BODY_PREVIEW.getKey(), null));
        calendarEvent.setStartTime(getDateTime(startTime, dateTimeFormatter));
        calendarEvent.setEndTime(getDateTime(endTime, dateTimeFormatter));
        calendarEvent.setLastModifiedDateTime(getDateTime(lastModifiedTime, dateTimeFormatter));
        calendarEvent.setEventId(jsonObject.getString(NotificationKeys.ID.getKey()));
        calendarEvent.setReferenceId(jsonObject.getString(NotificationKeys.CALENDAR_UNIQUE_ID.getKey()));

        if (Objects.isNull(calendarEvent.getStartTime()) && Objects.isNull(calendarEvent.getEndTime())) {
            calendarEvent.setDeleteEvent(true);
        }
        final Address address = new Address();

        address.setFullAddress(location);
        calendarEvent.setAddress(address);

        final Collection<com.tlc.notification.model.email.EmailAddress> emailAddresses = new HashSet<>();

        for (int index = 0; index < attendees.size(); index++) {
            final JsonObject attendee = attendees.getJsonObject(index);

            emailAddresses.add(new com.tlc.notification.model.email.EmailAddress(attendee.getJsonObject(emailAddressKey).getString(addressKey)));
        }
        calendarEvent.setEmailAddresses(emailAddresses);

        return calendarEvent;
    }

    /**
     * Recursively extracts and processes calendar events from the specified URL using the provided access token.
     *
     * @param uri         Represents the request url.
     * @param accessToken Represents the access token
     * @param syncedTime  Represents the sync start time.
     * @param events      Represents the collection of event ids.
     */
    private void handleInitialSyncRequest(final String uri, final String accessToken, final long syncedTime,
                                          final Map<String, CalendarEvent> events) {
        final JsonObject jsonObject = fetchCalendarEventsWithoutDeltaToken(uri, accessToken);

        if (Objects.nonNull(jsonObject)) {
            final String valueKey = NotificationKeys.VALUE.getKey();
            final String nextLinkKey = NotificationKeys.NEXT_LINK.getKey();

            if (jsonObject.containsKey(valueKey)) {
                final JsonArray jsonArray = jsonObject.getJsonArray(valueKey);

                for (int index = 0; index < jsonArray.size(); index++) {
                    final JsonObject object = jsonArray.getJsonObject(index);
                    final CalendarEvent calendarEvent = buildCalendarEvent(object);
                    final String eventId = calendarEvent.getEventId();

                    if (Objects.isNull(events.get(eventId))) {

                        if ((calendarEvent.getStartTime().getTimeStamp() >= syncedTime) ||
                                (calendarEvent.getLastModifiedDateTime().getTimeStamp() >= syncedTime)) {
                            events.put(eventId, calendarEvent);
                        }
                    }
                }
            }

            if (jsonObject.containsKey(nextLinkKey)) {
                handleInitialSyncRequest(jsonObject.getString(nextLinkKey), accessToken, syncedTime, events);
            }
        }
    }

    /**
     * <p>
     * Constructs the full address from the {@link PhysicalAddress}.
     * </p>
     *
     * @param address Represents the physical address of calendar
     * @return the full address
     */
    private static String constructFullAddress(final PhysicalAddress address) {
        final StringBuilder fullAddressBuilder = new StringBuilder();

        appendAddressBuilder(fullAddressBuilder, address.street);
        appendAddressBuilder(fullAddressBuilder, address.city);
        appendAddressBuilder(fullAddressBuilder, address.state);
        appendAddressBuilder(fullAddressBuilder, address.postalCode);
        appendAddressBuilder(fullAddressBuilder, address.countryOrRegion);

        return fullAddressBuilder.toString();
    }

    /**
     * <p>
     * Appends the specified value to the address {@link StringBuilder}, if it is not empty, a comma and space are appended before the value.
     * </p>
     *
     * @param builder Represents the {@link StringBuilder} to which the value should be appended.
     * @param value   Represents the value of address
     */
    private static void appendAddressBuilder(final StringBuilder builder, final String value) {
        if (Objects.nonNull(value) && !value.isEmpty()) {

            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(value);
        }
    }

    /**
     * <p>
     * Converts an {@link Address} to the {@link PhysicalAddress}.
     * </p>
     *
     * @param address Represents the {@link Address}
     * @return {@link PhysicalAddress}
     */
    private PhysicalAddress convertPhysicalAddress(final Address address) {
        final PhysicalAddress physicalAddress = new PhysicalAddress();

        physicalAddress.city = address.getCity();
        physicalAddress.countryOrRegion = address.getCountry();
        physicalAddress.postalCode = address.getZipcode();
        physicalAddress.state = address.getState();
        physicalAddress.street = address.getAddressLine1();

        return physicalAddress;
    }

    /**
     * <p>
     * Gets the {@link DateTime} for based on timestamp.
     * </p>
     *
     * @param timeStamp         Represents the timestamp
     * @param dateTimeFormatter Represents the {@link DateTimeFormatter}
     * @return {@link DateTime}
     */
    private DateTime getDateTime(final String timeStamp, final DateTimeFormatter dateTimeFormatter) {
        final String timestamp = TemporalServiceProvider.get().formatTimestamp(timeStamp,
                true, FRACTIONAL_SECONDS_COUNT);
        final LocalDateTime localDateTime = LocalDateTime.parse(timestamp, dateTimeFormatter);
        final String dateTime = localDateTime.format(dateTimeFormatter);

        return new DateTime(dateTime, dateTimeFormatter);
    }

    /**
     * <p>
     * Defines all supported Outlook calendar event types and provides a utility method
     * to identify event types that should be skipped during synchronization.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    private enum EventType {

        /**
         * Represents a standalone one-time event.
         */
        SINGLE_INSTANCE("singleInstance"),

        /**
         * Represents an occurrence in a recurring event series.
         */
        OCCURRENCE("occurrence"),

        /**
         * Represents an exception (a modified occurrence) in a recurring series.
         */
        EXCEPTION("exception"),

        /**
         * Represents the master event that defines a recurring event series.
         */
        SERIES_MASTER("seriesMaster");

        private final String type;

        EventType(final String type) {
            this.type = type;
        }

        /**
         * <p>
         * Occurrence and Exception types are typically ignored to prevent duplicate
         * or redundant processing.
         * </p>
         *
         * @param type The event type to check.
         * @return {@code true} if the event type should be skipped; {@code false} otherwise.
         */
        public static boolean isOccurrenceOrException(final String type) {
            if (Objects.isNull(type)) {
                return false;
            }

            return OCCURRENCE.type.equalsIgnoreCase(type) || EXCEPTION.type.equalsIgnoreCase(type);
        }
    }
}
