package com.tlc.crm.entity.currency.action.config;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.converter.ConverterConfig;

import java.util.Locale;

/**
 * <p>
 * Handles currency conversion tasks, ensuring the process adheres to the rules
 * and configurations provided by the base class, {@link ConverterConfig}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ConverterConfig
 */
public final class CurrencyConverterConfig extends ConverterConfig {

    private final boolean isBaseCurrency;

    public CurrencyConverterConfig(final Long orgId, final Long ownerId, final Locale locale, final boolean isPartialData,
                                   final JsonObject dataToBeConverted, final boolean isBaseCurrency) {
        super(orgId, ownerId, locale, isPartialData, dataToBeConverted);
        this.isBaseCurrency = isBaseCurrency;
    }

    public CurrencyConverterConfig(final Long orgId, final Long ownerId, final Locale locale, final boolean isBaseCurrency) {
        super(orgId, ownerId, locale, false, Json.object());
        this.isBaseCurrency = isBaseCurrency;
    }

    public CurrencyConverterConfig(final Locale locale, final boolean isBaseCurrency) {
        super(null, null, locale, false, Json.object());
        this.isBaseCurrency = isBaseCurrency;
    }

    public CurrencyConverterConfig(final boolean isBaseCurrency) {
        super(null, null, null, false, Json.object());
        this.isBaseCurrency = isBaseCurrency;
    }

    /**
     * <p>
     * Represents the currency is base or not.
     * </p>
     *
     * @return true, if the currency is base; otherwise fasle
     */
    public boolean isBaseCurrency() {
        return isBaseCurrency;
    }
}
