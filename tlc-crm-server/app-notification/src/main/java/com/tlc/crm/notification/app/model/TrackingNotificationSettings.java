package com.tlc.crm.notification.app.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.fields.AppNotificationAttributes;

import java.util.Objects;

/**
 * <p>
 * A model that holds the notification tracking configuration
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
public class TrackingNotificationSettings extends NotificationSettings {

    private boolean isEmailOpened;
    private boolean isEmailLinkClicked;
    private boolean isRepliedEmail;
    private boolean isTeamInboxEmail;

    public TrackingNotificationSettings() {
        isEmailOpened = true;
        isEmailLinkClicked = true;
        isRepliedEmail = true;
        isTeamInboxEmail = true;
    }

    public TrackingNotificationSettings(final String config) {
        final JsonObject jsonObject = Json.object(config);

        this.isEmailOpened = jsonObject.getBoolean(AppNotificationAttributes.IS_EMAIL_OPENED);
        this.isEmailLinkClicked = jsonObject.getBoolean(AppNotificationAttributes.IS_EMAIL_LINK_CLICKED);
        this.isRepliedEmail = jsonObject.getBoolean(AppNotificationAttributes.IS_REPLIED_EMAIL);
        this.isTeamInboxEmail = jsonObject.getBoolean(AppNotificationAttributes.IS_TEAM_INBOX_EMAIL);
    }

    public TrackingNotificationSettings(final JsonObject jsonObject) {
        this.isEmailOpened = jsonObject.optBoolean(AppNotificationAttributes.IS_EMAIL_OPENED, false);
        this.isEmailLinkClicked = jsonObject.optBoolean(AppNotificationAttributes.IS_EMAIL_LINK_CLICKED, false);
        this.isRepliedEmail = jsonObject.optBoolean(AppNotificationAttributes.IS_REPLIED_EMAIL, false);
        this.isTeamInboxEmail = jsonObject.optBoolean(AppNotificationAttributes.IS_TEAM_INBOX_EMAIL, false);
    }

    public void setEmailOpened(final boolean emailOpened) {
        this.isEmailOpened = emailOpened;
    }

    public void setEmailLinkClicked(final boolean emailLinkClicked) {
        this.isEmailLinkClicked = emailLinkClicked;
    }

    public void setRepliedEmail(final boolean repliedEmail) {
        this.isRepliedEmail = repliedEmail;
    }

    public void setTeamInboxEmail(final boolean teamInboxEmail) {
        this.isTeamInboxEmail = teamInboxEmail;
    }

    public boolean isEmailOpened() {
        return isEmailOpened;
    }

    public boolean isEmailLinkClicked() {
        return isEmailLinkClicked;
    }

    public boolean isRepliedEmail() {
        return isRepliedEmail;
    }

    public boolean isTeamInboxEmail() {
        return isTeamInboxEmail;
    }

    /**
     * <p>
     * Converts the {@link TrackingNotificationSettings} values into a JSON string representation.
     * </p>
     *
     * @return A string representing the tracking notification settings in the specified format.
     */
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

        jsonObject.put(AppNotificationAttributes.IS_EMAIL_OPENED, isEmailOpened);
        jsonObject.put(AppNotificationAttributes.IS_EMAIL_LINK_CLICKED, isEmailLinkClicked);
        jsonObject.put(AppNotificationAttributes.IS_REPLIED_EMAIL, isRepliedEmail);
        jsonObject.put(AppNotificationAttributes.IS_TEAM_INBOX_EMAIL, isTeamInboxEmail);

        return jsonObject;
    }

    /**
     * Constructs a JSON string representation of the notification tracking config.
     *
     * @return  A string representing the notification tracking config.
     */
    private String constructJsonString() {
        return String.format("{\"isEmailOpened\":%b,\"isEmailLinkClicked\":%b,\"isRepliedEmail\":%b,\"isTeamInboxEmail\":%b}",
                isEmailOpened, isEmailLinkClicked, isRepliedEmail, isTeamInboxEmail);
    }
}