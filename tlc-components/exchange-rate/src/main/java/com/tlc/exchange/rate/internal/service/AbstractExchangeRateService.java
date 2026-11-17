package com.tlc.exchange.rate.internal.service;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.internal.model.ExchangeRateContainer;
import com.tlc.exchange.rate.internal.resolver.ExchangeRateResolver;
import com.tlc.exchange.rate.model.ReadableExchangeRateContainer;
import com.tlc.exchange.rate.service.ExchangeRateService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

import java.util.Collection;
import java.util.Set;

/**
 * <p>
 * Provides the common base for implementing {@link ExchangeRateService}. Defines abstract methods
 * for accessing exchange rates and supported currencies, which can be implemented by concrete service providers.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public abstract class AbstractExchangeRateService implements ExchangeRateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractExchangeRateService.class);

    private final ExchangeRateResolver exchangeRateResolver;

    AbstractExchangeRateService() {
        this.exchangeRateResolver = ExchangeRateResolver.getInstance();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency Represents the base {@link Currency}
     * @return {@link ReadableExchangeRateContainer}
     */
    @Override
    public ReadableExchangeRateContainer getExchangeRates(final Currency baseCurrency) {
        return exchangeRateResolver.getExchangeRateContainer(baseCurrency);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency     Represents the base currency
     * @param targetCurrencies Represents the {@link Collection} of target currencies
     * @return {@link ReadableExchangeRateContainer}
     */
    @Override
    public ReadableExchangeRateContainer getExchangeRates(final Currency baseCurrency, final Set<Currency> targetCurrencies) {
        final ExchangeRateContainer existingContainer = exchangeRateResolver.getExchangeRateContainer(baseCurrency);
        final ExchangeRateContainer exchangeRateContainer = new ExchangeRateContainer();

        exchangeRateContainer.setBaseCurrency(baseCurrency);

        for (final Currency currency : targetCurrencies) {
            exchangeRateContainer.addExchangeRate(currency, existingContainer.getExchangeRate(currency));
        }

        return exchangeRateContainer;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency   Represents the base {@link Currency}
     * @param targetCurrency Represents the target {@link Currency}
     * @return the conversion rate
     */
    @Override
    public BigDecimal getExchangeRate(final Currency baseCurrency, final Currency targetCurrency) {
        return getExchangeRates(baseCurrency).getExchangeRate(targetCurrency);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link Collection} of {@link Currency} codes
     */
    @Override
    public Collection<String> getSupportedCurrencyCodes() {
        return getSupportedCurrencies().stream().map(Currency::getCode).toList();
    }

    /**
     * <p>
     * Retrieves the exchange rates for all currency codes with respect to the specified base currency from an online source
     * and encapsulated within an {@link ExchangeRateContainer}.
     * </p>
     *
     * @param baseCurrency Represents the base currency
     * @return the exchange rates with respective currency
     */
    public abstract ExchangeRateContainer getOnlineExchangeRateContainer(final Currency baseCurrency);

    /**
     * <p>
     * Retrieves the exchange rate for the provided target currency, considering the specified base currency.
     * </p>
     *
     * @param baseCurrency   Represents the base currency
     * @param targetCurrency Represents the target currency
     * @return the exchange rates of respective currencies
     */
    public abstract BigDecimal getOnlineExchangeRates(final Currency baseCurrency, final Currency targetCurrency);

    /**
     * <p>
     * Retrieves the {@link ExchangeRateContainer} for the specified collection of target currencies with
     * respect to the base currency from an online source.
     * </p>
     *
     * @param baseCurrency     Represents the base currency
     * @param targetCurrencies Represents the {@link Collection} of target currency
     * @return the exchange rates of respective currencies
     */
    public abstract ExchangeRateContainer getOnlineExchangeRateContainer(final Currency baseCurrency, final Collection<Currency> targetCurrencies);

    /**
     * <p>
     * Retrieves the supported currencies by comparing those provided by an online source with the currencies supported by our source.
     * </p>
     *
     * @return {@link Collection} of supported symbols with currency code as key and currency name as value
     */
    public abstract Collection<Currency> getSupportedCurrencies();
}
