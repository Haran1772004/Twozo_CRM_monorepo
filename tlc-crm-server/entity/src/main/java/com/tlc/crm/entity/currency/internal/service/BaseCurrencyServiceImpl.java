package com.tlc.crm.entity.currency.internal.service;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.crm.entity.currency.api.model.BaseCurrency;
import com.tlc.crm.entity.currency.api.model.BaseCurrency.Status;
import com.tlc.crm.entity.currency.api.model.BaseCurrency.DecimalPoint;
import com.tlc.crm.entity.currency.api.model.CurrencyUnit;
import com.tlc.crm.entity.currency.BaseCurrencyService;
import com.tlc.crm.entity.currency.internal.resolver.BaseCurrencyResolver;

import com.tlc.exchange.rate.model.ReadableExchangeRateContainer;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Provides the {@link BaseCurrency} related services.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see BaseCurrencyService
 */
public final class BaseCurrencyServiceImpl implements BaseCurrencyService {

    private final BaseCurrencyResolver baseCurrencyResolver;

    private BaseCurrencyServiceImpl() {
        this.baseCurrencyResolver = BaseCurrencyResolver.getInstance();
    }

    private static class Instance {
        private static final BaseCurrencyServiceImpl INSTANCE = new BaseCurrencyServiceImpl();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return The {@link BaseCurrencyServiceImpl}
     */
    public static BaseCurrencyServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link BaseCurrency}
     */
    @Override
    public BaseCurrency getBaseCurrency(final long orgId) {
        return baseCurrencyResolver.fetchBaseCurrency(orgId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link Collection} of {@link CurrencyUnit}
     */
    @Override
    public Collection<CurrencyUnit> getCurrencyUnits(final long orgId) {
        final BaseCurrency baseCurrency = baseCurrencyResolver.fetchBaseCurrency(orgId);

        return Objects.isNull(baseCurrency) ? null : baseCurrency.getCurrencyUnits();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param currencyUnitId Represents the currency unit id
     * @return {@link CurrencyUnit}
     */
    @Override
    public CurrencyUnit getCurrencyUnit(final long orgId, final long currencyUnitId) {
        final Collection<CurrencyUnit> currencyUnits = getCurrencyUnits(orgId);

        return Objects.isNull(currencyUnits) ? null : currencyUnits.stream().filter(currency ->
                Objects.equals(currency.id(), currencyUnitId)).findFirst().orElse(null);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link DecimalPoint}
     */
    @Override
    public DecimalPoint getDecimalPoint(final long orgId) {
        return getBaseCurrency(orgId).getDecimalPoint();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId    Represents an organization
     * @param currency Represents the target currency
     * @return the exchange rate
     */
    @Override
    public BigDecimal getCurrencyExchangeRate(final long orgId, final Currency currency) {
        final ReadableExchangeRateContainer readableExchangeRateContainer = getExchangeRate(orgId);

        return Objects.nonNull(readableExchangeRateContainer) ? readableExchangeRateContainer.getExchangeRate(currency) : BigDecimal.ZERO;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @return exchange rates
     */
    @Override
    public ReadableExchangeRateContainer getExchangeRate(final long orgId) {
        return baseCurrencyResolver.getExchangeRate(orgId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link Currency}
     */
    @Override
    public Currency getDefaultCurrency(final long orgId) {
        return getBaseCurrency(orgId).getDefaultCurrency();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @param value Represents the value
     * @return the formatted value
     */
    @Override
    public BigDecimal applyPrecision(final long orgId, final BigDecimal value) {
        final short decimalPoints = getDecimalPoint(orgId).getPoints();

        return value.setScale(decimalPoints, RoundingMode.HALF_DOWN);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId              Represents an organization
     * @param value              Represents the value
     * @param targetCurrencyCode Represents the target currency
     * @return the value
     */
    @Override
    public BigDecimal formatCalibratedValue(final long orgId, final BigDecimal value, final Currency targetCurrencyCode) {
        return applyPrecision(orgId, value.divide(getCurrencyExchangeRate(orgId, targetCurrencyCode), NumericalConstant.FIVE, RoundingMode.HALF_EVEN));
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link Collection} of {@link Currency}
     */
    @Override
    public Collection<Currency> getActiveCurrencies(final long orgId) {
        return getCurrenciesByStatus(orgId, Status.ACTIVE);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId          Represents an organization id
     * @param targetCurrency Represents the target {@link Currency}
     * @return the exchange rate
     */
    @Override
    public BigDecimal getExchangeRate(final long orgId, final Currency targetCurrency) {
        return getExchangeRate(orgId).getExchangeRate(targetCurrency);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency   Represents the base {@link Currency}
     * @param targetCurrency Represents the target {@link Currency}
     * @return the exchange rate
     */
    @Override
    public BigDecimal getExchangeRate(final Currency baseCurrency, final Currency targetCurrency) {
        return baseCurrencyResolver.getExchangeRate(baseCurrency).getExchangeRate(targetCurrency);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link Collection} of {@link Currency}
     */
    @Override
    public Collection<Currency> getInactiveCurrencies(final long orgId) {
        return getCurrenciesByStatus(orgId, Status.INACTIVE);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link Collection} of {@link Currency}
     */
    @Override
    public Collection<Currency> getAllCurrencies(final long orgId) {
        final Map<Status, Collection<Currency>> currencies = getCurrencies(orgId);
        final Collection<Currency> inactiveCurrencies = currencies.get(Status.INACTIVE);
        final Collection<Currency> activeCurrencies = new HashSet<>(currencies.get(Status.ACTIVE));

        if (!inactiveCurrencies.isEmpty()) {
            activeCurrencies.addAll(inactiveCurrencies);
        }

        return activeCurrencies;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId    Represents an organization
     * @param currency Represents the {@link Currency}
     * @return true, if the currency is active; otherwise false
     */
    @Override
    public boolean isActive(final long orgId, final Currency currency) {
        return getActiveCurrencies(orgId).contains(currency);
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link Currency} for an given organization.
     * </p>
     *
     * @param orgId Represents an organization
     * @return the active and inactive currencies
     */
    private Map<Status, Collection<Currency>> getCurrencies(final long orgId) {
        final BaseCurrency baseCurrency = getBaseCurrency(orgId);
        final Collection<Currency> activeCurrencies = new HashSet<>();
        final Collection<Currency> inactiveCurrencies = new HashSet<>();
        final Map<Status, Collection<Currency>> currencies = new HashMap<>();

        activeCurrencies.add(baseCurrency.getDefaultCurrency());
        baseCurrency.getCurrencyUnits().forEach(currencyUnit -> {
            final Currency currency = currencyUnit.getCurrency();

            if (currencyUnit.isEnabled()) {
                activeCurrencies.add(currency);
            } else {
                inactiveCurrencies.add(currency);
            }
        });

        currencies.put(Status.ACTIVE, activeCurrencies);
        currencies.put(Status.INACTIVE, inactiveCurrencies);

        return currencies;
    }

    /**
     * <p>
     * Gets the {@link Collection} of {@link Currency} based on active status
     * </p>
     *
     * @param orgId  Represents an organization
     * @param status Represents the {@link Status}
     * @return {@link Collection} of {@link Currency}
     */
    private Collection<Currency> getCurrenciesByStatus(final long orgId, final Status status) {
        return getCurrencies(orgId).get(status);
    }
}
