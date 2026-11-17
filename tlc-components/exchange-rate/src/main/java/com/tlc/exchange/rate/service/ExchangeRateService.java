package com.tlc.exchange.rate.service;

import com.tlc.commons.service.Service;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.model.ReadableExchangeRateContainer;

import java.math.BigDecimal;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * Provides the services for accessing the exchange rates from the cache based on the base currency.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface ExchangeRateService extends Service {

    /**
     * <p>
     * Retrieves the {@link ReadableExchangeRateContainer} which holds exchange rates for the specified base {@link Currency},
     * considering the respective target currencies.
     * For example, if the base currency is USD, the returned {@link ReadableExchangeRateContainer} might contain rates such as:
     *  <ul>
     *      <li>EUR: 0.85</li>
     *      <li>GBP: 0.75</li>
     *      <li>JPY: 105.50</li>
     *      <li>...</li>
     *  </ul>
     * </p>
     *
     * @param currency Represents the base {@link Currency}
     * @return The ExchangeRatesHolder containing exchange rates with all respective target currency for the specified base currency
     */
    ReadableExchangeRateContainer getExchangeRates(final Currency currency);

    /**
     * <p>
     * Retrieves the {@link ReadableExchangeRateContainer} containing exchange rates for the specified base currency
     * and target currencies, with each rate associated with its respective target currency.
     * For example, if the base currency is <b>USD</b> and the target currencies are <b>EUR</b> and <b>GBP</b>, the returned
     * {@link ReadableExchangeRateContainer} might contain rates such as:
     *  <ul>
     *      <li>EUR: 0.85</li>
     *      <li>GBP: 0.75</li>
     *  </ul>
     * </p>
     *
     * @param baseCurrency     Represents the base currency
     * @param targetCurrencies Represents the {@link Collection} of target currencies
     * @return The {@link ReadableExchangeRateContainer} containing exchange rates with its respective target currency for the specified base currency
     */
    ReadableExchangeRateContainer getExchangeRates(final Currency baseCurrency, final Set<Currency> targetCurrencies);

    /**
     * <p>
     * Fetches the exchange rate from the base {@link Currency} to the target currency.
     * </p>
     *
     * @param baseCurrency   Represents the base {@link Currency}
     * @param targetCurrency Represents the target {@link Currency}
     * @return The exchange rate from the base currency to the target currency, represented as a {@link BigDecimal}.
     */
    BigDecimal getExchangeRate(final Currency baseCurrency, final Currency targetCurrency);

    /**
     * <p>
     * Retrieves the {@link Collection} of supported {@link Currency} by comparing the currencies
     * offered by external providers with those supported by our service.
     * If a currency matches the supported currencies, it is considered supported and returned.
     * </p>
     *
     * @return {@link Collection} of {@link Currency}
     */
    Collection<Currency> getSupportedCurrencies();

    /**
     * <p>
     * Gets the {@link Collection} of supported {@link Currency} codes.
     * </p>
     *
     * @return {@link Collection} of {@link Currency} codes
     */
    Collection<String> getSupportedCurrencyCodes();
}
