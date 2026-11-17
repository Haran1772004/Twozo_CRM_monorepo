package com.tlc.crm.entity.currency.internal.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.cache.remote.CacheConfig;

import com.tlc.crm.entity.currency.fields.CurrencyUnitFields;
import com.tlc.crm.entity.currency.api.model.BaseCurrency;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Stores the {@link BaseCurrency} details into the cache.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see BaseCurrencyCache
 */
final class BaseCurrencyCacheImpl implements BaseCurrencyCache {

    private final Cache<Long, BaseCurrency> baseCurrencyCache;

    private static class Instance {
        private static final BaseCurrencyCacheImpl INSTANCE = new BaseCurrencyCacheImpl();
    }

    private BaseCurrencyCacheImpl() {
        final CacheConfig<Long, BaseCurrency> baseCurrencyConfig = new CacheConfig<>(CurrencyUnitFields.BASE_CURRENCY);

        baseCurrencyConfig.setTtl((int) TimeUnit.MINUTES.toMillis(10));
        this.baseCurrencyCache = CacheServiceProvider.get().createOrOpenCache(baseCurrencyConfig);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link BaseCurrencyCacheImpl}
     */
    public static BaseCurrencyCacheImpl getInstance() {
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
    public BaseCurrency getBaseCurrency(final long orgId) {
        return baseCurrencyCache.get(orgId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param baseCurrency {@link BaseCurrency}
     */
    @Override
    public void setBaseCurrency(final BaseCurrency baseCurrency) {
        baseCurrencyCache.put(baseCurrency.orgId(), baseCurrency);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization id
     * @return true, if the organization exists
     */
    @Override
    public boolean exists(final long orgId) {
        return baseCurrencyCache.containsKey(orgId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId Represents an organization id
     */
    @Override
    public void remove(final long orgId) {
        baseCurrencyCache.remove(orgId);
    }
}
