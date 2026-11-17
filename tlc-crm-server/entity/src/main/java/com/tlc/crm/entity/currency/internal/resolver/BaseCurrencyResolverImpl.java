package com.tlc.crm.entity.currency.internal.resolver;

import com.tlc.commons.concurrent.LockStore;

import com.tlc.exchange.rate.service.provider.ExchangeRateServiceProvider;
import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.service.ExchangeRateService;
import com.tlc.exchange.rate.model.ReadableExchangeRateContainer;

import com.tlc.crm.entity.currency.api.manager.BaseCurrencyManager;
import com.tlc.crm.entity.currency.api.model.BaseCurrency;
import com.tlc.crm.entity.currency.api.model.CurrencyUnit;
import com.tlc.crm.entity.currency.internal.cache.BaseCurrencyCache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * <p>
 * Resolves the {@link CurrencyUnit} related details.
 * </P>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
final class BaseCurrencyResolverImpl implements BaseCurrencyResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseCurrencyResolverImpl.class);
    private static final int STRIPE_COUNT = 10;

    private final LockStore<Long> lockStore;
    private final BaseCurrencyCache baseCurrencyCache;
    private final BaseCurrencyManager baseCurrencyManager;
    private final ExchangeRateService exchangeRateService;

    private BaseCurrencyResolverImpl() {
        this.lockStore = new LockStore<>(STRIPE_COUNT);
        this.baseCurrencyCache = BaseCurrencyCache.getInstance();
        this.baseCurrencyManager = BaseCurrencyManager.getInstance();
        this.exchangeRateService = ExchangeRateServiceProvider.get();
    }

    private static class Instance {
        private static final BaseCurrencyResolverImpl INSTANCE = new BaseCurrencyResolverImpl();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link BaseCurrencyResolverImpl}
     */
    public static BaseCurrencyResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return {@link BaseCurrency}
     */
    @Override
    public BaseCurrency fetchBaseCurrency(final long orgId) {
        final ReentrantReadWriteLock lock = lockStore.getLock(orgId);
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final BaseCurrency existingBaseCurrency;

        readLock.lock();

        try {
            existingBaseCurrency = baseCurrencyCache.getBaseCurrency(orgId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(existingBaseCurrency)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final BaseCurrency baseCurrency = baseCurrencyManager.getBaseCurrency(orgId);
                final Collection<CurrencyUnit> currencyUnits = baseCurrency.getCurrencyUnits();

                if (Objects.nonNull(currencyUnits) && !currencyUnits.isEmpty()) {
                    final Collection<CurrencyUnit> currencies = new HashSet<>(currencyUnits);
                    final Collection<Currency> inactiveBaseCurrencies = baseCurrencyManager.getInactiveBaseCurrencies(orgId);
                    final Collection<Currency> existingCurrencies = currencies.stream().map(CurrencyUnit::getCurrency).collect(Collectors.toUnmodifiableSet());

                    for (final Currency inactiveCurrency : inactiveBaseCurrencies) {

                        if (!existingCurrencies.contains(inactiveCurrency)) {
                            currencies.add(new CurrencyUnit(orgId, inactiveCurrency, false));
                        }
                    }
                    baseCurrency.setCurrencyUnits(currencies);
                }
                loadBaseCurrencyIntoCache(baseCurrency);

                return baseCurrency;
            } finally {
                writeLock.unlock();
            }
        }

        return existingBaseCurrency;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization
     * @return {@link ReadableExchangeRateContainer}
     */
    @Override
    public ReadableExchangeRateContainer getExchangeRate(final long orgId) {
        final BaseCurrency baseCurrency = fetchBaseCurrency(orgId);

        return baseCurrency.getReadableExchangeRateContainer();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param currency Represents the {@link Currency}
     * @return {@link ReadableExchangeRateContainer}
     */
    @Override
    public ReadableExchangeRateContainer getExchangeRate(final Currency currency) {
        return exchangeRateService.getExchangeRates(currency);
    }

    /**
     * <p>
     * Loads the base currency information for a specific organization in the currency cache.
     * </p>
     *
     * @param baseCurrency Represents the {@link BaseCurrency}
     */
    private void loadBaseCurrencyIntoCache(final BaseCurrency baseCurrency) {
        setExchangeRates(baseCurrency);
        baseCurrencyCache.setBaseCurrency(baseCurrency);
    }

    /**
     * <p>
     * Sets the exchange rates in the specified ExchangeRateContainer for a given set of currencies,
     * with respect to the provided base currency. The {@link ReadableExchangeRateContainer} are sourced from
     * the existing exchange rates of the base currency.
     * </p>
     *
     * @param baseCurrency Represents the {@link BaseCurrency}
     */
    private void setExchangeRates(final BaseCurrency baseCurrency) {
        final Set<Currency> currencies = new HashSet<>();
        final Collection<CurrencyUnit> currencyUnits = baseCurrency.getCurrencyUnits();
        final Currency defaultCurrency = baseCurrency.getDefaultCurrency();
        final Collection<Currency> inactiveBaseCurrencies = baseCurrencyManager.getInactiveBaseCurrencies(baseCurrency.orgId());

        if (!inactiveBaseCurrencies.isEmpty()) {
            currencies.addAll(inactiveBaseCurrencies);
        }

        if (Objects.nonNull(currencyUnits) && !currencyUnits.isEmpty()) {
            currencies.addAll(baseCurrency.getCurrencyUnits().stream()
                    .map(CurrencyUnit::getCurrency).collect(Collectors.toUnmodifiableSet()));
        }
        currencies.add(defaultCurrency);
        baseCurrency.setReadableExchangeRateContainer(exchangeRateService
                .getExchangeRates(defaultCurrency, currencies));
    }
}