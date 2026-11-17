package com.tlc.crm.marketplace.app.internal.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.remote.CacheConfig;
import com.tlc.cache.service.provider.CacheServiceProvider;

import com.tlc.crm.marketplace.app.api.model.CrmMarketplaceApp;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the CrmMarketplaceAppCache interface responsible for caching CrmMarketplaceApp instances.
 *
 * @author ThalaimalaiPandiyanT, Muthu kumar V
 * @version 1.0
 */
final class CrmMarketplaceAppCacheImpl implements CrmMarketplaceAppCache {

    private final Cache<Long, CrmMarketplaceApp> marketplaceAppCache;

    private CrmMarketplaceAppCacheImpl() {
        final CacheConfig<Long, CrmMarketplaceApp> preferenceCacheConfig = new CacheConfig<>("marketplaceAppCache");

        preferenceCacheConfig.setTtl((int) TimeUnit.HOURS.toMillis(1));

        this.marketplaceAppCache = CacheServiceProvider.get().createOrOpenCache(preferenceCacheConfig);
    }

    private static class Instance {
        private static final CrmMarketplaceAppCacheImpl INSTANCE = new CrmMarketplaceAppCacheImpl();
    }

    /**
     * Gets the {@link CrmMarketplaceAppCacheImpl} instance.
     *
     * @return {@link CrmMarketplaceAppCacheImpl}
     */
    public static CrmMarketplaceAppCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Loads the data into the cache.
     */
    @Override
    public void load() {
    }

    /**
     * {@inheritDoc}
     *
     * @param id The unique ID of the CrmMarketplaceApp to retrieve.
     * @return The {@code CrmMarketplaceApp} with the specified ID, or {@code null} if not found.
     */
    @Override
    public CrmMarketplaceApp get(final long id) {
        return marketplaceAppCache.get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmMarketplaceApp The {@code CrmMarketplaceApp}.
     */
    @Override
    public void setMarketplaceApp(final CrmMarketplaceApp crmMarketplaceApp) {
        marketplaceAppCache.put(crmMarketplaceApp.id(), crmMarketplaceApp);
    }

    /**
     * {@inheritDoc}
     *
     * @param crmMarketplaceApps The collection of CrmMarketplaceApp instances.
     */
    @Override
    public void setMarketplaceApps(final Collection<CrmMarketplaceApp> crmMarketplaceApps) {
        if (Objects.nonNull(crmMarketplaceApps)) {
            crmMarketplaceApps.forEach(marketplaceApp -> marketplaceAppCache.put(marketplaceApp.id(), marketplaceApp));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param appIds The collection of CrmMarketplaceApp ids.
     */
    @Override
    public void remove(final Set<Long> appIds) {
        marketplaceAppCache.removeAll(appIds);
    }
}