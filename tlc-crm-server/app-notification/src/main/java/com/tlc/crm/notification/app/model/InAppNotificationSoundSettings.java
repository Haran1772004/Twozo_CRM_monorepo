package com.tlc.crm.notification.app.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.fields.AppNotificationAttributes;

import java.util.Objects;

/**
 * A model which holds the details of chat notification settings
 *
 * @author Saravanan
 * @version 1.0
 */
public class InAppNotificationSoundSettings extends NotificationSettings {

    private final boolean isEnabled;
    private JsonObject configJson;

    public InAppNotificationSoundSettings() {
        isEnabled = false;
    }

    public InAppNotificationSoundSettings(final String config) {
        final JsonObject configJson = Json.object(config);

        isEnabled = configJson.getBoolean(AppNotificationAttributes.IS_ENABLED);
    }

    public InAppNotificationSoundSettings(final JsonObject config) {
        isEnabled = config.getBoolean(AppNotificationAttributes.IS_ENABLED);
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * {@inheritDoc}
     *
     * @return the config JSON
     */
    @Override
    public JsonObject configAsJson() {
        if (Objects.isNull(configJson)) {
            configJson = Json.object();
            configJson.put(AppNotificationAttributes.IS_ENABLED, isEnabled);
        }

        return configJson;
    }

    /**
     * {@inheritDoc}
     *
     * @return the config as JSON string
     */
    @Override
    public String getConfigAsJsonString() {
        return configAsJson().toString();
    }
}
