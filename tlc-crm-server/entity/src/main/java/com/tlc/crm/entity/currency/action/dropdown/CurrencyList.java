package com.tlc.crm.entity.currency.action.dropdown;

import com.tlc.crm.entity.currency.action.config.CurrencyConverterConfig;

import com.tlc.crm.entity.dropdown.AbstractDropdownAction;
import com.tlc.exchange.rate.service.provider.ExchangeRateServiceProvider;

import com.tlc.i18n.I18nResolver;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.crm.entity.currency.api.model.BaseCurrency;
import com.tlc.crm.entity.currency.api.model.CurrencyUnit;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.currency.api.model.converter.CurrencyUnitConverter;
import com.tlc.crm.entity.currency.fields.CurrencyUnitFields;
import com.tlc.crm.entity.dropdown.DropdownAction;
import com.tlc.crm.entity.dropdown.DropdownLimit;
import com.tlc.crm.entity.dropdown.DropdownRequest;
import com.tlc.crm.entity.dropdown.OptionContainer;
import com.tlc.crm.entity.dropdown.annotation.StaticDropdown;

import com.tlc.exchange.rate.service.ExchangeRateService;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 * Provides functionality related to managing {@link Currency} related dropdown actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see DropdownAction
 */
@StaticDropdown(name = "currencyList")
public class CurrencyList extends AbstractDropdownAction {

    private final BaseCurrencyService baseCurrencyService;
    private final CurrencyUnitConverter currencyUnitConverter;
    private final ExchangeRateService exchangeRateService;

    public CurrencyList() {
        this.baseCurrencyService = BaseCurrencyService.getInstance();
        this.currencyUnitConverter = CurrencyUnitConverter.getInstance();
        this.exchangeRateService = ExchangeRateServiceProvider.get();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param dropdownRequest Holds the details needed for the dropdown request.
     * @return {@link OptionContainer} that holds the requested dropdown values.
     */
    @Override
    public OptionContainer fetchData(final DropdownRequest dropdownRequest) {
        final Long orgId = dropdownRequest.getOrgId();
        final DropdownLimit dropdownLimit = dropdownRequest.getLimit();
        final I18nResolver i18nResolver = dropdownRequest.i18nResolver();
        final Collection<Currency> supportedCurrencies = new ArrayList<>(exchangeRateService.getSupportedCurrencies());
        final BaseCurrency baseCurrency = baseCurrencyService.getBaseCurrency(orgId);
        final Collection<Currency> orgBasedCurrencies = new ArrayList<>(baseCurrency.getCurrencyUnits().stream()
                .filter(CurrencyUnit::isEnabled).map(CurrencyUnit::getCurrency).toList());
        final Currency defaultCurrency = baseCurrency.getDefaultCurrency();
        final String baseCurrencyCode = defaultCurrency.getCode();

        orgBasedCurrencies.add(defaultCurrency);
        supportedCurrencies.removeAll(orgBasedCurrencies);
        final Collection<Currency> currencies = applyCriteria(supportedCurrencies, dropdownRequest.getCriteria(),
                List.of(currency -> i18nResolver.get(currency.getI18nKey()), Currency::getCode), Optional.empty());
        final int start = dropdownLimit.start();
        final long totalRecords = currencies.size();
        final OptionContainer optionContainer = new OptionContainer(totalRecords);

        if (totalRecords > 0 && start <= totalRecords) {

            for (final Currency currency : currencies) {
                final BigDecimal exchangeRate = baseCurrencyService.getExchangeRate(defaultCurrency, currency);
                final JsonObject jsonObject = Json.object();
                final boolean isBaseCurrency = baseCurrencyCode.contains(currency.getCode());

                jsonObject.put(CurrencyUnitFields.EXCHANGE_RATE, currencyUnitConverter.getExchangeRateJson(exchangeRate, defaultCurrency, currency));
                jsonObject.put(CurrencyUnitFields.CURRENCY, currencyUnitConverter.convert(new CurrencyConverterConfig(orgId,
                        dropdownRequest.getUserId(), dropdownRequest.locale(), isBaseCurrency), currency));

                optionContainer.addOption(jsonObject);
            }
        }

        return optionContainer;
    }
}
