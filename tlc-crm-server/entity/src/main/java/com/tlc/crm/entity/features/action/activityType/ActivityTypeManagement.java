package com.tlc.crm.entity.features.action.activityType;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.api.activity.model.converter.ActivityTypeConverter;
import com.tlc.crm.entity.features.field.activity.ActivityTypeFields;
import com.tlc.crm.entity.internal.status.ActivityErrorCodes;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.crm.security.permission.annotation.authorizer.SettingsAuthorizer;
import com.tlc.crm.security.permission.model.Category;
import com.tlc.crm.security.permission.model.SubCategory;
import com.tlc.web.WebAction;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * <p>
 * Manages {@link ActivityType} related web actions
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see CrmConfigAction
 */
@SettingsAuthorizer(category = Category.Type.ADMIN_ACCESS, subCategory = SubCategory.Type.MANAGE_SALES_ACTIVITIES, needActionHandling = true)
@WebAction(path = "/activity/type/mgmt")
public class ActivityTypeManagement extends CrmConfigAction<ActivityType> {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 50;
    private static final long DEACTIVATION_RESTRICTION_LIMIT = 1;

    private final ConverterConfigBuilder converterConfigBuilder;
    private final ActivityTypeManager activityTypeManager;
    private final ActivityTypeConverter activityTypeConverter;

    public ActivityTypeManagement() {
        converterConfigBuilder = ConverterConfigBuilder.getInstance();
        activityTypeManager = ActivityTypeManager.getInstance();
        activityTypeConverter = ActivityTypeConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final String type = requestJson.getString("type");
        final JsonObject data = requestJson.getJsonObject("data");
        final Long orgId = request.orgId();
        final Long userId = request.userId();

        switch (type) {
            case "list" -> response.put(ActivityTypeFields.ACTIVITY_TYPES.getName(), getActivityTypeList(orgId, data, request, response));
            case "updateStatus" -> updateStatus(orgId, userId, data, response);
            case "updateOrder" -> updateActivityTypesOrder(orgId, data);
        }
    }

    /**
     * Gets the list of activity type
     *
     * @param orgId    Represents the organization id
     * @param data     Represents the request data
     * @param request  Represents the crm request
     * @param response Represents the crm response
     * @return {@link JsonArray} of activity type
     */
    private JsonArray getActivityTypeList(final Long orgId, final JsonObject data, final CrmRequest request, final CrmResponse response) {
        final JsonArray jsonArray = Json.array();
        final Boolean isActive = data.containsKey(ActivityTypeFields.IS_ACTIVE.getName()) ? data.getBoolean(ActivityTypeFields.IS_ACTIVE.getName()) : null;
        final int start = Math.max(data.optInt(ActivityTypeFields.START.getName(), DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(data.optInt(ActivityTypeFields.LIMIT.getName(), DEFAULT_LIMIT), DEFAULT_LIMIT);
        final Long totalRecords = activityTypeManager.getActivityTypeCount(orgId, isActive);
        final Collection<ActivityType> activityTypes = totalRecords > 0 ? activityTypeManager.getTypeList(orgId, start, limit, isActive, null) : null;

        if (Objects.nonNull(activityTypes)) {
            activityTypes.stream()
                    .sorted(Comparator.comparing(ActivityType::getActive).reversed())
                    .forEach(activityType -> jsonArray.put(activityTypeConverter.convert(ConverterConfigBuilder.getInstance().build(request, false, null), activityType)));
        }
        response.put(ActivityTypeFields.TOTAL_RECORDS.getName(), totalRecords);
        response.put(ActivityTypeFields.HAS_MORE.getName(), (start + limit) <= totalRecords);

        return jsonArray;
    }

    /**
     * Updates the status of a specific activity type for a given organization.
     *
     * @param orgId    Represents the organization id
     * @param userId   Represents the user id
     * @param data     Represents the request data
     * @param response Represents the crm response
     */
    private void updateStatus(final Long orgId, final Long userId, final JsonObject data, final CrmResponse response) {
        final Long typeId = data.getLong(ActivityTypeFields.TYPE_ID.getName());
        final boolean isActive = data.getBoolean(ActivityTypeFields.IS_ACTIVE.getName());

        if (!activityTypeManager.exists(orgId, typeId)) {
            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_TYPE_NOT_FOUND, "i18n.activity.type.not.found");
        }

        if (!isActive && Objects.equals(DEACTIVATION_RESTRICTION_LIMIT, activityTypeManager.getActivityTypeCount(orgId, true))) {
            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_TYPE_DEACTIVATION_RESTRICTED, "i18n.activity.activityType.deActivate.restricted");
        }

        try {
            updateTypeStatus(orgId, userId, typeId, isActive);
            response.put(ActivityTypeFields.MESSAGE.getName(), response.resolveI18nString("i18n.activity.type.status.updated"));
        } catch (Exception exception) {
            sendError(response, null, ErrorCode.get(ActivityErrorCodes.UPDATE_ACTION_FAILED, "i18n.activity.type.update.action.failed"));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConfigManager<ActivityType> getConfigManager() {
        return activityTypeManager;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityType convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return activityTypeConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JsonObject convert(final CrmRequest request, final ActivityType activityType) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, null);

        return activityTypeConverter.convert(converterConfig, activityType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActivityType convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return activityTypeConverter.convert(converterConfig, jsonObject);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action, final Collection<ActivityType> activityTypes) {
        if (Objects.nonNull(activityTypes) && !activityTypes.isEmpty()) {

            switch (action) {
                case CREATE, UPDATE -> {
                    final boolean isCreateEvent = action.equals(Action.CREATE);

                    if (isCreateEvent && activityTypeManager.isMaxLimitReached(orgId)) {
                        throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_TYPE_LIMIT_REACHED, "i18n.activity.activityType.max.limit.reached");
                    }

                    for (final ActivityType activityType : activityTypes) {

                        if (activityTypeManager.exists(isCreateEvent, orgId, activityType)) {
                            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_TYPE_ALREADY_EXISTS, "i18n.activity.activityType.already.exists");
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * <p>
     * Changes the order of activity type
     * </p>
     *
     * @param orgId      Represents the organization id
     * @param jsonObject The JSON object containing the activity type IDs representing the new order
     */
    private void updateActivityTypesOrder(final Long orgId, final JsonObject jsonObject) {
        final Set<Long> activityTypeIds = new LinkedHashSet<>();
        final List<Object> idsList = new ArrayList<>(jsonObject.getJsonArray(ActivityTypeFields.IDS.getName()).toList());

        Collections.reverse(idsList);
        idsList.forEach(activityTypeId -> activityTypeIds.add((Long) activityTypeId));

        if (!activityTypeManager.exists(orgId, activityTypeIds)) {
            throw ErrorCode.get(ActivityErrorCodes.ACTIVITY_TYPE_NOT_FOUND, "i18n.activity.type.not.found");
        }
        activityTypeManager.updateActivityTypesOrder(orgId, activityTypeIds);
    }

    /**
     * <p>
     * Update the status of {@link ActivityType} for a given type ID.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param typeId   Represents the activity type id
     * @param isActive A flag which represents the type is active or not
     */
    private void updateTypeStatus(final Long orgId, final Long userId, final Long typeId, final boolean isActive) {
        final String calendarSyncConfig = activityTypeManager.getCalendarSyncConfig(orgId, userId);
        final String syncJson;

        if (Objects.nonNull(calendarSyncConfig)) {
            final JsonObject calendarSyncJson = Json.object(calendarSyncConfig);

            if (!isActive) {
                final String eventIdKey = ActivityTypeFields.CALENDAR_EVENT_ID.getName(); // Refers the activity type event id for two-way sync
                final JsonObject syncTypeJson = calendarSyncJson.getJsonObject(ActivityTypeFields.SYNC_TYPE.getName());
                final JsonArray activityTypeArray = calendarSyncJson.getJsonArray(ActivityTypeFields.ACTIVITY_TYPES.getName());

                if (syncTypeJson.containsKey(eventIdKey) && syncTypeJson.getInt(eventIdKey) == typeId) {
                    final ActivityType defaultActivityType = activityTypeManager.getActiveDefaultActivityType(orgId, typeId);

                    if (Objects.nonNull(defaultActivityType)) {
                        syncTypeJson.put(eventIdKey, defaultActivityType.id());
                    }
                }

                for (int index = 0; index < activityTypeArray.size(); index++) {
                    final Object id = activityTypeArray.get(index);

                    if (NumberUtils.parseNumber(id).longValue() == typeId) {
                        activityTypeArray.remove(index);
                        break;
                    }
                }
            }
            syncJson = calendarSyncJson.toString();
        } else {
            syncJson = null;
        }

        activityTypeManager.updateStatus(orgId, userId, typeId, isActive, syncJson);
    }
}