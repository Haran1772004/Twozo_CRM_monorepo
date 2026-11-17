package com.tlc.crm.entity.features.api.activity.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.features.api.activity.manager.ActivityTypeManager;
import com.tlc.crm.entity.features.api.activity.model.ActivityType;
import com.tlc.crm.entity.features.field.activity.ActivityTypeFields;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.models.type.converter.IconConverter;
import com.tlc.crm.icon.service.IconService;
import com.tlc.crm.icon.model.Icon;

import java.util.Objects;

/**
 * <p>
 * Provides different ways to convert the data from / into {@link ActivityType}
 * </p>
 *
 * @author Kowsalya SP
 * @version 1.0
 * @see EntityConverter
 */
public class ActivityTypeConverter extends EntityConverter<ActivityType> {
    private final ActivityTypeManager activityTypeManager;
    private final IconService iconService;
    private final IconConverter iconConverter;

    private static class Instance {
        private static final ActivityTypeConverter INSTANCE = new ActivityTypeConverter();
    }

    private ActivityTypeConverter() {
        activityTypeManager = ActivityTypeManager.getInstance();
        iconService = IconService.getInstance();
        iconConverter = IconConverter.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ActivityTypeConverter} Instance
     * </p>
     *
     * @return {@link ActivityTypeConverter}
     */
    public static ActivityTypeConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link ActivityType}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Which contains the request data
     * @return {@link ActivityType} activityType entity
     */
    @Override
    public ActivityType convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        if (converterConfig.isPartialData()) {
            return buildActivityTypeByPartialData(converterConfig, jsonObject);
        } else {
            return buildActivityType(converterConfig, jsonObject);
        }
    }

    /**
     * <p>
     * Converts the {@link JsonObject} into {@link ActivityType}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      Which contains the request data
     * @return {@link ActivityType} activityType entity
     */
    private ActivityType buildActivityType(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final ActivityType activityType = new ActivityType();
        final String idKey = ActivityTypeFields.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            activityType.setId(jsonObject.getLong(idKey));
        }
        setDataToActivityType(converterConfig, activityType, jsonObject);
        return activityType;
    }

    /**
     * <p>
     * Sets the json data into the {@link ActivityType}
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param activityType    An entity that holds the activityType attributes
     * @param jsonObject      which contains the data
     */
    private void setDataToActivityType(final ConverterConfig converterConfig, final ActivityType activityType, final JsonObject jsonObject) {
        final String iconIdKey = ActivityTypeFields.ICON_ID.getName();

        activityType.setOrgId(converterConfig.orgId());
        activityType.setOwnerId(converterConfig.ownerId());
        activityType.setName(jsonObject.optString(ActivityTypeFields.NAME.getName(), activityType.getName()));

        if (jsonObject.containsKey(iconIdKey)) {
            final Long iconId = jsonObject.getLong(iconIdKey);

            activityType.setActivityIcon(iconService.get(iconId));
        }
    }

    /**
     * <p>
     * Converts the partial json into the ActivityType
     * </p>
     *
     * @param converterConfig Represents the converter configuration
     * @param jsonObject      which contains the partial activityType data
     * @return {@link ActivityType} which is filled with the activityType data
     */
    private ActivityType buildActivityTypeByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final ActivityType activityType = activityTypeManager.get(converterConfig.orgId(),
                jsonObject.optLong(ActivityTypeFields.ID.getName(), 0));

        setDataToActivityType(converterConfig, activityType, jsonObject);
        return activityType;
    }

    /**
     * <p>
     * Converts the {@link ActivityType} into the {@link JsonObject}
     * </p>
     *
     * @param config       Represents the converter configuration
     * @param activityType An entity that holds the activity type attributes
     * @return the {@link JsonObject} filled with data
     */
    @Override
    public JsonObject convert(final ConverterConfig config, final ActivityType activityType) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(ActivityTypeFields.ID.getName(), activityType.id());
        jsonObject.put(ActivityTypeFields.ORG_ID.getName(), activityType.orgId());
        jsonObject.put(ActivityTypeFields.NAME.getName(), activityType.getName());
        jsonObject.put(ActivityTypeFields.IS_ACTIVE.getName(), activityType.getActive());

        final Icon activityIcon = activityType.getActivityIcon();

        if (Objects.nonNull(activityIcon)) {
            jsonObject.put(ActivityTypeFields.ICON.getName(), iconConverter.convert(config, activityIcon));
        }

        return jsonObject;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param activityType Represents the activity type
     * @return {@link ActivityType} of {@link JsonObject}
     */
    @Override
    public JsonObject convertPartialData(final ActivityType activityType) {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(ActivityTypeFields.ID.getName(), activityType.id());
        jsonObject.put(ActivityTypeFields.NAME.getName(), activityType.getName());

        return jsonObject;
    }
}
