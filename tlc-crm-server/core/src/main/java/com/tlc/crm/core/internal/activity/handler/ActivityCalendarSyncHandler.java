package com.tlc.crm.core.internal.activity.handler;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.core.features.api.activity.model.converter.calendar.CalendarEventMapper;
import com.tlc.crm.entity.features.api.activity.manager.ActivityManager;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.entity.sql.resource.OMCALEXTERNALEVENTS;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.notification.model.calendar.CalendarEvent;
import com.tlc.notification.model.email.EmailAddress;
import com.tlc.notification.service.calendar.CalendarService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles creation, update, and deletion of calendar events
 * corresponding to CRM activities based on user’s synced calendar settings.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.1
 */
public class ActivityCalendarSyncHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivityCalendarSyncHandler.class);

    private final ActivityManager activityManager;
    private final ActivityTypeManager activityTypeManager;
    private final CalendarService calendarService;
    private final CalendarEventMapper calendarEventMapper;

    /**
     * <p>
     * Initializes a new instance of {@link ActivityCalendarSyncHandler}.
     * </p>
     */
    private ActivityCalendarSyncHandler() {
        this.activityManager = ActivityManager.getInstance();
        this.activityTypeManager = ActivityTypeManager.getInstance();
        this.calendarService = CalendarService.getInstance();
        this.calendarEventMapper = CalendarEventMapper.getInstance();
    }

    /**
     * <p>
     * Singleton holder for lazy initialization.
     * </p>
     */
    private static class Instance {
        private static final ActivityCalendarSyncHandler INSTANCE = new ActivityCalendarSyncHandler();
    }

    /**
     * <p>
     * Provides the singleton instance of {@link ActivityCalendarSyncHandler}.
     * </p>
     *
     * @return The singleton handler instance.
     */
    public static ActivityCalendarSyncHandler getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Gets the orgDataStore
     *
     * @param orgId Represents the organization
     * @return {@link OrgDataStore}
     */
    private OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Handles synchronization of calendar events based on the specified {@link Action}.
     * </p>
     *
     * @param orgId           The organization ID.
     * @param action          The performed action (CREATE, UPDATE, DELETE).
     * @param activityHolders The collection of {@link ActivityHolder} instances to process.
     */
    public void handle(final long orgId, final Action action, final Collection<ActivityHolder> activityHolders) {
        try {
            if (activityHolders.isEmpty()) {
                return;
            }
            final ActivityHolder firstHolder = activityHolders.iterator().next();
            final Long creatorId = firstHolder.getActivity().getCreatorId();
            final String syncedEmail = activityManager.getSyncedCalendarEmailAddress(orgId, creatorId);

            if (Objects.isNull(syncedEmail)) {
                return;
            }
            final EmailAddress emailAddress = new EmailAddress(syncedEmail);
            final String calendarConfig = activityManager.getCalendarSyncConfig(orgId, creatorId, emailAddress);

            if (Objects.isNull(calendarConfig)) {
                return;
            }
            final JsonObject configJson = Json.object(calendarConfig);
            final Collection<Long> configuredTypeIds = configJson.getJsonArray(ActivityFields.ACTIVITY_TYPES.getName())
                    .toList().stream()
                    .map(typeId -> ((Integer) typeId).longValue())
                    .collect(Collectors.toUnmodifiableSet());

            final boolean isLinkedWithSource = ActivityFields.LINKED.getName().equalsIgnoreCase(
                    configJson.getString(ActivityFields.SOURCE_LINK_STATUS.getName()));

            final Collection<Long> validTypeIds = activityTypeManager.get(orgId, configuredTypeIds)
                    .stream()
                    .map(ActivityType::id)
                    .collect(Collectors.toUnmodifiableSet());
            final Table calendarEventTable = Table.get(OMCALEXTERNALEVENTS.TABLE);
            final List<Row> calendarEventRows = new ArrayList<>(activityHolders.size());
            final boolean isCreateEvent = Action.CREATE == action;
            final Long syncEmailId = activityManager.getCalendarSyncId(orgId, creatorId, true);

            for (final ActivityHolder activityHolder : activityHolders) {
                final Activity activity = activityHolder.getActivity();
                final Long typeId = activity.getType().id();

                if (!validTypeIds.contains(typeId) && action != Action.UPDATE) {
                    continue;
                }

                switch (action) {
                    case CREATE -> handleCreate(activity, activityHolder, emailAddress, isLinkedWithSource);
                    case UPDATE -> handleUpdate(orgId, activity, activityHolder, emailAddress, isLinkedWithSource, typeId, validTypeIds);
                    case DELETE -> handleDelete(activity, typeId, validTypeIds, emailAddress);
                }
                final String eventId = activity.getEventId();

                if (isCreateEvent && Objects.nonNull(eventId)) {
                    final Row calendarEventRow = new Row(calendarEventTable);

                    if (Objects.nonNull(syncEmailId)) {
                        calendarEventRow.setOrgId(orgId);
                        calendarEventRow.set(OMCALEXTERNALEVENTS.EXTERNAL_EVENT_ID, eventId);
                        calendarEventRow.set(OMCALEXTERNALEVENTS.REFERENCE_ID, activity.getReferenceId());
                        calendarEventRow.set(OMCALEXTERNALEVENTS.ACTIVITY_ID, activity.id());
                        calendarEventRow.set(OMCALEXTERNALEVENTS.CALENDAR_SYNC_ID, syncEmailId);

                        calendarEventRows.add(calendarEventRow);
                    }
                }
                orgDataStore(orgId).addRows(calendarEventRows);
            }
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Activity Calendar sync action failed, message: {}, trace: {}", exception.getMessage(), exception);
        }
    }

    /**
     * <p>
     * Handles calendar event creation for an activity by converting the given {@link ActivityHolder}
     * into a {@link CalendarEvent} and adding it to the user's calendar.
     * </p>
     *
     * @param activity           The {@link Activity} being created.
     * @param holder             The {@link ActivityHolder} containing activity details.
     * @param emailAddress       The email address of the synced calendar.
     * @param isLinkedWithSource Indicates whether the event is linked with its source entity.
     */
    private void handleCreate(final Activity activity, final ActivityHolder holder,
                              final EmailAddress emailAddress, final boolean isLinkedWithSource) {
        final CalendarEvent calendarEvent = calendarEventMapper.convertActivityHolderToEvent(holder, isLinkedWithSource);

        calendarService.addEvent(calendarEvent, emailAddress);
        activity.setEventId(calendarEvent.getEventId());
        activity.setReferenceId(calendarEvent.getReferenceId());
    }

    /**
     * <p>
     * Handles calendar event update for an activity by checking if the corresponding event exists,
     * and then updating it if valid. If the event type is no longer applicable, it deletes the event.
     * </p>
     *
     * @param orgId              The organization ID.
     * @param activity           The {@link Activity} to be updated.
     * @param holder             The {@link ActivityHolder} containing updated activity data.
     * @param emailAddress       The email address of the synced calendar.
     * @param isLinkedWithSource Indicates whether the event is linked with its source entity.
     * @param typeId             The type ID of the activity.
     * @param validTypeIds       The set of valid activity type IDs configured for synchronization.
     */
    private void handleUpdate(final long orgId, final Activity activity, final ActivityHolder holder,
                              final EmailAddress emailAddress, final boolean isLinkedWithSource,
                              final Long typeId, final Collection<Long> validTypeIds) {
        final String eventId = activityManager.getEventId(orgId, activity.id());

        if (validTypeIds.contains(typeId)) {
            if (Objects.nonNull(eventId)) {
                final CalendarEvent calendarEvent = calendarEventMapper.convertActivityHolderToEvent(holder, isLinkedWithSource);

                calendarEvent.setEventId(eventId);

                if (calendarService.eventExists(eventId, emailAddress)) {
                    activity.setEventId(calendarService.updateEvent(calendarEvent, emailAddress));
                }
            }
        } else {
            deleteEvent(eventId, emailAddress);
        }
    }

    /**
     * <p>
     * Handles deletion of a calendar event associated with an activity.
     * The event is deleted only if it belongs to a valid type.
     * </p>
     *
     * @param activity     The {@link Activity} whose event is to be deleted.
     * @param typeId       The type ID of the activity.
     * @param validTypeIds The collection of valid type IDs configured for synchronization.
     * @param emailAddress The email address of the synced calendar.
     */
    private void handleDelete(final Activity activity, final Long typeId, final Collection<Long> validTypeIds, final EmailAddress emailAddress) {
        if (validTypeIds.contains(typeId)) {
            deleteEvent(activity.getEventId(), emailAddress);
        }
    }

    /**
     * <p>
     * Deletes a calendar event for the specified user if the event exists.
     * </p>
     *
     * @param eventId      The unique identifier of the event to be deleted.
     * @param emailAddress The email address of the user whose event should be deleted.
     */
    private void deleteEvent(final String eventId, final EmailAddress emailAddress) {
        if (Objects.nonNull(eventId) && calendarService.eventExists(eventId, emailAddress)) {
            calendarService.deleteEvent(eventId, emailAddress);
        }
    }
}