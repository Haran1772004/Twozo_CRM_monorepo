package com.tlc.exchange.rate.model;

import com.tlc.l10n.model.currency.Currency;

import java.math.BigDecimal;

/**
 * <p>
 * Provides the services to retrieve the exchange rate containers, which typically contain information about currency exchange rates.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface ReadableExchangeRateContainer {

    /**
     * <p>
     * Retrieves the exchange rate for the specified target currency from the rate container.
     * </p>
     *
     * @param targetCurrency Represents the target {@link Currency}
     * @return the exchange rate
     */
    BigDecimal getExchangeRate(final Currency targetCurrency);

    /**
     * <p>
     * Gets the base {@link Currency}.
     * </p>
     *
     * @return the base {@link Currency}
     */
    Currency getBaseCurrency();

    /**
     * <p>
     * Checks whether the {@link ReadableExchangeRateContainer} instance has any exchange rate information.
     * </p>
     *
     * @return If the container is empty, it returns true; otherwise, it returns false
     */
    boolean isEmpty();
}
