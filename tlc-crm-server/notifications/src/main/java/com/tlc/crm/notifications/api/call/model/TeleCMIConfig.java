package com.tlc.crm.notifications.api.call.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.crm.notifications.fields.TelephoneFields;

import java.util.Locale;
import java.util.Objects;

/**
 * The {@link TeleCMIConfig} class represents the configuration details.
 *
 * @author kishore J
 * @version 1.0
 */
public class TeleCMIConfig extends TelephoneConfig {

    private final String id;
    private final String secret;
    private final String password;

    public TeleCMIConfig() {
        id = null;
        secret = null;
        password = null;
    }

    public TeleCMIConfig(final JsonObject configJson) {
        id = configJson.optString(TelephoneFields.ID, null);
        secret = configJson.getJsonObject(TelephoneFields.AUTHENTICATE).getString(TelephoneFields.SECRET);
        password = configJson.optString(TelephoneFields.PASSWORD, null);
    }

    public boolean canMakeCall() {
        return Objects.nonNull(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param locale Represents the locale
     * @return the config JSON
     */
    @Override
    public JsonObject getConfigJson(final Locale locale) {
        final JsonObject teleCMIConfig = Json.object();

        teleCMIConfig.put(TelephoneFields.NAME, getName());
        teleCMIConfig.put(TelephoneFields.DISPLAY_NAME, i18nService.getResolver(locale).get(getDisplayName()));
        final JsonObject configJson = Json.object()
                .put(TelephoneFields.USER_ID, id)
                .put(TelephoneFields.SECRET, secret)
                .put(TelephoneFields.PASSWORD, password)
                .put(TelephoneFields.IS_WEBRTC_ENABLED, false)
                .put(TelephoneFields.IS_FOLLOW_ME_ENABLED, true);

        teleCMIConfig.put(TelephoneFields.CAN_MAKE_CALL, canMakeCall());
        teleCMIConfig.put(TelephoneFields.CONFIG, configJson);

        return teleCMIConfig;
    }
}
