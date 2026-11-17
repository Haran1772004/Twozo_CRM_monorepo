package com.tlc.crm.notification.app.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.fields.AppNotificationAttributes;

import java.util.Objects;

/**
 * <p>
 * A model that holds the assignment notification settings
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class AssignmentNotificationSettings extends NotificationSettings {

    private boolean isSourceAssigned;
    private boolean isActivityAssigned;

    public AssignmentNotificationSettings() {
        isSourceAssigned = true;
        isActivityAssigned = true;
    }

    public AssignmentNotificationSettings(final String config) {
        final JsonObject jsonObject = Json.object(config);

        this.isSourceAssigned = jsonObject.getBoolean(AppNotificationAttributes.IS_SOURCE_ASSIGNED);
        this.isActivityAssigned = jsonObject.getBoolean(AppNotificationAttributes.IS_ACTIVITY_ASSIGNED);
    }

    public AssignmentNotificationSettings(final JsonObject jsonObject) {
        this.isSourceAssigned = jsonObject.optBoolean(AppNotificationAttributes.IS_SOURCE_ASSIGNED, false);
        this.isActivityAssigned = jsonObject.optBoolean(AppNotificationAttributes.IS_ACTIVITY_ASSIGNED, false);
    }

    public void setSourceAssigned(final boolean sourceAssigned) {
        this.isSourceAssigned = sourceAssigned;
    }

    public void setActivityAssigned(final boolean activityAssigned) {
        this.isActivityAssigned = activityAssigned;
    }

    public boolean isSourceAssigned() {
        return isSourceAssigned;
    }

    public boolean isActivityAssigned() {
        return isActivityAssigned;
    }

    /**
     * <p>
     * Converts the {@link AssignmentNotificationSettings} values into a JSON string representation.
     * </p>
     *
     * @return A string representing the notification assignment config in the specified format.
     */
    @Override
    public String getConfigAsJsonString() {

        if (Objects.isNull(jsonString) || jsonString.trim().isEmpty()) {
            jsonString = constructJsonString();
        }
        return jsonString;
    }

    /**
     * <p>
     * Converts the configuration properties into a JSON object
     * </p>
     *
     * @return A JSON representation of the configuration settings
     */
    @Override
    public JsonObject configAsJson() {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(AppNotificationAttributes.IS_SOURCE_ASSIGNED, isSourceAssigned);
        jsonObject.put(AppNotificationAttributes.IS_ACTIVITY_ASSIGNED, isActivityAssigned);

        return jsonObject;
    }

    /**
     * Constructs a JSON string representation of the assignment notification settings.
     *
     * @return  A string representing the assignment notification settings.
     */
    private String constructJsonString() {
        return String.format("{\"isSourceAssigned\":%b,\"isActivityAssigned\":%b}", isSourceAssigned, isActivityAssigned);
    }
}