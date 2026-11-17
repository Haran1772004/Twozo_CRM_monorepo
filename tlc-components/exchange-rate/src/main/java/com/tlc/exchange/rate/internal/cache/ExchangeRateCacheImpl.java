package com.tlc.exchange.rate.internal.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.cache.remote.CacheConfig;

import com.tlc.l10n.model.currency.Currency;

import com.tlc.exchange.rate.internal.model.ExchangeRateContainer;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Provides the functionalities for an {@link ExchangeRateCache}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see ExchangeRateCache
 */
final class ExchangeRateCacheImpl implements ExchangeRateCache {

    private final Cache<Currency, ExchangeRateContainer> exchangeRateCache;

    private static class Instance {
        private static final ExchangeRateCacheImpl INSTANCE = new ExchangeRateCacheImpl();
    }

    private ExchangeRateCacheImpl() {
        final CacheConfig<Currency, ExchangeRateContainer> exchangeRateConfig = new CacheConfig<>("exchangeRate");

        exchangeRateConfig.setTtl((int) TimeUnit.HOURS.toMillis(1));

        this.exchangeRateCache = CacheServiceProvider.get().createOrOpenCache(exchangeRateConfig);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link ExchangeRateCacheImpl}
     */
    public static ExchangeRateCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency Represents the base {@link Currency}
     * @return {@link ExchangeRateContainer}
     */
    @Override
    public ExchangeRateContainer get(final Currency baseCurrency) {
        return exchangeRateCache.get(baseCurrency);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param exchangeRateContainer Represents the {@link ExchangeRateContainer}
     */
    @Override
    public void put(final ExchangeRateContainer exchangeRateContainer) {
        exchangeRateCache.put(exchangeRateContainer.getBaseCurrency(), exchangeRateContainer);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency Represents the base {@link Currency}
     * @return true, if the currency exists; otherwise false
     */
    @Override
    public boolean exists(final Currency baseCurrency) {
        return exchangeRateCache.containsKey(baseCurrency);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    public void clear() {
        exchangeRateCache.clear();
    }
}
