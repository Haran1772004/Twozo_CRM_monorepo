package com.tlc.crm.notification.app.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.fields.AppNotificationAttributes;

import java.util.Objects;

/**
 * <p>
 * A model that holds the notification daily summary configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class SummaryNotificationSettings extends NotificationSettings {

    private boolean isAssigned;
    private boolean isTransferred;

    public SummaryNotificationSettings() {
        isAssigned = true;
        isTransferred = true;
    }

    public SummaryNotificationSettings(final String config) {
        final JsonObject jsonObject = Json.object(config);

        this.isAssigned = jsonObject.getBoolean(AppNotificationAttributes.IS_ASSIGNED);
        this.isTransferred = jsonObject.getBoolean(AppNotificationAttributes.IS_TRANSFERRED);
    }

    public SummaryNotificationSettings(final JsonObject jsonObject) {
        this.isAssigned = jsonObject.optBoolean(AppNotificationAttributes.IS_ASSIGNED, false);
        this.isTransferred = jsonObject.optBoolean(AppNotificationAttributes.IS_TRANSFERRED, false);
    }

    public void setAssigned(final boolean assigned) {
        this.isAssigned = assigned;
    }

    public void setTransferred(final boolean transferred) {
        this.isTransferred = transferred;
    }

    public boolean isAssigned() {
        return isAssigned;
    }

    public boolean isTransferred() {
        return isTransferred;
    }

    /**
     * <p>
     * Converts the {@link SummaryNotificationSettings} values into a JSON string representation.
     * </p>
     *
     * @return A string representing the summary notification settings in the specified format.
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

        jsonObject.put(AppNotificationAttributes.IS_ASSIGNED, isAssigned);
        jsonObject.put(AppNotificationAttributes.IS_TRANSFERRED, isTransferred);

        return jsonObject;
    }

    /**
     * Constructs a JSON string representation of the notification summary config.
     *
     * @return  A string representing the notification summary config.
     */
    private String constructJsonString() {
        return String.format("{\"isAssigned\":%b,\"isTransferred\":%b}", isAssigned, isTransferred);
    }
}