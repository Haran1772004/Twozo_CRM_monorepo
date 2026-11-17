package com.tlc.exchange.rate.internal.manager;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.internal.manager.impl.ExchangeRateManagerImpl;
import com.tlc.exchange.rate.internal.model.ExchangeRateContainer;

import java.util.Collection;

/**
 * <p>
 * Provides the services for managing and retrieving conversion rates.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface ExchangeRateManager {

    /**
     * <p>
     * Gets the instance of {@link ExchangeRateManagerImpl}
     * </p>
     *
     * @return {@link ExchangeRateManagerImpl}
     */
    static ExchangeRateManager get() {
        return ExchangeRateManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link ExchangeRateContainer} for the given {@link Currency}.
     * </p>
     *
     * @param currency Represents the {@link Currency}
     * @return {@link ExchangeRateContainer}
     */
    ExchangeRateContainer getExchangeRateContainer(final Currency currency);

    /**
     * <p>
     * Creates and stores the {@link Collection} of {@link ExchangeRateContainer} into the database. Takes a collection
     * of ExchangeRateContainer and persists them into the underlying database.
     * </p>
     *
     * @param exchangeRateContainers Represents a set of exchange rates between different currencies.
     */
    void create(final Collection<ExchangeRateContainer> exchangeRateContainers);

    /**
     * <p>
     * Updates and stores the {@link Collection} of {@link ExchangeRateContainer} into the database. Takes a collection
     * of ExchangeRateContainer and persists them into the underlying database.
     * </p>
     *
     * @param exchangeRateContainers Represents a set of exchange rates between different currencies.
     */
    void update(final Collection<ExchangeRateContainer> exchangeRateContainers);

    /**
     * <p>
     * Retrieves the total number of available exchange rates exists in the db.
     * </p>
     *
     * @return the exchange rate count
     */
    long getTotalRecords();
}
