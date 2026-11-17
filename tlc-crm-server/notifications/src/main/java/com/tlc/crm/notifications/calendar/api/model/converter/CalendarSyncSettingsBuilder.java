package com.tlc.crm.notifications.calendar.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;

import com.tlc.crm.notifications.calendar.api.manager.CalendarSyncManager;
import com.tlc.crm.notifications.calendar.api.model.CalendarSyncSettings;
import com.tlc.crm.notifications.calendar.api.model.internal.CalendarSyncConfig;
import com.tlc.crm.notifications.calendar.fields.CalendarFields;

import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.crm.common.sync.SyncMode;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Builds and configures the {@link CalendarSyncSettings}. Use this builder to set various
 * synchronization settings before initiating calendar synchronization.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class CalendarSyncSettingsBuilder {

    private final ActivityTypeManager activityTypeManager;
    private final CalendarSyncManager calendarSyncManager;

    private CalendarSyncSettingsBuilder() {
        this.activityTypeManager = ActivityTypeManager.getInstance();
        this.calendarSyncManager = CalendarSyncManager.getInstance();
    }

    private static class Instance {
        private static final CalendarSyncSettingsBuilder INSTANCE = new CalendarSyncSettingsBuilder();
    }

    /**
     * <p>
     * Gets the {@link CalendarSyncSettingsBuilder} Instance
     * </p>
     *
     * @return {@link CalendarSyncSettingsBuilder}
     */
    public static CalendarSyncSettingsBuilder getInstance() {
        return CalendarSyncSettingsBuilder.Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link CalendarSyncSettings}
     * </p>
     *
     * @param orgId      Represents an organization id.
     * @param userId     Represents a user id
     * @param jsonObject Represents the {@link JsonObject}
     * @return entity of {@link CalendarSyncSettings}
     */
    public CalendarSyncSettings convert(final long orgId, final long userId, final Long syncId, final JsonObject jsonObject) {
        final CalendarSyncSettings calendarSyncSettings = new CalendarSyncSettings();

        calendarSyncSettings.setId(syncId);
        calendarSyncSettings.setOrgId(orgId);
        calendarSyncSettings.setUserId(userId);

        if (jsonObject.containsKey(CalendarFields.SYNC_CONFIG)) {
            final JsonObject syncConfigJson = jsonObject.getJsonObject(CalendarFields.SYNC_CONFIG);
            final JsonObject syncType = syncConfigJson.getJsonObject(CalendarFields.SYNC_TYPE);

            if (!syncType.containsKey(CalendarFields.CALENDAR_EVENT_ID)) {
                final Optional<String> calendarSyncConfig = calendarSyncManager.getCalendarSyncConfig(orgId, userId);

                calendarSyncConfig.ifPresent(syncConfig -> syncType.put(CalendarFields.CALENDAR_EVENT_ID,
                        Json.object(syncConfig).getJsonObject(CalendarFields.SYNC_TYPE).getLong(CalendarFields.CALENDAR_EVENT_ID)));
            }
            final CalendarSyncConfig syncConfig = new CalendarSyncConfig(syncConfigJson.toString());

            syncConfig.setSyncTypeConfig(Objects.equals(SyncMode.TWO_WAY, SyncMode.getMode(syncConfig.getSyncType()))
                    ? activityTypeManager.get(orgId, syncConfig.getCalendarEventId()) : null);
            syncConfig.setActivityTypes(activityTypeManager.get(orgId, syncConfig.getActivityTypeIds()));

            calendarSyncSettings.setCalendarSyncConfig(syncConfig);
        }

        return calendarSyncSettings;
    }

    /**
     * <p>
     * Converts the {@link CalendarSyncSettings} to {@link JsonObject}.
     * </p>
     *
     * @param calendarSyncSettings Represents the {@link CalendarSyncSettings}
     * @param crmResponse          Represents the {@link CrmResponse}
     * @return {@link JsonObject} of calendar sync settings configuration
     */
    public JsonObject convertCalendarSyncToJson(final CalendarSyncSettings calendarSyncSettings, final CrmResponse crmResponse) {
        final JsonObject jsonObject = Json.object();
        final CalendarSyncConfig calendarSyncConfig = calendarSyncSettings.getDisplaySyncConfig();
        final JsonObject configJson = Json.object();
        final JsonObject syncTypeJson = Json.object();
        final JsonArray activityTypeArray = Json.array();
        final String syncType = calendarSyncConfig.getSyncType();

        syncTypeJson.put(CalendarFields.TYPE, syncType);
        syncTypeJson.put(CalendarFields.CALENDAR_EVENT_ID, calendarSyncConfig.getCalendarEventId());

        final Collection<ActivityType> activityTypes = calendarSyncConfig.getActivityTypes();

        if (Objects.nonNull(activityTypes) && !activityTypes.isEmpty()) {

            for (final ActivityType activityType : activityTypes) {
                final JsonObject typeJson = Json.object();

                typeJson.put(CalendarFields.ID, activityType.id());
                typeJson.put(CalendarFields.NAME, activityType.getName());
                typeJson.put(CalendarFields.IS_ACTIVE, activityType.getActive());

                activityTypeArray.put(typeJson);
            }
        }
        configJson.put(CalendarFields.SYNC_TYPE, syncTypeJson);
        configJson.put(CalendarFields.ACTIVITY_TYPES, activityTypeArray);
        configJson.put(CalendarFields.SOURCE_LINK_STATUS, calendarSyncConfig.getLinkedWithSource().getName());

        jsonObject.put(CalendarFields.SYNC_CONFIG, configJson);
        jsonObject.put(CalendarFields.ID, calendarSyncSettings.id());
        jsonObject.put(CalendarFields.ORG_ID, calendarSyncSettings.orgId());
        jsonObject.put(CalendarFields.USER_ID, calendarSyncSettings.getUserId());
        jsonObject.put(CalendarFields.STATE, crmResponse.resolveI18nString(SyncStatus.getStateValue(calendarSyncSettings.getSyncStatus())));
        jsonObject.put(CalendarFields.LAST_SYNC_TIME, calendarSyncSettings.getLastSyncTime());
        jsonObject.put(CalendarFields.EMAIL_ADDRESS, calendarSyncSettings.getAddress().getEmailId());

        return jsonObject;
    }
}