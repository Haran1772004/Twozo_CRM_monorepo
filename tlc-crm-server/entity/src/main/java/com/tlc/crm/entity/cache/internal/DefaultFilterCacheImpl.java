package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;

import com.tlc.crm.entity.cache.filter.DefaultFilterCache;
import com.tlc.crm.entity.filter.api.manager.FilterManager;
import com.tlc.crm.entity.filter.api.models.Filter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * <p>
 * Provides the cache service of the default filters.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DefaultFilterCacheImpl implements DefaultFilterCache {

    private final Cache<Long, Collection<Filter>> filterCache;
    private final FilterManager filterManager;

    private static final class Instance {
        private static final DefaultFilterCacheImpl INSTANCE = new DefaultFilterCacheImpl();
    }

    private DefaultFilterCacheImpl() {
        filterCache = CacheManager.getInstance().createCache();
        filterManager = FilterManager.getInstance();
    }

    public static DefaultFilterCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Filter> getAll() {
        final Collection<Filter> filters = new ArrayList<>();

        filterCache.forEach((providerId, defaultFilters) -> filters.addAll(defaultFilters.stream().map(Filter::clone).toList()));

        return filters;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        filterCache.putAll(filterManager.getDefaultFilters());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        filterCache.clear();
    }
}
