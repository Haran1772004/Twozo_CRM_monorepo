package com.tlc.crm.entity.currency.internal.resolver;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.model.ReadableExchangeRateContainer;

import com.tlc.crm.entity.currency.api.model.BaseCurrency;

/**
 * <p>
 * Provides the services for {@link BaseCurrencyResolver}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface BaseCurrencyResolver {

    /**
     * <p>
     * Gets the instance for {@link BaseCurrencyResolver}.
     * </p>
     *
     * @return {@link BaseCurrencyResolver}
     */
    static BaseCurrencyResolver getInstance() {
        return BaseCurrencyResolverImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the base currency associated with the specified organization.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link BaseCurrency}
     */
    BaseCurrency fetchBaseCurrency(final long orgId);

    /**
     * <p>
     * Gets the {@link ReadableExchangeRateContainer} for an provided organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @return the {@link ReadableExchangeRateContainer}
     */
    ReadableExchangeRateContainer getExchangeRate(final long orgId);

    /**
     * <p>
     * Gets the {@link ReadableExchangeRateContainer} based on {@link Currency}.
     * </p>
     *
     * @param targetCurrency Represents the target {@link Currency}
     * @return the {@link ReadableExchangeRateContainer}
     */
    ReadableExchangeRateContainer getExchangeRate(final Currency targetCurrency);
}
