package com.tlc.exchange.rate.internal.resolver;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.internal.model.ExchangeRateContainer;

/**
 * <p>
 * Provides the services to resolve exchange rates based on the base currency
 * and enables efficient retrieval and calculation of exchange rates for different currencies.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface ExchangeRateResolver {

    /**
     * <p>
     * Gets the instance for {@link ExchangeRateResolver}.
     * </p>
     *
     * @return {@link ExchangeRateResolver}
     */
    static ExchangeRateResolver getInstance() {
        return ExchangeRateResolverImpl.getInstance();
    }

    /**
     * <p>
     * Retrieves the {@link ExchangeRateContainer} containing exchange rates for the respective target currencies based on the given base currency.
     * If the exchange rates are found in the existing cache, they are retrieved from there; otherwise, they are fetched from the database and loaded into the cache.
     * </p>
     *
     * @param baseCurrency Represents the base {@link Currency}
     * @return The exchangeRateContainer containing exchange rates for the respective target currencies.
     */
    ExchangeRateContainer getExchangeRateContainer(final Currency baseCurrency);
}
