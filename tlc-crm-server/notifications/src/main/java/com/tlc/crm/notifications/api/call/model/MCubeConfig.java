package com.tlc.crm.notifications.api.call.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notifications.fields.TelephoneFields;
import com.tlc.crm.notifications.marketplace.api.model.CrmMCubeUser;
import com.tlc.notification.service.telephony.mcube.MCubeService;

import java.net.URL;
import java.util.Locale;

/**
 * The {@link MCubeConfig} class represents the MCube telephony configuration details.
 * <p>
 * MCube uses a click-to-call model (no WebRTC), so WebRTC and follow-me features are disabled.
 * The configuration includes the MCube agent ID, secret key, and auth token.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class MCubeConfig extends TelephoneConfig {

    private final String id;
    private final String authToken;
    private final String phoneNumber;
    private final CrmMCubeUser.SyncStatus syncStatus;
    private final URL webhookURL;

    /**
     * Constructs the MCube configuration from a JSON object.
     *
     * @param configJson JSON containing the MCube configuration (id, authenticate block)
     */
    public MCubeConfig(final JsonObject configJson) {
        id = configJson.optString(TelephoneFields.ID, null);
        authToken = configJson.getJsonObject(TelephoneFields.AUTHENTICATE).getString("authToken");
        phoneNumber = configJson.optString(TelephoneFields.PHONE_NUMBER, null);
        syncStatus = configJson.containsKey(TelephoneFields.SYNC_STATUS)
                ? CrmMCubeUser.SyncStatus.getSyncStatusById(configJson.getInt(TelephoneFields.SYNC_STATUS)) : CrmMCubeUser.SyncStatus.FAILED;
        webhookURL = MCubeService.getInstance().getWebhookURL();
    }

    /**
     * Determines whether this configuration allows making calls.
     * A call can be made only if the MCube agent ID is configured.
     *
     * @return true if the agent ID is present
     */
    public boolean canMakeCall() {
        return syncStatus.isConnected();
    }

    /**
     * Builds the MCube telephony configuration JSON for the client-side response.
     * MCube does not support WebRTC or follow-me features.
     *
     * @param locale the locale for i18n display name resolution
     * @return the JSON configuration object
     */
    @Override
    public JsonObject getConfigJson(final Locale locale) {
        final JsonObject mCubeConfig = Json.object();

        mCubeConfig.put(TelephoneFields.NAME, getName());
        mCubeConfig.put(TelephoneFields.DISPLAY_NAME, i18nService.getResolver(locale).get(getDisplayName()));
        final JsonObject configJson = Json.object()
                .put(TelephoneFields.USER_ID, id)
                .put(TelephoneFields.WEBHOOK_URL, webhookURL.toString())
                .put(TelephoneFields.AUTH_TOKEN, authToken)
                .put(TelephoneFields.PHONE_NUMBER, phoneNumber)
                .put(TelephoneFields.IS_WEBRTC_ENABLED, false)
                .put(TelephoneFields.IS_FOLLOW_ME_ENABLED, false);

        mCubeConfig.put(TelephoneFields.CAN_MAKE_CALL, canMakeCall());
        mCubeConfig.put(TelephoneFields.CONFIG, configJson);

        return mCubeConfig;
    }
}
