package com.tlc.crm.entity.features.api.activity.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.map.util.MapUrlProvider;
import com.tlc.crm.common.iam.api.model.converter.CrmUserConverter;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.service.UserService;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.features.api.activity.model.Activity;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.Availability;
import com.tlc.crm.entity.features.api.activity.model.CheckInOutRecord;
import com.tlc.crm.entity.features.api.activity.model.Priority;
import com.tlc.crm.entity.features.api.activity.model.Reminder;
import com.tlc.crm.entity.features.api.activity.model.Status;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.crm.entity.features.api.note.model.converter.NoteConverter;
import com.tlc.crm.entity.features.field.activity.ActivityFields;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.util.NumberUtils;
import com.tlc.crm.icon.model.Icon;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.resolver.DateTimeUtil;

import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.Map;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link Activity}
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see EntityConverter
 */
public class ActivityConverter {

    private static final int DEFAULT_TIME_DIFFERENCE_IN_MINUTES = 30;

    private final ActivityTypeConverter activityTypeConverter;
    private final NoteConverter noteConverter;
    private final CrmUserConverter userConverter;
    private final UserService userService;
    private final DateTimeUtil dateTimeUtil;

    private static class Instance {
        private static final ActivityConverter INSTANCE = new ActivityConverter();
    }

    /**
     * <p>
     * Gets the {@link ActivityConverter} Instance
     * </p>
     *
     * @return {@link ActivityConverter}
     */
    public static ActivityConverter getInstance() {
        return Instance.INSTANCE;
    }

    private ActivityConverter() {
        noteConverter = NoteConverter.getInstance();
        activityTypeConverter = ActivityTypeConverter.getInstance();
        userConverter = CrmUserConverter.getInstance();
        userService = UserService.getInstance();
        dateTimeUtil = DateTimeUtil.getInstance();
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link Activity}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activity        Holds the activity related data
     * @param jsonObject      Which contains the request data
     * @return {@link Activity} filled with the data
     */
    public Activity buildActivity(final ConverterConfig converterConfig, final Activity activity, final JsonObject jsonObject) {
        final String idKey = ActivityFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            activity.setId(jsonObject.getLong(idKey));
        }
        setJsonToActivity(converterConfig, activity, jsonObject);

        return activity;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link Activity}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Which contains the request data
     * @return {@link Activity} filled with the data
     */
    public Activity buildActivity(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return buildActivity(converterConfig, new Activity(), jsonObject);
    }

    /**
     * <p>
     * Sets the json data into the {@link Activity}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activity        An entity that holds the activity attributes
     * @param jsonObject      Which contains the data
     */
    private void setJsonToActivity(final ConverterConfig converterConfig, final Activity activity, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();
        final String titleKey = ActivityFields.TITLE.getName();
        final String availabilityKey = ActivityFields.AVAILABILITY.getName();
        final String reminderKey = ActivityFields.REMINDER.getName();
        final String priorityKey = ActivityFields.PRIORITY.getName();
        final String collaboratorsKey = ActivityFields.COLLABORATORS.getName();
        final String typeIdKey = ActivityFields.TYPE_ID.getName();
        final String assigneeIdKey = ActivityFields.ASSIGNEE_ID.getName();
        final String startTimeKey = ActivityFields.START_TIME.getName();
        final String endTimeKey = ActivityFields.END_TIME.getName();
        final boolean isDone = jsonObject.optBoolean(ActivityFields.IS_DONE.getName(), activity.getDone());
        final String locationKey = ActivityFields.LOCATION.getName();
        final String descriptionKey = ActivityFields.DESCRIPTION.getName();
        final DateTime currentTime = new DateTime(Instant.now().toEpochMilli());
        final Long activityId = activity.id();
        final String noteKey = ActivityFields.NOTE.getName();
        final boolean containsStartTime = jsonObject.containsKey(startTimeKey);
        final boolean containsEndTime = jsonObject.containsKey(endTimeKey);

        if (containsStartTime) {
            activity.setStartTime(new DateTime(jsonObject.getLong(startTimeKey)));
        } else if (Objects.isNull(activity.getStartTime()) && !containsEndTime) {
            activity.setStartTime(currentTime);
        }

        if (containsEndTime) {
            final long endTimeStamp = jsonObject.getLong(endTimeKey);

            activity.setEndTime(new DateTime(endTimeStamp));

            if (!containsStartTime && Objects.isNull(activity.getStartTime())) {
                final Calendar calendar = Calendar.getInstance();

                calendar.setTimeInMillis(endTimeStamp);
                calendar.add(Calendar.MINUTE, -DEFAULT_TIME_DIFFERENCE_IN_MINUTES);

                activity.setStartTime(new DateTime(calendar.getTimeInMillis()));
            }
        } else if (Objects.isNull(activity.getEndTime())) {
            final Calendar calendar = Calendar.getInstance();

            calendar.setTimeInMillis(activity.getStartTime().getTimeStamp());
            calendar.add(Calendar.MINUTE, DEFAULT_TIME_DIFFERENCE_IN_MINUTES);

            activity.setEndTime(new DateTime(calendar.getTimeInMillis()));
        }
        final Long startTime = activity.getStartTime().getTimeStamp();
        final Long endTime = activity.getEndTime().getTimeStamp();

        activity.setDuration((int) TimeUnit.MILLISECONDS.toMinutes(endTime - startTime));
        activity.setOrgId(orgId);
        activity.setCreatorId(converterConfig.ownerId());
        activity.setModifiedById(converterConfig.ownerId());

        if (jsonObject.containsKey(assigneeIdKey)) {
            setAssigneeId(jsonObject.getLong(assigneeIdKey), activity);
        } else {
            setAssigneeId(converterConfig.isPartialData() ? activity.getAssigneeId() : converterConfig.ownerId(), activity);
        }
        activity.setDone(isDone);
        activity.setDescription(jsonObject.optNullableValue(descriptionKey, activity.getDescription()));
        activity.setLocation(jsonObject.optNullableValue(locationKey, activity.getLocation()));

        if (jsonObject.containsKey(noteKey)) {
            if (jsonObject.isKeyPresentWithNullValue(noteKey)) {
                activity.setNote(null);
            } else {
                activity.setNote(noteConverter.convert(converterConfig, jsonObject.getJsonObject(noteKey)));
            }
        }

        if (jsonObject.containsKey(availabilityKey)) {
            if (jsonObject.isKeyPresentWithNullValue(availabilityKey)) {
                activity.setAvailability(null);
            } else {
                activity.setAvailability(Availability.get(Math.toIntExact(jsonObject.getInt(availabilityKey))));
            }
        } else if (Objects.isNull(activityId)) {
            activity.setAvailability(Availability.BUSY);
        }

        if (jsonObject.containsKey(reminderKey)) {
            if (jsonObject.isKeyPresentWithNullValue(reminderKey)) {
                activity.setReminder(null);
            } else {
                activity.setReminder(Reminder.get(Math.toIntExact(jsonObject.getInt(reminderKey))));
            }
        } else if (Objects.isNull(activityId)) {
            activity.setReminder(Reminder.NO_REMINDER);
        }

        if (jsonObject.containsKey(priorityKey)) {
            if (jsonObject.isKeyPresentWithNullValue(priorityKey)) {
                activity.setPriority(null);
            } else {
                activity.setPriority(Priority.get(Math.toIntExact(jsonObject.getInt(priorityKey))));
            }
        } else if (Objects.isNull(activityId)) {
            activity.setPriority(Priority.MEDIUM);
        }

        if (isDone) {
            activity.setStatus(Status.COMPLETED);
            activity.setCompletedTime(currentTime);
        } else {
            activity.setStatus(currentTime.getTimeStamp() < startTime ? Status.UPCOMING : Status.OVERDUE);
        }

        if (jsonObject.containsKey(typeIdKey)) {
            if (jsonObject.isKeyPresentWithNullValue(typeIdKey)) {
                activity.setType(null);
            } else {
                final ActivityType activityType = new ActivityType();

                activityType.setId(jsonObject.getLong(typeIdKey));
                activityType.setOrgId(orgId);
                activity.setType(activityType);
            }
        }

        if (jsonObject.containsKey(titleKey)) {
            activity.setTitle(jsonObject.getNullableString(titleKey));
        }

        if (jsonObject.containsKey(collaboratorsKey)) {
            if (jsonObject.isKeyPresentWithNullValue(collaboratorsKey)) {
                activity.setCollaboratorIds(null);
            } else {
                final Collection<Long> collaborators = new HashSet<>();
                final String valueKey = ActivityFields.VALUE.getName();

                jsonObject.getJsonArray(collaboratorsKey).forEach(collaborator -> {
                    if (collaborator instanceof Map) {
                        final Map<?, ?> collaboratorMap = (Map<?, ?>) collaborator;

                        collaborators.add(NumberUtils.parseNumber(collaboratorMap.get(valueKey)).longValue());
                    } else {
                        collaborators.add(NumberUtils.parseNumber(collaborator).longValue());
                    }
                });
                activity.setCollaboratorIds(collaborators);
            }
        }

        if (converterConfig.areReadOnlyFieldsRequired()) {
            final String createdAtKey = ActivityFields.CREATED_AT.getName();

            if (jsonObject.containsKey(createdAtKey)) {
                activity.setCreatedTime(new DateTime(jsonObject.getLong(createdAtKey)));
            }
        }
    }

    /**
     * <p>
     * Converts the {@link Activity} into the {@link JsonObject}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activity        An entity that holds the activity attributes
     * @return the {@link JsonObject} filled with data
     */
    public JsonObject convert(final ConverterConfig converterConfig, final Activity activity, final JsonObject activityJson) {
        final Note note = activity.getNote();
        final ActivityType activityType = activity.getType();
        final Long orgId = activity.orgId();
        final Collection<Long> collaborators = activity.getCollaboratorIds();
        final Status status = activity.getStatus();
        final Reminder reminder = activity.getReminder();
        final Availability availability = activity.getAvailability();
        final Priority priority = activity.getPriority();
        final CrmUser assignee = activity.getAssignee();
        final CrmUser creator = activity.getCreator();
        final CrmUser modifiedBy = activity.getModifiedBy();
        final Long createdTime = dateTimeUtil.getTimeStamp(activity.getCreatedTime());
        final Long updatedTime = dateTimeUtil.getTimeStamp(activity.getUpdatedTime());

        activityJson.put(ActivityFields.ID.getName(), activity.id());
        activityJson.put(ActivityFields.ORG_ID.getName(), orgId);
        activityJson.put(ActivityFields.TITLE.getName(), activity.getTitle());
        activityJson.put(ActivityFields.START_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getStartTime()));
        activityJson.put(ActivityFields.CREATED_TIME.getName(), createdTime);
        activityJson.put(ActivityFields.UPDATED_TIME.getName(), updatedTime);
        activityJson.put(ActivityFields.IS_DONE.getName(), activity.getDone());
        activityJson.put(ActivityFields.STATUS.getName(), Objects.isNull(status) ? null : status.getId());
        activityJson.put(ActivityFields.REMINDER.getName(), Objects.isNull(reminder) ? null : reminder.getId());
        activityJson.put(ActivityFields.AVAILABILITY.getName(), Objects.isNull(availability) ? null : availability.getId());
        activityJson.put(ActivityFields.PRIORITY.getName(), Objects.isNull(priority) ? null : priority.getId());
        activityJson.put(ActivityFields.END_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getEndTime()));
        activityJson.put(ActivityFields.DURATION.getName(), activity.getDuration());
        activityJson.put(ActivityFields.LOCATION.getName(), activity.getLocation());
        activityJson.put(ActivityFields.DESCRIPTION.getName(), activity.getDescription());
        loadCheckInOutDataIntoJson(converterConfig.getOperatingSystem(), activity, activityJson);

        if (Objects.nonNull(activityType)) {
            activityJson.put(ActivityFields.ACTIVITY_TYPE.getName(), activityTypeConverter.convert(converterConfig, activityType));
        }

        if (activity.getDone()) {
            activityJson.put(ActivityFields.COMPLETED_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getCompletedTime()));
        }

        if (Objects.nonNull(note)) {
            activityJson.put(ActivityFields.NOTE.getName(), noteConverter.convertPartialData(note));
        }

        if (!collaborators.isEmpty()) {
            activityJson.put(ActivityFields.COLLABORATORS.getName(), convertCollaborators(orgId, collaborators));
        }

        if (Objects.nonNull(assignee)) {
            activityJson.put(ActivityFields.ASSIGNEE.getName(), userConverter.convert(assignee, true, false));
        }

        if (Objects.nonNull(creator) && Objects.nonNull(modifiedBy)) {
            final JsonObject editInfoJson = Json.object();

            editInfoJson.put(ActivityFields.CREATED_TIME.getName(), createdTime);
            editInfoJson.put(ActivityFields.UPDATED_TIME.getName(), updatedTime);
            editInfoJson.put(ActivityFields.CREATED_BY.getName(), creator.getFullName());
            editInfoJson.put(ActivityFields.LAST_MODIFIED_BY.getName(), modifiedBy.getFullName());
            activityJson.put(ActivityFields.EDIT_INFO.getName(), editInfoJson);
        }

        return activityJson;
    }

    /**
     * <p>
     * Converts the collaborators into {@link JsonArray}.
     * </p>
     *
     * @param orgId         Represents the organisation id.
     * @param collaborators Represents the collaborators ids.
     * @return Converted {@link JsonArray}
     */
    private JsonArray convertCollaborators(final Long orgId, final Collection<Long> collaborators) {
        final JsonArray jsonArray = Json.array();

        for (final Long collaborator : collaborators) {
            jsonArray.put(userConverter.convert(userService.getUser(orgId, collaborator), true, false));
        }

        return jsonArray;
    }

    /**
     * <p>
     * Converts the {@link Activity} into the {@link JsonObject}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activity        An entity that holds the activity attributes
     * @return the {@link JsonObject} filled with data
     */
    public JsonObject convertPartial(final ConverterConfig converterConfig, final Activity activity) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(ActivityFields.ID.getName(), activity.id());
        jsonObject.put(ActivityFields.TITLE.getName(), activity.getTitle());
        jsonObject.put(ActivityFields.START_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getStartTime()));
        jsonObject.put(ActivityFields.END_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getEndTime()));
        jsonObject.put(ActivityFields.ACTIVITY_TYPE.getName(), activityTypeConverter.convert(converterConfig, activity.getType()));
        jsonObject.put(ActivityFields.IS_DONE.getName(), activity.getDone());
        jsonObject.put(ActivityFields.ROW_STATE.getName(), activity.getStatus().name());

        return jsonObject;
    }

    /**
     * <p>
     * Converts the {@link Activity} into a {@link JsonObject}, the activity contains only partial data.
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activity        An entity that holds the activity attributes
     * @return the {@link JsonObject} filled with data
     */
    public JsonObject convertActivityToJsonObject(final ConverterConfig converterConfig, final Activity activity) {
        final ActivityType activityType = activity.getType();
        final JsonObject activityJson = Json.object();

        activityJson.put(ActivityFields.ID.getName(), activity.id());
        activityJson.put(ActivityFields.ORG_ID.getName(), activity.orgId());
        activityJson.put(ActivityFields.IS_DONE.getName(), activity.getDone());
        activityJson.put(ActivityFields.TITLE.getName(), activity.getTitle());

        if (Objects.nonNull(activityType)) {
            activityJson.put(ActivityFields.ACTIVITY_TYPE.getName(), activityTypeConverter.convert(converterConfig, activityType));
        }

        return activityJson;
    }

    /**
     * <p>
     * Converts the {@link Activity} summary into the {@link JsonObject}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activity        An entity that holds the activity attributes
     * @return the {@link JsonObject} filled with data
     */
    public JsonObject convertSummary(final ConverterConfig converterConfig, final Activity activity, final JsonObject activityJson) {
        final Note note = activity.getNote();
        final ActivityType activityType = activity.getType();
        final Long orgId = activity.orgId();
        final Status status = activity.getStatus();
        final Availability availability = activity.getAvailability();
        final Reminder reminder = activity.getReminder();
        final Priority priority = activity.getPriority();
        final Optional<Long> endTime = Optional.ofNullable(dateTimeUtil.getTimeStamp(activity.getEndTime()));

        activityJson.put(ActivityFields.ID.getName(), activity.id());
        activityJson.put(ActivityFields.ORG_ID.getName(), orgId);
        activityJson.put(ActivityFields.TITLE.getName(), activity.getTitle());
        activityJson.put(ActivityFields.START_TIME.getName(), activity.getStartTime().getTimeStamp());
        activityJson.put(ActivityFields.IS_DONE.getName(), activity.getDone());
        activityJson.put(ActivityFields.STATUS.getName(), Objects.isNull(status) ? null : status.getId());
        activityJson.put(ActivityFields.STATE.getName(), Objects.isNull(status) ? null : status);
        activityJson.put(ActivityFields.REMINDER.getName(), Objects.isNull(reminder) ? null : reminder.getId());
        activityJson.put(ActivityFields.AVAILABILITY.getName(), Objects.isNull(availability) ? null : availability.getId());
        activityJson.put(ActivityFields.PRIORITY.getName(), Objects.isNull(priority) ? null : priority.getId());
        activityJson.put(ActivityFields.DURATION.getName(), activity.getDuration());
        activityJson.put(ActivityFields.DESCRIPTION.getName(), activity.getDescription());
        activityJson.put(ActivityFields.LOCATION.getName(), activity.getLocation());
        activityJson.put(ActivityFields.OWNER.getName(), userConverter.convert(activity.getAssignee(), true, true));
        activityJson.put(ActivityFields.CREATED_BY.getName(), userConverter.convert(activity.getCreator(), true, true));
        activityJson.put(ActivityFields.CREATED_TIME.getName(), activity.getCreatedTime().getTimeStamp());
        activityJson.put(ActivityFields.LAST_MODIFIED_BY.getName(), userConverter.convert(activity.getModifiedBy(), true, true));
        activityJson.put(ActivityFields.LAST_MODIFIED_AT.getName(), activity.getUpdatedTime().getTimeStamp());
        loadCheckInOutDataIntoJson(converterConfig.getOperatingSystem(), activity, activityJson);

        endTime.ifPresent(time -> {
            activityJson.put(ActivityFields.END_TIME.getName(), time);

            if (Status.OVERDUE == status) {
                activityJson.put(ActivityFields.OVERDUE_DURATION.getName(), Instant.now().toEpochMilli() - time);
            }
        });

        if (Objects.nonNull(activityType)) {
            activityJson.put(ActivityFields.ACTIVITY_TYPE.getName(), activityTypeConverter.convert(converterConfig, activityType));
        }

        if (activity.getDone()) {
            activityJson.put(ActivityFields.COMPLETED_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getCompletedTime()));
        }

        if (Objects.nonNull(note)) {
            activityJson.put(ActivityFields.NOTE.getName(), noteConverter.convertPartialData(note));
        }

        return activityJson;
    }

    /**
     * <p>
     * Loads the activity {@link CheckInOutRecord} into json
     * </p>
     *
     * @param operatingSystem Represents the operating system name
     * @param activity        Represents the {@link Activity}
     * @param activityJson    Represents the activity json
     */
    private void loadCheckInOutDataIntoJson(final String operatingSystem, final Activity activity, final JsonObject activityJson) {
        final CheckInOutRecord checkInRecord = activity.getCheckInRecord();
        final CheckInOutRecord checkOutRecord = activity.getCheckOutRecord();

        if (Objects.nonNull(checkInRecord)) {
            final JsonObject checkInJson = checkInRecord.getDataJson();

            loadCheckInOutLocationUrl(operatingSystem, checkInJson);
            activityJson.put(ActivityFields.CHECK_IN.getName(), checkInJson);
        }

        if (Objects.nonNull(checkOutRecord)) {
            final JsonObject checkOutJson = checkOutRecord.getDataJson();

            loadCheckInOutLocationUrl(operatingSystem, checkOutJson);
            activityJson.put(ActivityFields.CHECK_OUT.getName(), checkOutJson);
            activityJson.put(ActivityFields.TOTAL_TIME_SPENT.getName(), activity.getTotalTimeSpent());
        }
        activityJson.put(ActivityFields.IS_CHECK_IN_VISIBLE.getName(), Objects.equals(LoggedInUserInfo.getUser().id(), activity.getAssigneeId()));
    }

    /**
     * <p>
     * Loads the location url for the {@link CheckInOutRecord}
     * </p>
     *
     * @param operatingSystem Represents the operation system name
     * @param dataJson        Represents the data json
     */
    private void loadCheckInOutLocationUrl(final String operatingSystem, final JsonObject dataJson) {

        if (Objects.nonNull(operatingSystem)) {
            dataJson.put(ActivityFields.LOCATION_URL.getName(), MapUrlProvider.fetchUrl(operatingSystem,
                    dataJson.getString(ActivityFields.LATITUDE.getName()), dataJson.getString(ActivityFields.LONGITUDE.getName()), null));
        }
    }

    /**
     * <p>
     * Assigns the assignee for the given activity
     * </p>
     *
     * @param activity   Represents the {@link Activity}
     * @param assigneeId Represents an assignee id
     */
    private void setAssigneeId(final Long assigneeId, final Activity activity) {
        final Long id = activity.getAssigneeId();

        activity.setNotifiable(Objects.isNull(id) || !Objects.equals(assigneeId, id));
        activity.setAssigneeId(assigneeId);
    }

    /**
     * <p>
     * Converts the {@link Activity} into the {@link JsonObject}
     * </p>
     *
     * @param converterConfig The configuration object providing settings for the conversion process, including locale.
     * @param activity        The {@link Activity} entity whose attributes are being converted into a JSON representation.
     * @param activityJson    The {@link JsonObject} that will be filled with activity data. This JSON object is updated and returned.
     * @return the {@link JsonObject} filled with data
     */
    public JsonObject convertModelToJson(final ConverterConfig converterConfig, final Activity activity, final JsonObject activityJson) {
        final Note note = activity.getNote();
        final ActivityType activityType = activity.getType();
        final Long orgId = activity.orgId();
        final Collection<Long> collaborators = activity.getCollaboratorIds();
        final Status status = activity.getStatus();
        final Reminder reminder = activity.getReminder();
        final Availability availability = activity.getAvailability();
        final Priority priority = activity.getPriority();
        final CrmUser assignee = activity.getAssignee();

        activityJson.put(ActivityFields.ID.getName(), activity.id());
        activityJson.put(ActivityFields.ORG_ID.getName(), orgId);
        activityJson.put(ActivityFields.TITLE.getName(), activity.getTitle());
        activityJson.put(ActivityFields.START_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getStartTime()));
        activityJson.put(ActivityFields.CREATED_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getCreatedTime()));
        activityJson.put(ActivityFields.UPDATED_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getUpdatedTime()));
        activityJson.put(ActivityFields.IS_DONE.getName(), activity.getDone());

        if (Objects.nonNull(status)) {
            activityJson.put(ActivityFields.STATUS.getName(), status.convertModelToOptionJson(status, converterConfig));
        }

        if (Objects.nonNull(reminder)) {
            activityJson.put(ActivityFields.REMINDER.getName(), reminder.convertModelToOptionJson(reminder, converterConfig));
        }

        if (Objects.nonNull(activityType)) {
            activityJson.put(ActivityFields.AVAILABILITY.getName(), availability.convertModelToOptionJson(availability, converterConfig));
        }

        if (Objects.nonNull(priority)) {
            activityJson.put(ActivityFields.PRIORITY.getName(), priority.convertModelToOptionJson(priority, converterConfig));
        }
        activityJson.put(ActivityFields.END_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getEndTime()));
        activityJson.put(ActivityFields.DURATION.getName(), activity.getDuration());
        activityJson.put(ActivityFields.LOCATION.getName(), activity.getLocation());
        activityJson.put(ActivityFields.DESCRIPTION.getName(), activity.getDescription());

        if (Objects.nonNull(activityType)) {
            activityJson.put(ActivityFields.ACTIVITY_TYPE.getName(), activityTypeConverter.convert(converterConfig, activityType));
        }

        if (activity.getDone()) {
            activityJson.put(ActivityFields.COMPLETED_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getCompletedTime()));
        }

        if (Objects.nonNull(note)) {
            activityJson.put(ActivityFields.NOTE.getName(), noteConverter.convertPartialData(note));
        }

        if (!collaborators.isEmpty()) {
            activityJson.put(ActivityFields.COLLABORATORS.getName(), convertCollaborators(orgId, collaborators));
        }

        if (Objects.nonNull(assignee)) {
            activityJson.put(ActivityFields.ASSIGNEE.getName(), userConverter.convert(assignee, true, false));
        }

        return activityJson;
    }

    /**
     * <p>
     * Converts the {@link Activity} into a {@link JsonObject}.
     * </p>
     *
     * @param activity An entity that holds the attributes of the activity, including  deal, company
     * @return {@link JsonObject} A JSON object filled with data extracted from the activity holder, including nested entities.
     */
    public JsonObject convertKanbanActivities(final Activity activity) {
        final JsonObject activityJson = Json.object();
        final CrmUser assignee = activity.getAssignee();
        final JsonObject titleJson = Json.object();
        final ActivityType activityType = activity.getType();
        final Status status = activity.getStatus();
        final CheckInOutRecord checkInRecord = activity.getCheckInRecord();
        final CheckInOutRecord checkOutRecord = activity.getCheckOutRecord();

        titleJson.put(ActivityFields.ACTIVITY_TITLE.getName(), activity.getTitle());

        if (Objects.nonNull(activityType)) {
            final Icon activityIcon = activityType.getActivityIcon();

            if (Objects.nonNull(activityIcon)) {
                titleJson.put(ActivityFields.ICON_NAME.getName(), activityIcon.getName());
            }
        }

        activityJson.put(ActivityFields.TITLE.getName(), titleJson);
        activityJson.put(ActivityFields.IS_DONE.getName(), activity.getDone());
        activityJson.put(ActivityFields.ID.getName(), activity.id());
        activityJson.put(ActivityFields.ORG_ID.getName(), activity.orgId());
        activityJson.put(ActivityFields.START_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getStartTime()));
        activityJson.put(ActivityFields.END_TIME.getName(), dateTimeUtil.getTimeStamp(activity.getEndTime()));
        activityJson.put(ActivityFields.STATUS.getName(), activity.getStatus());
        activityJson.put(ActivityFields.IS_CHECK_IN_VISIBLE.getName(), Objects.equals(LoggedInUserInfo.getUser().id(), activity.getAssigneeId()));

        if (Objects.nonNull(checkInRecord)) {
            activityJson.put(ActivityFields.CHECK_IN.getName(), checkInRecord.getDataJson());
        }

        if (Objects.nonNull(checkOutRecord)) {
            activityJson.put(ActivityFields.CHECK_OUT.getName(), checkOutRecord.getDataJson());
            activityJson.put(ActivityFields.TOTAL_TIME_SPENT.getName(), activity.getTotalTimeSpent());
        }

        if (Objects.nonNull(assignee)) {
            activityJson.put(ActivityFields.ASSIGNEE.getName(), userConverter.convert(assignee, true, false));
        }

        if (Objects.nonNull(status)) {
            activityJson.put(ActivityFields.IS_OVER_DUE.getName(), Status.OVERDUE == status);
        }

        return activityJson;
    }

    /**
     * <p>
     * Converts the check-in or check-out details
     * </p>
     *
     * @param data  Represents the data json
     * @return The {@link CheckInOutRecord}
     */
    public CheckInOutRecord convertCheckInOutDetails(final JsonObject data) {
        final CheckInOutRecord checkInOutRecord = new CheckInOutRecord();
        final String latitudeKey = ActivityFields.LATITUDE.getName();
        final String longitudeKey = ActivityFields.LONGITUDE.getName();

        if (data.containsKey(latitudeKey)) {
            checkInOutRecord.setLatitude(data.getDouble(latitudeKey));
        }

        if (data.containsKey(longitudeKey)) {
            checkInOutRecord.setLongitude(data.getDouble(longitudeKey));
        }
        checkInOutRecord.setActivityId(data.getLong(ActivityFields.ID.getName()));
        checkInOutRecord.setLocation(data.optString(ActivityFields.LOCATION.getName(), null));
        checkInOutRecord.setTime(data.optLong(ActivityFields.TIME.getName(), Instant.now().toEpochMilli()));

        return checkInOutRecord;
    }
}
