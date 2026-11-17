package com.tlc.crm.entity.currency;

import com.tlc.exchange.rate.model.ReadableExchangeRateContainer;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.entity.currency.api.model.BaseCurrency;
import com.tlc.crm.entity.currency.api.model.BaseCurrency.DecimalPoint;
import com.tlc.crm.entity.currency.api.model.CurrencyUnit;
import com.tlc.crm.entity.currency.internal.service.BaseCurrencyServiceImpl;

import java.math.BigDecimal;

import java.util.Collection;

/**
 * <p>
 * Provides an abstract way to deal with the {@link BaseCurrency} related services.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface BaseCurrencyService {

    /**
     * <p>
     * Gets the instance of {@link BaseCurrencyService}.
     * </p>
     *
     * @return {@link BaseCurrencyService}
     */
    static BaseCurrencyService getInstance() {
        return BaseCurrencyServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the default {@link Currency} of given organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link Currency}
     */
    Currency getDefaultCurrency(final long orgId);

    /**
     * <p>
     * Gets the {@link BaseCurrency} for an respective organization.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link BaseCurrency}
     */
    BaseCurrency getBaseCurrency(final long orgId);

    /**
     * <p>
     * Gets the {@link Collection} of {@link CurrencyUnit} for a specific organization.
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link Collection} of {@link CurrencyUnit}
     */
    Collection<CurrencyUnit> getCurrencyUnits(final long orgId);

    /**
     * <p>
     * Gets the {@link CurrencyUnit} for a specific organization and give currency.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param currencyUnitId Represents the currency unit id
     * @return {@link CurrencyUnit}
     */
    CurrencyUnit getCurrencyUnit(final long orgId, final long currencyUnitId);

    /**
     * <p>
     * Gets the {@link DecimalPoint} for the given organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link DecimalPoint}
     */
    DecimalPoint getDecimalPoint(final long orgId);

    /**
     * <p>
     * Gets the exchange rate for the specified organization and the provided target currency.
     * </p>
     *
     * @param orgId          Represents an organization
     * @param targetCurrency Represents the target currency
     * @return the rate
     */
    BigDecimal getCurrencyExchangeRate(final long orgId, final Currency targetCurrency);

    /**
     * <p>
     * Retrieves the latest {@link ReadableExchangeRateContainer} associated with the specified organization for each supported currency.
     * </p>
     *
     * @param orgId Represents an organization
     * @return exchange rates
     */
    ReadableExchangeRateContainer getExchangeRate(final long orgId);

    /**
     * <p>
     * Formats the given value according to the decimal places configured for the organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @param value Represents the value
     * @return the formatted value
     */
    BigDecimal applyPrecision(final long orgId, final BigDecimal value);

    /**
     * <p>
     * Calculates the value which is converted based on exchange rate for the given target
     * currency code and also the value will be formatted based on the decimal points.
     * </p>
     *
     * @param orgId              Represents an organization
     * @param value              Represents the value
     * @param targetCurrencyCode Represents the target currency
     * @return the value
     */
    BigDecimal formatCalibratedValue(final long orgId, final BigDecimal value, final Currency targetCurrencyCode);

    /**
     * <p>
     * Gets the {@link Collection} of active {@link Currency} codes within the organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link Collection} of {@link Currency}
     */
    Collection<Currency> getActiveCurrencies(final long orgId);

    /**
     * <p>
     * Gets the current exchange rate for target currency for an specified organization.
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param targetCurrency Represents the target {@link Currency}
     * @return the exchange rate
     */
    BigDecimal getExchangeRate(final long orgId, final Currency targetCurrency);

    /**
     * <p>
     * Gets the current exchange rate for target currency based on given base {@link Currency}.
     * </p>
     *
     * @param baseCurrency   Represents the base {@link Currency}
     * @param targetCurrency Represents the target {@link Currency}
     * @return the exchange rate
     */
    BigDecimal getExchangeRate(final Currency baseCurrency, final Currency targetCurrency);

    /**
     * <p>
     * Gets the {@link Collection} of inactive {@link Currency} codes within the organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link Collection} of {@link Currency}
     */
    Collection<Currency> getInactiveCurrencies(final long orgId);

    /**
     * <p>
     * Gets the {@link Collection} of all {@link Currency} codes within the organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link Collection} of {@link Currency}
     */
    Collection<Currency> getAllCurrencies(final long orgId);

    /**
     * <p>
     * Checks whether the currency is active or not.
     * </p>
     *
     * @param orgId    Represents an organization
     * @param currency Represents the {@link Currency}
     * @return true, if the currency active; otherwise false
     */
    boolean isActive(final long orgId, final Currency currency);
}
