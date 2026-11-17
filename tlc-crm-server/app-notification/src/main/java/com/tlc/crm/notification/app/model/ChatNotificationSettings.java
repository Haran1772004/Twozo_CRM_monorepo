package com.tlc.crm.notification.app.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notification.app.fields.AppNotificationAttributes;

/**
 * A model which holds the details of chat notification settings
 *
 * @author Maharaja S
 * @version 1.0
 */
public class ChatNotificationSettings extends NotificationSettings {

    private final boolean isChatAssigned;
    private final boolean isChatInitiated;

    public ChatNotificationSettings() {
        isChatAssigned = true;
        isChatInitiated = true;
    }

    public ChatNotificationSettings(final String config) {
        final JsonObject configJson = Json.object(config);

        isChatAssigned = configJson.getBoolean(AppNotificationAttributes.IS_CHAT_ASSIGNED);
        isChatInitiated = configJson.getBoolean(AppNotificationAttributes.IS_CHAT_INITIATED);
    }

    public ChatNotificationSettings(final JsonObject config) {
        isChatAssigned = config.getBoolean(AppNotificationAttributes.IS_CHAT_ASSIGNED);
        isChatInitiated = config.getBoolean(AppNotificationAttributes.IS_CHAT_INITIATED);
    }

    public boolean isChatAssigned() {
        return isChatAssigned;
    }

    public boolean isChatInitiated() {
        return isChatInitiated;
    }

    /**
     * {@inheritDoc}
     *
     * @return the config json
     */
    @Override
    public JsonObject configAsJson() {
        final JsonObject jsonObject = Json.object();

        jsonObject.put(AppNotificationAttributes.IS_CHAT_ASSIGNED, isChatAssigned);
        jsonObject.put(AppNotificationAttributes.IS_CHAT_INITIATED, isChatInitiated);

        return jsonObject;
    }

    /**
     * {@inheritDoc}
     *
     * @return the config as json string
     */
    @Override
    public String getConfigAsJsonString() {
        return String.format("{\"isChatAssigned\":%b,\"isChatInitiated\":%b}", isChatAssigned, isChatInitiated);
    }
}
