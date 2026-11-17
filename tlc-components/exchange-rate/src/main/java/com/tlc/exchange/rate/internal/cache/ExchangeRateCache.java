package com.tlc.exchange.rate.internal.cache;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.internal.model.ExchangeRateContainer;

/**
 * <p>
 * Provides the services for accessing and loading the exchange rate details from the cache.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface ExchangeRateCache {

    /**
     * <p>
     * Gets the instance for {@link ExchangeRateCache}.
     * </p>
     *
     * @return {@link ExchangeRateCache}
     */
    static ExchangeRateCache getInstance() {
        return ExchangeRateCacheImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link ExchangeRateContainer} containing exchange rates for the
     * respective target currency based on currency from the cache.
     * </p>
     *
     * @param baseCurrency Represents the base {@link Currency}
     * @return {@link ExchangeRateContainer}
     */
    ExchangeRateContainer get(final Currency baseCurrency);

    /**
     * <p>
     * Sets the {@link ExchangeRateContainer} containing exchange rates for the
     * respective target currency based on currency from the cache.
     * </p>
     *
     * @param exchangeRateContainer Represents the {@link ExchangeRateContainer}
     */
    void put(final ExchangeRateContainer exchangeRateContainer);

    /**
     * <p>
     * Checks whether the respective currency exists in the cache.
     * </p>
     *
     * @param baseCurrency Represents the base {@link Currency}
     * @return true, if the currency exists; otherwise false
     */
    boolean exists(final Currency baseCurrency);

    /**
     * <p>
     * Clears the cache.
     * </p>
     */
    void clear();
}
