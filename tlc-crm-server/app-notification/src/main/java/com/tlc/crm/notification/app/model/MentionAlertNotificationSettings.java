package com.tlc.crm.notification.app.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.fields.AppNotificationAttributes;

import java.util.Objects;

/**
 * <p>
 * A model that holds the mentions alert notification settings
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class MentionAlertNotificationSettings extends NotificationSettings {

    private final boolean isEntityEmailEnabled;
    private final boolean isActivityPrivateNoteEmailEnabled;
    private final boolean isEntityEnabled;
    private final boolean isActivityPrivateNoteEnabled;

    private Long userId;

    public MentionAlertNotificationSettings() {
        isEntityEmailEnabled = true;
        isActivityPrivateNoteEmailEnabled = true;
        isEntityEnabled = true;
        isActivityPrivateNoteEnabled = true;
    }

    public MentionAlertNotificationSettings(final String config) {
        final JsonObject configJson = Json.object(config);

        this.isEntityEmailEnabled = configJson.getBoolean(AppNotificationAttributes.IS_ENTITY_EMAIL_ENABLED);
        this.isActivityPrivateNoteEmailEnabled = configJson.getBoolean(AppNotificationAttributes.IS_ACTIVITY_PRIVATE_NOTE_EMAIL_ENABLED);
        this.isEntityEnabled = configJson.getBoolean(AppNotificationAttributes.IS_ENTITY_ENABLED);
        this.isActivityPrivateNoteEnabled = configJson.getBoolean(AppNotificationAttributes.IS_ACTIVITY_PRIVATE_NOTE_ENABLED);
    }

    public MentionAlertNotificationSettings(final JsonObject configJson) {
        this.isEntityEmailEnabled = configJson.optBoolean(AppNotificationAttributes.IS_ENTITY_EMAIL_ENABLED, false);
        this.isActivityPrivateNoteEmailEnabled = configJson.optBoolean(AppNotificationAttributes.IS_ACTIVITY_PRIVATE_NOTE_EMAIL_ENABLED, false);
        this.isEntityEnabled = configJson.optBoolean(AppNotificationAttributes.IS_ENTITY_ENABLED, false);
        this.isActivityPrivateNoteEnabled = configJson.optBoolean(AppNotificationAttributes.IS_ACTIVITY_PRIVATE_NOTE_ENABLED, false);
    }

    public boolean isEntityEmailEnabled() {
        return isEntityEmailEnabled;
    }

    public boolean isActivityPrivateNoteEmailEnabled() {
        return isActivityPrivateNoteEmailEnabled;
    }

    public boolean isEntityEnabled() {
        return isEntityEnabled;
    }

    public boolean isActivityPrivateNoteEnabled() {
        return isActivityPrivateNoteEnabled;
    }

    /**
     * {@inheritDoc}
     *
     * @return the config as json
     */
    @Override
    public JsonObject configAsJson() {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(AppNotificationAttributes.IS_ENTITY_EMAIL_ENABLED, isEntityEmailEnabled);
        jsonObject.put(AppNotificationAttributes.IS_ACTIVITY_PRIVATE_NOTE_EMAIL_ENABLED, isActivityPrivateNoteEmailEnabled);
        jsonObject.put(AppNotificationAttributes.IS_ENTITY_ENABLED, isEntityEnabled);
        jsonObject.put(AppNotificationAttributes.IS_ACTIVITY_PRIVATE_NOTE_ENABLED, isActivityPrivateNoteEnabled);

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @return the config as json string
     */
    @Override
    public String getConfigAsJsonString() {
        if (Objects.isNull(jsonString) || jsonString.trim().isEmpty()) {
            jsonString = constructJsonString();
        }
        return jsonString;
    }

    /**
     * Constructs a JSON string representation of the note mention notification settings.
     *
     * @return A string representing the note mention notification settings.
     */
    private String constructJsonString() {
        final JsonObject jsonObject = Json.object();
        jsonObject.put(AppNotificationAttributes.IS_ENTITY_EMAIL_ENABLED, isEntityEmailEnabled);
        jsonObject.put(AppNotificationAttributes.IS_ACTIVITY_PRIVATE_NOTE_EMAIL_ENABLED, isActivityPrivateNoteEmailEnabled);
        jsonObject.put(AppNotificationAttributes.IS_ENTITY_ENABLED, isEntityEnabled);
        jsonObject.put(AppNotificationAttributes.IS_ACTIVITY_PRIVATE_NOTE_ENABLED, isActivityPrivateNoteEnabled);

        return jsonObject.toString();
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }
}
