package com.tlc.crm.common.converter;

import com.tlc.commons.json.JsonObject;
import com.tlc.crm.common.resolver.I18NCRMResolutionAdapter;
import com.tlc.i18n.I18nResolutionAdapter;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import java.util.Locale;
import java.util.Objects;

/**
 * Holds the configuration that are used to convert the entities
 *
 * @author Selvakumar
 * @version 1.0
 */
public class ConverterConfig {

    private Long orgId;
    private Long ownerId;
    private Locale locale;
    private boolean isPartialData;
    private JsonObject dataToBeConverted;
    private boolean isIdRequiredOnKey;
    private final boolean isMobileClient;
    private boolean isAccessConfigRequired;
    private String operatingSystem;
    private I18nResolutionAdapter i18NResolutionAdapter;
    private boolean areReadOnlyFieldsRequired;

    public ConverterConfig(final Long orgId, final Long ownerId, final Locale locale, final boolean isPartialData,
                           final JsonObject dataToBeConverted) {
        this(orgId, ownerId, locale, isPartialData, dataToBeConverted, false, false);
    }

    public ConverterConfig(final Long orgId, final Long ownerId, final Locale locale, final boolean isPartialData,
            final JsonObject dataToBeConverted, final boolean areReadOnlyFieldsRequired) {
        this(orgId, ownerId, locale, isPartialData, dataToBeConverted, false, false);
        this.areReadOnlyFieldsRequired = areReadOnlyFieldsRequired;
    }

    public ConverterConfig(final Long orgId, final Long ownerId, final Locale locale, final boolean isPartialData,
            final JsonObject dataToBeConverted, final String operatingSystem) {
        this(orgId, ownerId, locale, isPartialData, dataToBeConverted, false, false);
        this.operatingSystem = operatingSystem;
    }

    public ConverterConfig(final Long orgId, final Long ownerId, final Locale locale, final boolean isPartialData,
                           final JsonObject dataToBeConverted, final boolean isMobileClient, final boolean isAccessConfigRequired) {
        this.orgId = orgId;
        this.ownerId = ownerId;
        this.locale = locale;
        this.isPartialData = isPartialData;
        this.isIdRequiredOnKey = true;
        this.isMobileClient = isMobileClient;
        this.isAccessConfigRequired = isAccessConfigRequired;
        this.dataToBeConverted = dataToBeConverted;

        if (Objects.nonNull(locale)) {
            this.i18NResolutionAdapter = new I18NCRMResolutionAdapter(I18nServiceProvider.get().getResolver(locale), orgId);
        }
    }

    public ConverterConfig(final boolean isIdRequiredOnKey, final boolean isMobileClient) {
        this.isIdRequiredOnKey = isIdRequiredOnKey;
        this.isMobileClient = isMobileClient;
    }

    public Long orgId() {
        return orgId;
    }

    public Long ownerId() {
        return ownerId;
    }

    public Locale locale() {
        return locale;
    }

    public boolean isPartialData() {
        return isPartialData;
    }

    public JsonObject dataToBeConverted() {
        return dataToBeConverted;
    }

    public boolean isIdRequiredOnKey() {
        return isIdRequiredOnKey;
    }

    public void setPartialData(final boolean isPartialData) {
        this.isPartialData = isPartialData;
    }

    /**
     * Checks whether the client is mobile or not
     *
     * @return true if the client is mobile
     */
    public boolean isMobileClient() {
        return isMobileClient;
    }

    public boolean isAccessConfigRequired() {
        return isAccessConfigRequired;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public I18nResolutionAdapter getI18nPlaceholderResolver() {
        return i18NResolutionAdapter;
    }

    public boolean areReadOnlyFieldsRequired() {
        return areReadOnlyFieldsRequired;
    }
}
