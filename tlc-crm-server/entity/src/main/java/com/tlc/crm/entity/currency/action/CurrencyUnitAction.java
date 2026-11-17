package com.tlc.crm.entity.currency.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.CrmConfigAction;
import com.tlc.crm.common.action.secure.AuthorizationController;
import com.tlc.crm.common.config.manager.ConfigManager;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.models.Action;

import com.tlc.exchange.rate.service.provider.ExchangeRateServiceProvider;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.service.ExchangeRateService;

import com.tlc.crm.entity.builder.ConverterConfigBuilder;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.currency.fields.CurrencyUnitFields;
import com.tlc.crm.entity.internal.status.CurrencyUnitErrorCodes;
import com.tlc.crm.entity.currency.api.manager.BaseCurrencyManager;
import com.tlc.crm.entity.currency.api.manager.CurrencyUnitManager;
import com.tlc.crm.common.converter.ConverterConfig;
import com.tlc.crm.entity.currency.api.model.BaseCurrency;
import com.tlc.crm.entity.currency.api.model.BaseCurrency.DecimalPoint;
import com.tlc.crm.entity.currency.api.model.CurrencyUnit;
import com.tlc.crm.entity.currency.api.model.converter.CurrencyUnitConverter;

import com.tlc.web.WebAction;

import java.math.BigDecimal;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Manages the actions related to currency settings, such as retrieval, update, and other web related actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AuthorizationController
 */
@WebAction(path = "/settings/currency/mgmt")
public class CurrencyUnitAction extends CrmConfigAction<CurrencyUnit> {

    private static final int DEFAULT_START_VALUE = 1;
    private static final int DEFAULT_LIMIT = 200;

    private final CurrencyUnitManager currencyUnitManager;
    private final BaseCurrencyManager baseCurrencyManager;
    private final ConverterConfigBuilder converterConfigBuilder;
    private final CurrencyUnitConverter currencyUnitConverter;
    private final BaseCurrencyService baseCurrencyService;
    private final ExchangeRateService exchangeRateService;

    public CurrencyUnitAction() {
        this.currencyUnitManager = CurrencyUnitManager.getInstance();
        this.baseCurrencyManager = BaseCurrencyManager.getInstance();
        this.converterConfigBuilder = ConverterConfigBuilder.getInstance();
        this.currencyUnitConverter = CurrencyUnitConverter.getInstance();
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.exchangeRateService = ExchangeRateServiceProvider.get();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param request  Represents the {@link CrmRequest} data
     * @param response Represents the {@link CrmResponse} data
     */
    @Override
    public void handle(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final Long orgId = request.orgId();
        final JsonObject data = requestJson.getJsonObject("data");
        final String type = requestJson.getString("type");

        switch (type) {
            case "updateBaseCurrency" -> updateBaseCurrency(orgId, data, response);
            case "updateStatus" -> updateStatus(orgId, data, response);
            case "list" -> getCurrencyList(orgId, data, response);
            case "getExchangeRate" -> getExchangeRate(orgId, data, response);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param crmResponse   Represents the {@link CrmResponse}
     * @param orgId         Represents an organization id
     * @param userId        Represents the user id
     * @param currencyUnits Represents the {@link Collection} of {@link CurrencyUnit}
     * @param eventData     Represents the {@link EventData}
     */
    @Override
    public void update(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<CurrencyUnit> currencyUnits, final EventData eventData) {
        throw ErrorCode.get(CurrencyUnitErrorCodes.UNSUPPORTED_ACTION, "i18n.currency.unit.unsupported.action");
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param crmResponse   Represents the {@link CrmResponse}
     * @param orgId         Represents an organization id
     * @param userId        Represents the user id
     * @param currencyUnits Represents the {@link Collection} of {@link CurrencyUnit}
     * @param eventData      Represents the {@link EventData}
     */
    @Override
    public void delete(final CrmResponse crmResponse, final Long orgId, final Long userId, final Collection<CurrencyUnit> currencyUnits, EventData eventData) {
        throw ErrorCode.get(CurrencyUnitErrorCodes.UNSUPPORTED_ACTION, "i18n.currency.unit.unsupported.action");
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the currency unit id
     * @return
     */
    @Override
    public CurrencyUnit get(final Long orgId, final Long id) {
        throw ErrorCode.get(CurrencyUnitErrorCodes.UNSUPPORTED_ACTION, "i18n.currency.unit.unsupported.action");
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link CurrencyUnit}
     */
    @Override
    public ConfigManager<CurrencyUnit> getConfigManager() {
        return currencyUnitManager;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param request    Represents the {@link CrmRequest} data
     * @param jsonObject Represents the {@link JsonObject} request data
     * @return {@link CurrencyUnit}
     */
    @Override
    public CurrencyUnit convert(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, false, jsonObject);

        return currencyUnitConverter.convert(converterConfig, jsonObject);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param request    Represents the {@link CrmRequest} data
     * @param jsonObject Represents the {@link JsonObject} request data
     * @return {@link CurrencyUnit}
     */
    @Override
    public CurrencyUnit convertPartialData(final CrmRequest request, final JsonObject jsonObject) {
        final ConverterConfig converterConfig = converterConfigBuilder.build(request, true, jsonObject);

        return currencyUnitConverter.convert(converterConfig, jsonObject);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId         Represents an organization id
     * @param action        Which contains the {@link Action} related data
     * @param currencyUnits Represents the {@link Collection} of {@link CurrencyUnit}
     * @return true, means is valid details
     */
    @Override
    protected final boolean isValidDetails(final Long orgId, final Long userId, final Action action,
                                           final Collection<CurrencyUnit> currencyUnits) {
        if (!currencyUnits.isEmpty()) {

            switch (action) {
                case CREATE, UPDATE -> {
                    final boolean isCurrencyExists = currencyUnitManager.isCurrencyExists(orgId, currencyUnits.stream()
                            .map(settings -> (long) settings.getCurrency().getId()).collect(Collectors.toUnmodifiableSet()));

                    if (isCurrencyExists) {
                        throw ErrorCode.get(CurrencyUnitErrorCodes.CURRENCY_ID_ALREADY_EXISTS, "i18n.currency.unit.id.already.exits");
                    }
                    final Collection<String> currencyCodes = exchangeRateService.getSupportedCurrencyCodes();

                    for (final CurrencyUnit currencyUnit : currencyUnits) {

                        if (!currencyCodes.contains(currencyUnit.getCurrency().getCode())) {
                            throw ErrorCode.get(CurrencyUnitErrorCodes.UNSUPPORTED_CURRENCY, "i18n.currency.unsupported");
                        }
                    }
                }
            }

            return true;
        }

        return false;
    }

    /**
     * <p>
     * Retrieves the exchange rate between two currencies, specifically obtaining
     * the rate from {@code targetCurrency} to {@code baseCurrency}.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param data        Represents the {@link JsonObject} request data
     * @param crmResponse Represents the {@link CrmResponse} data
     */
    private void getExchangeRate(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final Currency targetCurrency = Currency.getCurrencyById(data.getShort(CurrencyUnitFields.ID));
        final Currency baseCurrency = baseCurrencyService.getDefaultCurrency(orgId);
        final BigDecimal exchangeRate = baseCurrencyService.getExchangeRate(baseCurrency, targetCurrency);

        crmResponse.put(CurrencyUnitFields.CURRENCY,
                currencyUnitConverter.getExchangeRateJson(exchangeRate, baseCurrency, targetCurrency));
    }

    /**
     * <p>
     * Modifies the {@link BaseCurrency} by updating details such as the decimal point or currency based on the provided information.
     * </p>
     *
     * @param orgId       Represents an organization
     * @param data        Represents the {@link JsonObject} request data
     * @param crmResponse Represents the {@link CrmResponse} data
     */
    private void updateBaseCurrency(final long orgId, final JsonObject data, final CrmResponse crmResponse) {
        final short id;
        final boolean checkDecimalPointUpdate;

        if (data.containsKey(CurrencyUnitFields.CURRENCY_ID)) {

            if (baseCurrencyManager.isImmutable(orgId)) {
                throw ErrorCode.get(CurrencyUnitErrorCodes.BASE_CURRENCY_UPDATE_RESTRICTED, "i18n.base.currency.update.restricted");
            } else {
                final Currency currency = Currency.getCurrencyById(data.getShort(CurrencyUnitFields.CURRENCY_ID));

                id = currency.getId();
                checkDecimalPointUpdate = false;

                if (currencyUnitManager.isCurrencyInPriceTag(orgId, baseCurrencyService.getBaseCurrency(orgId).getDefaultCurrency())) {
                    throw ErrorCode.get(CurrencyUnitErrorCodes.BASE_CURRENCY_DEACTIVATION_RESTRICTED, "i18n.base.currency.update.restricted.associated.with.product.price");
                }

                if (!exchangeRateService.getSupportedCurrencyCodes().contains(currency.getCode())) {
                    throw ErrorCode.get(CurrencyUnitErrorCodes.UNSUPPORTED_CURRENCY, "i18n.currency.unsupported");
                }

                if (baseCurrencyManager.currencyExists(orgId, id)) {
                    throw ErrorCode.get(CurrencyUnitErrorCodes.CURRENCY_ID_ALREADY_EXISTS, "i18n.currency.unit.id.already.exits");
                }
            }
        } else {
            final DecimalPoint decimalPoint = DecimalPoint.get(data.getShort(CurrencyUnitFields.DECIMAL_POINT_ID));

            id = decimalPoint.id();
            checkDecimalPointUpdate = true;
        }

        if (baseCurrencyManager.exists(orgId)) {
            baseCurrencyManager.updateBaseCurrencyDetails(orgId, id, checkDecimalPointUpdate);
            crmResponse.put(CurrencyUnitFields.MESSAGE, crmResponse.resolveI18nString("i18n.base.currency.update.success"));
        } else {
            throw ErrorCode.get(CurrencyUnitErrorCodes.BASE_CURRENCY_NOT_FOUND, "i18n.base.currency.invalid");
        }
    }

    /**
     * <p>
     * Updates the status for the {@link CurrencyUnit} for the given currency id.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param data     Represents the {@link JsonObject} request data
     * @param response Represents the {@link CrmResponse} data
     */
    private void updateStatus(final long orgId, final JsonObject data, final CrmResponse response) {
        final long id = data.getLong(CurrencyUnitFields.ID);

        if (currencyUnitManager.exists(orgId, Set.of(id))) {
            final boolean isEnabled = data.optBoolean(CurrencyUnitFields.IS_ENABLED);
            final CurrencyUnit currencyUnit = currencyUnitManager.get(orgId, id);

            if (!isEnabled && currencyUnitManager.isCurrencyInPriceTag(orgId, Currency.getCurrencyById(currencyUnit.getCurrency().getId()))) {
                throw ErrorCode.get(CurrencyUnitErrorCodes.CURRENCY_DEACTIVATION_RESTRICTED, "i18n.currency.deactivation.restricted.associated.with.product.price");
            }
            currencyUnitManager.updateStatus(orgId, id, isEnabled);
            response.put(CurrencyUnitFields.MESSAGE, response.resolveI18nString("i18n.currency.unit.update.success"));
        } else {
            throw ErrorCode.get(CurrencyUnitErrorCodes.CURRENCY_NOT_FOUND, "i18n.currency.unit.not.found");
        }
    }

    /**
     * <p>
     * Gets the {@link CurrencyUnit} for the given organization.
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param jsonObject  Represents the {@link JsonObject} request
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void getCurrencyList(final long orgId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        final int start = Math.max(jsonObject.optInt(CurrencyUnitFields.START, DEFAULT_START_VALUE), DEFAULT_START_VALUE);
        final int limit = Math.min(jsonObject.optInt(CurrencyUnitFields.LIMIT, DEFAULT_LIMIT), DEFAULT_LIMIT);
        final BaseCurrency baseCurrency = baseCurrencyManager.getBaseCurrency(orgId, start, limit);
        final Collection<CurrencyUnit> currencyUnits = baseCurrency.getCurrencyUnits();
        final DecimalPoint decimalPoint = baseCurrency.getDecimalPoint();
        final Currency defaultCurrency = baseCurrency.getDefaultCurrency();
        final long totalRecords = currencyUnitManager.getCurrencyCount(orgId);
        final JsonObject json = Json.object();

        if (totalRecords > 0 && (Objects.nonNull(currencyUnits) && !currencyUnits.isEmpty())
                && currencyUnitManager.exists(orgId, currencyUnits.stream()
                .map(CurrencyUnit::id).collect(Collectors.toUnmodifiableSet()))) {
            final JsonArray currencyArray = Json.array();
            final String exchangeRateKey = CurrencyUnitFields.EXCHANGE_RATE;

            for (final CurrencyUnit currencyUnit : currencyUnits) {
                final JsonObject currencyJson = currencyUnitConverter.convertCurrencyUnitToJson(currencyUnit, crmResponse);
                final Currency currency = currencyUnit.getCurrency();
                final BigDecimal exchangeRate = baseCurrencyService.getCurrencyExchangeRate(orgId, currency);

                currencyJson.put(exchangeRateKey, currencyUnitConverter.getExchangeRateJson(exchangeRate, defaultCurrency, currency));
                currencyArray.put(currencyJson);
            }
            json.put(CurrencyUnitFields.LIST, currencyArray);
            json.put(CurrencyUnitFields.TOTAL_RECORDS, totalRecords);
            json.put(CurrencyUnitFields.HAS_MORE, (start + limit) <= totalRecords);

            crmResponse.put(CurrencyUnitFields.CURRENCY, json);
        }
        final JsonObject baseCurrencyJson = Json.object();
        final JsonObject decimalPointJson = Json.object();

        baseCurrencyJson.put(CurrencyUnitFields.ID, defaultCurrency.getId());
        baseCurrencyJson.put(CurrencyUnitFields.NAME, crmResponse.resolveI18nString(defaultCurrency.getI18nKey()));
        baseCurrencyJson.put(CurrencyUnitFields.CURRENCY_CODE, defaultCurrency.getCode());

        decimalPointJson.put(CurrencyUnitFields.ID, decimalPoint.id());
        decimalPointJson.put(CurrencyUnitFields.VALUE, decimalPoint.getPoints());

        crmResponse.put(CurrencyUnitFields.BASE_CURRENCY, baseCurrencyJson);
        crmResponse.put(CurrencyUnitFields.DECIMAL_POINT, decimalPointJson);
        crmResponse.put(CurrencyUnitFields.ORG_ID, baseCurrency.orgId());
        crmResponse.put(CurrencyUnitFields.ID, baseCurrency.id());
        crmResponse.put(CurrencyUnitFields.IMMUTABLE, baseCurrency.isImmutable());
    }
}
