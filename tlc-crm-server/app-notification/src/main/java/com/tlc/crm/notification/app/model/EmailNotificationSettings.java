package com.tlc.crm.notification.app.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.fields.AppNotificationAttributes;

import java.util.Objects;

/**
 * <p>
 * A model that holds the email notification settings
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class EmailNotificationSettings extends NotificationSettings {

    private boolean isActivityAssigned;
    private boolean isActivityCompleted;
    private boolean isModifiedActivity;
    private boolean isSourceAssigned;
    private boolean isSourceTransferred;

    public EmailNotificationSettings() {
        isActivityAssigned = true;
        isActivityCompleted = true;
        isModifiedActivity = true;
        isSourceAssigned = true;
        isSourceTransferred = true;
    }

    public EmailNotificationSettings(final String config) {
        final JsonObject jsonObject = Json.object(config);

        this.isActivityAssigned = jsonObject.getBoolean(AppNotificationAttributes.IS_ACTIVITY_ASSIGNED);
        this.isActivityCompleted = jsonObject.getBoolean(AppNotificationAttributes.IS_ACTIVITY_COMPLETED);
        this.isSourceAssigned = jsonObject.getBoolean(AppNotificationAttributes.IS_SOURCE_ASSIGNED);
        this.isModifiedActivity = jsonObject.getBoolean(AppNotificationAttributes.IS_MODIFIED_ACTIVITY);
        this.isSourceTransferred = jsonObject.getBoolean(AppNotificationAttributes.IS_SOURCE_TRANSFERRED);
    }

    public EmailNotificationSettings(final JsonObject jsonObject) {
        this.isActivityAssigned = jsonObject.optBoolean(AppNotificationAttributes.IS_ACTIVITY_ASSIGNED, false);
        this.isActivityCompleted = jsonObject.optBoolean(AppNotificationAttributes.IS_ACTIVITY_COMPLETED, false);
        this.isSourceAssigned = jsonObject.optBoolean(AppNotificationAttributes.IS_SOURCE_ASSIGNED, false);
        this.isModifiedActivity = jsonObject.optBoolean(AppNotificationAttributes.IS_MODIFIED_ACTIVITY, false);
        this.isSourceTransferred = jsonObject.optBoolean(AppNotificationAttributes.IS_SOURCE_TRANSFERRED, false);
    }

    public void setActivityAssigned(final boolean activityAssigned) {
        this.isActivityAssigned = activityAssigned;
    }

    public void setActivityCompleted(final boolean activityCompleted) {
        this.isActivityCompleted = activityCompleted;
    }

    public void setModifiedActivity(final boolean modifiedActivity) {
        this.isModifiedActivity = modifiedActivity;
    }

    public void setSourceAssigned(final boolean sourceAssigned) {
        this.isSourceAssigned = sourceAssigned;
    }

    public void setSourceTransferred(final boolean sourceTransferred) {
        this.isSourceTransferred = sourceTransferred;
    }

    public boolean isActivityAssigned() {
        return isActivityAssigned;
    }

    public boolean isActivityCompleted() {
        return isActivityCompleted;
    }

    public boolean isModifiedActivity() {
        return isModifiedActivity;
    }

    public boolean isSourceAssigned() {
        return isSourceAssigned;
    }

    public boolean isSourceTransferred() {
        return isSourceTransferred;
    }

    /**
     * <p>
     * Converts the {@link EmailNotificationSettings} values into a JSON string representation.
     * </p>
     *
     * @return A string representing the email notification settings in the specified format.
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

        jsonObject.put(AppNotificationAttributes.IS_ACTIVITY_ASSIGNED, isActivityAssigned);
        jsonObject.put(AppNotificationAttributes.IS_ACTIVITY_COMPLETED, isActivityCompleted);
        jsonObject.put(AppNotificationAttributes.IS_MODIFIED_ACTIVITY, isModifiedActivity);
        jsonObject.put(AppNotificationAttributes.IS_SOURCE_ASSIGNED, isSourceAssigned);
        jsonObject.put(AppNotificationAttributes.IS_SOURCE_TRANSFERRED, isSourceTransferred);

        return jsonObject;
    }

    /**
     * Constructs a JSON string representation of the notification email config.
     *
     * @return  A string representing the notification email config.
     */
    private String constructJsonString() {
        return String.format("{\"isActivityAssigned\":%b,\"isActivityCompleted\":%b,\"isSourceAssigned\":%b,\"isModifiedActivity\":%b,\"isSourceTransferred\":%b}",
                isActivityAssigned, isActivityCompleted, isSourceAssigned, isModifiedActivity, isSourceTransferred);
    }
}