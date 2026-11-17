package com.tlc.crm.notifications.api.call.model;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.notifications.fields.TelephoneFields;

import com.tlc.i18n.I18nKey;
import com.tlc.i18n.I18nService;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.notification.model.telephone.TelephoneProvider;

import java.util.Locale;
import java.util.Objects;

/**
 * The {@link TelephoneConfig} class represents the configuration details.
 *
 * @author kishore J
 * @version 1.0
 */
public class TelephoneConfig {

    /** The internal name identifier of the telephone provider. */
    private String name;

    /** The display name key used for i18n resolution. */
    private I18nKey displayName;

    /** The ID of the user associated with this telephone configuration. */
    private Long userId;

    /** The i18n service used to resolve localized display names. */
    protected final I18nService i18nService;

    /**
     * Default constructor that initializes the {@link I18nService} provider.
     */
    protected TelephoneConfig() {
        i18nService = I18nServiceProvider.get();
    }

    /**
     * Returns the internal name of the telephone provider.
     *
     * @return the provider name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the internal name of the telephone provider.
     *
     * @param name the provider name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Returns the i18n display name key of the telephone provider.
     *
     * @return the display name key
     */
    public I18nKey getDisplayName() {
        return displayName;
    }

    /**
     * Sets the i18n display name key of the telephone provider.
     *
     * @param displayName the display name key to set
     */
    public void setDisplayName(final I18nKey displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the user ID associated with this telephone configuration.
     *
     * @return the user ID
     */
    public Long getUserId() {
        return userId;
    }

    /**
     * Sets the user ID associated with this telephone configuration.
     *
     * @param userId the user ID to set
     */
    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    /**
     * <p>
     * Gets the config JSON of the provider
     * </p>
     *
     * @param locale Represents the locale
     * @return the config JSON
     */
    public JsonObject getConfigJson(final Locale locale) {
        final JsonObject providerConfig = Json.object();

        providerConfig.put(TelephoneFields.NAME, getName());
        providerConfig.put(TelephoneFields.DISPLAY_NAME, i18nService.getResolver(locale).get(getDisplayName()));
        providerConfig.put(TelephoneFields.CAN_MAKE_CALL, false);

        return providerConfig;
    }

    /**
     * <p>
     * Creates a {@link TelephoneConfig} instance based on the given {@link TelephoneProvider}
     * and its corresponding configuration JSON.
     * </p>
     * <p>
     * If {@code configJson} is {@code null}, a default {@link TelephoneConfig} is returned.
     * Otherwise, a provider-specific subclass is instantiated.
     * </p>
     *
     * @param telephoneProvider the telephone provider type
     * @param configJson        the JSON object containing provider-specific configuration;
     *                          may be {@code null}
     * @return a {@link TelephoneConfig} instance appropriate for the given provider
     */
    public static TelephoneConfig create(final TelephoneProvider telephoneProvider, final JsonObject configJson) {
        return Objects.isNull(configJson) ? new TelephoneConfig() : switch (telephoneProvider) {
            case TWILIO -> new TelephoneConfig();
            case TELE_CMI -> new TeleCMIConfig(configJson);
            case M_CUBE -> new MCubeConfig(configJson);
        };
    }
}
