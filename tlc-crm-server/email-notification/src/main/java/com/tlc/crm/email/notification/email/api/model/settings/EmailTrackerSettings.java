package com.tlc.crm.email.notification.email.api.model.settings;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.email.notification.email.fields.EmailFields;

import java.io.Serializable;

import java.util.Objects;

/**
 * <p>
 * An entity that holds the email tracking parameters.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public class EmailTrackerSettings implements Serializable {

    private boolean isEmailTrackerEnabled;
    private boolean isLinkTrackerEnabled;
    private String jsonString;

    public EmailTrackerSettings() {
    }

    public EmailTrackerSettings(final JsonObject jsonObject) {
        this.isEmailTrackerEnabled = jsonObject.getBoolean(EmailFields.IS_EMAIL_TRACKER_ENABLED.getName());
        this.isLinkTrackerEnabled = jsonObject.getBoolean(EmailFields.IS_LINK_TRACKER_ENABLED.getName());
    }

    public EmailTrackerSettings(final String emailTrackerSettings) {
        final JsonObject emailTrackerSettingsJson = Json.object(emailTrackerSettings);

        this.isEmailTrackerEnabled = emailTrackerSettingsJson.getBoolean(EmailFields.IS_EMAIL_TRACKER_ENABLED.getName());
        this.isLinkTrackerEnabled = emailTrackerSettingsJson.getBoolean(EmailFields.IS_LINK_TRACKER_ENABLED.getName());
    }

    public boolean isEmailTrackerEnabled() {
        return isEmailTrackerEnabled;
    }

    public boolean isLinkTrackerEnabled() {
        return isLinkTrackerEnabled;
    }

    public void setEmailTrackerEnabled(final boolean isEmailTrackerEnabled) {
        this.isEmailTrackerEnabled = isEmailTrackerEnabled;
    }

    public void setLinkTrackerEnabled(final boolean isLinkTrackerEnabled) {
        this.isLinkTrackerEnabled = isLinkTrackerEnabled;
    }

    /**
     * <p>
     * Converts the EmailTracker settings object's tracking settings into a JSON string representation.
     * </p>
     *
     * @return A string representing the email tracking configuration in the specified format.
     */
    public String getTrackingSettingsJson() {

        if (Objects.isNull(jsonString)) {
            jsonString = constructJsonString();
        }
        return jsonString;
    }

    /**
     * Constructs a JSON string representation of the email tracker settings object's tracking settings.
     *
     * @return A string representing the email tracking configuration in the specified format.
     */
    private String constructJsonString() {
        return String.format("{\"isLinkTrackerEnabled\":%b,\"isEmailTrackerEnabled\":%b}",
                isLinkTrackerEnabled, isEmailTrackerEnabled);
    }
}
