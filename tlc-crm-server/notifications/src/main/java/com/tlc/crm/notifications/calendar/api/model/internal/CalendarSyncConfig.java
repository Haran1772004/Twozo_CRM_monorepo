package com.tlc.crm.notifications.calendar.api.model.internal;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.features.api.activity.model.ActivityType;

import com.tlc.crm.notifications.calendar.fields.CalendarFields;
import com.tlc.crm.notifications.calendar.internal.status.CalendarErrorCodes;

import com.tlc.crm.common.sync.SyncMode;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * <p>
 * Allows users to manage and customize configuration settings for calendar synchronization.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class CalendarSyncConfig {

    private final Map<SyncMode, ActivityType> syncTypeMap = new HashMap<>();
    private Map<Config, Object> configMap;
    private Collection<ActivityType> activityTypes;

    public CalendarSyncConfig() {
    }

    public CalendarSyncConfig(final String config) {
        this.configMap = parseConfig(config);
    }

    /**
     * <p>
     * Represents the configuration for calendar sync.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public enum Config {

        /**
         * Represents the synchronization type of the calendar, indicating whether it's one-way or two-way.
         */
        SYNC_TYPE("syncType"),

        /**
         * Represents the collection of activity types associated with the calendar.
         */
        ACTIVITY_TYPES("activityTypes"),

        /**
         * Indicates whether the calendar is linked to a data source.
         */
        SOURCE_LINK_STATUS("sourceLinkStatus"),
        ;

        private final String name;

        Config(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public Collection<Long> getActivityTypeIds() {
        return (Collection<Long>) configMap.get(Config.ACTIVITY_TYPES);
    }

    public SourceLinkStatus getLinkedWithSource() {
        return SourceLinkStatus.get((String) configMap.get(Config.SOURCE_LINK_STATUS));
    }

    public String getSyncType() {
        return (String) getSyncTypeMap().get(CalendarFields.TYPE);
    }

    public Long getCalendarEventId() {
        return (Long) getSyncTypeMap().get(CalendarFields.CALENDAR_EVENT_ID);
    }

    public void setSyncTypeConfig(final ActivityType activityType) {
        syncTypeMap.put(SyncMode.getMode(getSyncType()), activityType);
    }

    public Map<SyncMode, ActivityType> getSyncTypeConfig() {
        return syncTypeMap;
    }

    public void setActivityTypes(final Collection<ActivityType> activityTypes) {
        this.activityTypes = activityTypes;

        configMap.put(Config.ACTIVITY_TYPES, activityTypes.stream().map(ActivityType::id).collect(Collectors.toUnmodifiableSet()));
    }

    public Collection<ActivityType> getActivityTypes() {
        return activityTypes;
    }

    public boolean contains(final Config config) {
        return configMap.containsKey(config);
    }

    public Map<String, Object> getSyncTypeMap() {
        return (Map<String, Object>) configMap.get(Config.SYNC_TYPE);
    }

    public String getConfig() {
        return configMap.toString();
    }

    /**
     * <p>
     * Represents the {@link SourceLinkStatus} which defines whether a source should be linked or not.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     */
    public enum SourceLinkStatus {

        /**
         * Represents the case where the source should be linked.
         */
        LINKED("linked"),

        /**
         * Represents the case where the source should not be linked.
         */
        NOT_LINKED("notLinked"),
        ;

        private final String status;

        SourceLinkStatus(final String status) {
            this.status = status;
        }

        public String getName() {
            return status;
        }

        /**
         * <p>
         * Gets the {@link SourceLinkStatus}.
         * </p>
         *
         * @param name Represents the name
         * @return {@link SourceLinkStatus}
         */
        public static SourceLinkStatus get(final String name) {
            return switch (name.toLowerCase()) {
                case "linked" -> LINKED;
                case "notlinked" -> NOT_LINKED;
                default -> throw ErrorCode.get(CalendarErrorCodes.INVALID_LINKED_SOURCE_STATUS,
                        "i18n.notifications.calendar.sync.linked.source.invalid");
            };
        }
    }

    /**
     * <p>
     * Gets the parsed collection of configuration options from the given config.
     * </p>
     *
     * @param config Represents the configuration to be parsed.
     * @return A map containing configuration options and their corresponding values.
     */
    private Map<Config, Object> parseConfig(final String config) {
        final JsonObject jsonObject = Json.object(config);
        final JsonObject syncTypeJson = jsonObject.getJsonObject(Config.SYNC_TYPE.getName());
        final Map<Config, Object> calendarSyncConfigs = new HashMap<>();
        final Map<String, Object> syncTypeConfigs = new HashMap<>();
        final String typeKey = CalendarFields.TYPE;
        final String type = syncTypeJson.getString(typeKey);

        syncTypeConfigs.put(typeKey, type);
        syncTypeConfigs.put(CalendarFields.CALENDAR_EVENT_ID, syncTypeJson.getLong(CalendarFields.CALENDAR_EVENT_ID));

        calendarSyncConfigs.put(Config.SYNC_TYPE, syncTypeConfigs);
        calendarSyncConfigs.put(Config.ACTIVITY_TYPES, jsonObject.getJsonArray(CalendarFields.ACTIVITY_TYPES).toList()
                .stream().map(id -> ((Number) id).longValue()).collect(Collectors.toUnmodifiableSet()));
        calendarSyncConfigs.put(Config.SOURCE_LINK_STATUS, jsonObject.getString(Config.SOURCE_LINK_STATUS.getName()));

        return calendarSyncConfigs;
    }

    /**
     * <p>
     * Gets the default calendar sync configuration.
     * </p>
     *
     * @param activityTypes Represents the {@link Collection} of {@link ActivityType}
     * @return the default configuration
     */
    public static String getDefaultConfigJsonAsString(final Collection<ActivityType> activityTypes) {
        final JsonObject calendarSyncConfigJson = Json.object();
        final JsonObject syncTypeJson = Json.object();

        syncTypeJson.put(CalendarFields.TYPE, SyncMode.TWO_WAY.getMode());

        final JsonArray activityTypeArray = Json.array();

        if (!activityTypes.isEmpty()) {
            final Iterator<ActivityType> iterator = activityTypes.iterator();

            if (iterator.hasNext()) {
                final ActivityType firstActivityType = iterator.next();

                syncTypeJson.put(CalendarFields.CALENDAR_EVENT_ID, firstActivityType.id());
                activityTypeArray.put(firstActivityType.id());

                for (final ActivityType activityType : activityTypes) {

                    if (!Objects.equals(activityType, firstActivityType)) {
                        activityTypeArray.put(activityType.id());
                    }
                }
            }
        }
        calendarSyncConfigJson.put(CalendarFields.SYNC_TYPE, syncTypeJson);
        calendarSyncConfigJson.put(CalendarFields.ACTIVITY_TYPES, activityTypeArray);
        calendarSyncConfigJson.put(CalendarFields.SOURCE_LINK_STATUS, SourceLinkStatus.LINKED.getName());

        return calendarSyncConfigJson.toString();
    }

    /**
     * <p>
     * Gets the calendar sync configuration.
     * </p>
     *
     * @return the sync config
     */
    public String getConfigJsonAsString() {
        final JsonObject calendarSyncConfigJson = Json.object();
        final JsonObject syncTypeJson = Json.object();
        final Long calendarEventId = getCalendarEventId();

        if (Objects.nonNull(calendarEventId)) {
            syncTypeJson.put(CalendarFields.CALENDAR_EVENT_ID, calendarEventId);
        }
        syncTypeJson.put(CalendarFields.TYPE, getSyncType());
        calendarSyncConfigJson.put(CalendarFields.SYNC_TYPE, syncTypeJson);
        calendarSyncConfigJson.put(CalendarFields.ACTIVITY_TYPES, getActivityTypeIds());
        calendarSyncConfigJson.put(CalendarFields.SOURCE_LINK_STATUS, getLinkedWithSource().getName());

        return calendarSyncConfigJson.toString();
    }
}
