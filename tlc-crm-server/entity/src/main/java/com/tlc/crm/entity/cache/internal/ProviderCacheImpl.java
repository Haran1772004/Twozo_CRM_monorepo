package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.commons.pair.Pair;
import com.tlc.crm.entity.cache.list.ProviderCache;
import com.tlc.crm.entity.list.model.Provider;
import com.tlc.crm.entity.manager.list.ProviderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ProviderCacheImpl implements ProviderCache {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProviderCacheImpl.class);

    private final Cache<Long, Pair<Long, Provider>> providerCache;
    private final ProviderManager providerManager;

    private static class Instance {
        private static final ProviderCacheImpl INSTANCE = new ProviderCacheImpl();
    }

    private ProviderCacheImpl() {
        providerCache = CacheManager.getInstance().createCache();
        providerManager = ProviderManager.getInstance();
    }

    /**
     * Gets the {@link ProviderCacheImpl} instance.
     *
     * @return {@link ProviderCacheImpl}.
     */
    public static ProviderCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        final Map<Long, Pair<Long, Provider>> providers = providerManager.fetchAll();

        if (!providers.isEmpty()) {
            LOGGER.info("Total number of provider entity loaded into cache : {}", providers.size());
            providerCache.putAll(providers);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        providerCache.clear();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link Map} containing key as provider id value as {@link Provider}.
     */
    @Override
    public Map<Long, Provider> getAll() {
        final Map<Long, Provider> providers = new HashMap<>();

        providerCache.forEach((sourceId, providerEntry) -> providers.putAll(Map.ofEntries(providerEntry)));

        return providers;
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceId Represents the source id.
     * @return The {@link Pair} containing key as  provider id value as {@link Provider}
     */
    @Override
    public Pair<Long, Provider> get(final Long sourceId) {
        return providerCache.get(sourceId);
    }

    /**
     * {@inheritDoc}
     *
     * @param sourceId      Represents the source id.
     * @param providerEntry Which contains {@link Pair} key as provider id value as {@link Provider}
     */
    @Override
    public void setProvider(final Long sourceId, final Pair<Long, Provider> providerEntry) {
        providerCache.put(sourceId, providerEntry);
    }
}
