package com.tlc.crm.entity.currency.api.model.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmResponse;

import com.tlc.i18n.I18nResolver;
import com.tlc.i18n.service.provider.I18nServiceProvider;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.entity.currency.action.config.CurrencyConverterConfig;
import com.tlc.crm.entity.builder.EntityConverter;
import com.tlc.crm.entity.currency.fields.CurrencyUnitFields;
import com.tlc.crm.entity.fields.type.FieldSubTypeKeys;
import com.tlc.crm.entity.currency.api.manager.CurrencyUnitManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.currency.api.model.CurrencyUnit;

import java.math.BigDecimal;

import java.util.Locale;
import java.util.Objects;

/**
 * <p>
 * Converts and configures the {@link CurrencyUnit} and formatting the attributes.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EntityConverter
 */
public class CurrencyUnitConverter extends EntityConverter<CurrencyUnit> {

    private static final int BASE_VALUE = 1;

    private static class Instance {
        private static final CurrencyUnitConverter INSTANCE = new CurrencyUnitConverter();
    }

    private CurrencyUnitConverter() {
    }

    /**
     * <p>
     * Gets the instance of {@link CurrencyUnitConverter}.
     * </p>
     *
     * @return {@link CurrencyUnitConverter}
     */
    public static CurrencyUnitConverter getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param jsonObject      Represents the {@link JsonObject} request data
     * @return {@link CurrencyUnit}
     */
    @Override
    public CurrencyUnit convert(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        return converterConfig.isPartialData() ? buildCurrencyByPartialData(converterConfig, jsonObject)
                : buildCurrency(converterConfig, jsonObject);
    }

    /**
     * <p>
     * Converts the {@link CurrencyUnit} to {@link JsonObject}.
     * </p>
     *
     * @param currencyUnit Represents the {@link CurrencyUnit}
     * @param crmResponse  Represents the {@link CrmResponse}
     * @return {@link JsonObject} of currency
     */
    public JsonObject convertCurrencyUnitToJson(final CurrencyUnit currencyUnit, final CrmResponse crmResponse) {
        final JsonObject json = Json.object();
        final JsonObject currencyJson = Json.object();
        final Currency currency = currencyUnit.getCurrency();

        currencyJson.put(CurrencyUnitFields.ID, currency.getId());
        currencyJson.put(CurrencyUnitFields.CURRENCY_CODE, currency.getCode());
        currencyJson.put(CurrencyUnitFields.NAME, crmResponse.resolveI18nString(currency.getI18nKey()));

        json.put(CurrencyUnitFields.CURRENCY, currencyJson);
        json.put(CurrencyUnitFields.CONVERSION_TYPE, currencyUnit.getConversionType().id());
        json.put(CurrencyUnitFields.SYMBOL, currencyUnit.getSymbol());
        json.put(CurrencyUnitFields.IS_ENABLED, currencyUnit.isEnabled());
        json.put(CurrencyUnitFields.ORG_ID, currencyUnit.orgId());
        json.put(CurrencyUnitFields.ID, currencyUnit.id());

        return json;
    }

    /**
     * <p>
     * Retrieves the exchange rate represented by a {@link JsonObject} for the specified default and target {@link Currency}.
     * </p>
     *
     * @param exchangeRate    Represents the exchange rate for target {@link Currency}
     * @param defaultCurrency Represents the default {@link Currency}
     * @param targetCurrency  Represents the target {@link Currency}
     * @return {@link JsonObject} of conversion rate
     */
    public JsonObject getExchangeRateJson(final BigDecimal exchangeRate, final Currency defaultCurrency, final Currency targetCurrency) {
        final JsonObject exchangeRateJson = Json.object();
        final JsonObject baseCurrencyJson = Json.object();
        final JsonObject targetCurrencyJson = Json.object();

        baseCurrencyJson.put(CurrencyUnitFields.VALUE, BASE_VALUE);
        baseCurrencyJson.put(CurrencyUnitFields.CURRENCY_CODE, defaultCurrency.getCode());
        baseCurrencyJson.put(CurrencyUnitFields.SYMBOL, defaultCurrency.getSymbol());

        targetCurrencyJson.put(CurrencyUnitFields.VALUE, exchangeRate);
        targetCurrencyJson.put(CurrencyUnitFields.CURRENCY_CODE, targetCurrency.getCode());
        targetCurrencyJson.put(CurrencyUnitFields.SYMBOL, targetCurrency.getSymbol());

        exchangeRateJson.put(CurrencyUnitFields.BASE_CURRENCY, baseCurrencyJson);
        exchangeRateJson.put(CurrencyUnitFields.TARGET_CURRENCY, targetCurrencyJson);

        return exchangeRateJson;
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link CurrencyUnit}
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param jsonObject      Represents the {@link JsonObject} of currency fields data
     * @return the {@link CurrencyUnit}
     */
    private CurrencyUnit buildCurrency(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final CurrencyUnit currencyUnit = new CurrencyUnit();
        final String idKey = FieldSubTypeKeys.ID.getName();

        if (jsonObject.containsKey(idKey)) {
            currencyUnit.setId(jsonObject.getLong(idKey));
        }
        setDataToCurrency(currencyUnit, converterConfig, jsonObject);

        return currencyUnit;
    }

    /**
     * <p>
     * Sets the {@link JsonObject} into {@link CurrencyUnit}
     * </p>
     *
     * @param currencyUnit Represents the {@link CurrencyUnit} data
     * @param jsonObject   Represents the {@link JsonObject} of currency fields data
     */
    private void setDataToCurrency(final CurrencyUnit currencyUnit, final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final Long orgId = converterConfig.orgId();

        if (jsonObject.containsKey(CurrencyUnitFields.CURRENCY_ID)) {
            final Currency currency = Currency.getCurrencyById(jsonObject.getShort(CurrencyUnitFields.CURRENCY_ID));

            currencyUnit.setCurrency(currency);
            currencyUnit.setSymbol(currency.getSymbol());
        }
        currencyUnit.setOrgId(orgId);
        currencyUnit.setConversionType(CurrencyUnit.ConversionType.AUTO);
        currencyUnit.setEnabled(true);
    }

    /**
     * <p>
     * Converts {@link JsonObject} into {@link CurrencyUnit}.
     * </p>
     *
     * @param converterConfig Represents the {@link ConverterConfig}
     * @param jsonObject      Represents the {@link JsonObject} request data
     * @return the {@link CurrencyUnit}
     */
    private CurrencyUnit buildCurrencyByPartialData(final ConverterConfig converterConfig, final JsonObject jsonObject) {
        final CurrencyUnit currencyUnit = CurrencyUnitManager.getInstance()
                .get(converterConfig.orgId(), jsonObject.getLong(CurrencyUnitFields.ID));

        setDataToCurrency(currencyUnit, converterConfig, jsonObject);

        return currencyUnit;
    }

    /**
     * <p>
     * Converts {@link Currency} to {@link JsonObject}
     * </p>
     *
     * @param currencyConverterConfig A flag which represents that the currency is base or not
     * @param currency                Represents the {@link Currency}
     * @return {@link JsonObject} of {@link Currency}
     */
    public JsonObject convert(final CurrencyConverterConfig currencyConverterConfig, final Currency currency) {
        final JsonObject currencyJson = Json.object();
        final Locale locale = currencyConverterConfig.locale();

        currencyJson.put(FieldSubTypeKeys.ID.getName(), currency.getId());
        currencyJson.put(FieldSubTypeKeys.CODE.getName(), currency.getCode());
        currencyJson.put(FieldSubTypeKeys.SYMBOL.getName(), currency.getSymbol());

        if (Objects.nonNull(locale)) {
            final I18nResolver i18nResolver = I18nServiceProvider.get().getResolver(locale);

            currencyJson.put(FieldSubTypeKeys.NAME.getName(), i18nResolver.get(currency.getI18nKey()));
        }
        currencyJson.put(FieldSubTypeKeys.IS_BASE.getName(), currencyConverterConfig.isBaseCurrency());

        return currencyJson;
    }

    /**
     * <p>
     * Converts {@link Currency} to {@link JsonObject}
     * </p>
     *
     * @param currencyConverterConfig A flag which represents that the currency is base or not
     * @param currency                Represents the {@link Currency}
     * @return {@link JsonObject} of {@link Currency}
     */
    public JsonObject convertModelToJson(final CurrencyConverterConfig currencyConverterConfig, final Currency currency) {
        final JsonObject currencyJson = Json.object();

        currencyJson.put(FieldSubTypeKeys.ID.getName(), currency.getId());
        currencyJson.put(FieldSubTypeKeys.CODE.getName(), currency.getCode());
        currencyJson.put(FieldSubTypeKeys.SYMBOL.getName(), currency.getSymbol());
        currencyJson.put(FieldSubTypeKeys.IS_BASE.getName(), currencyConverterConfig.isBaseCurrency());

        return currencyJson;
    }
}

