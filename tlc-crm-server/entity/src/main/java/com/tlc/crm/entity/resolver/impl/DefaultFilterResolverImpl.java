package com.tlc.crm.entity.resolver.impl;

import com.tlc.crm.entity.cache.filter.DefaultFilterCache;
import com.tlc.crm.entity.resolver.filter.DefaultFilterResolver;
import com.tlc.crm.entity.filter.api.models.Filter;

import java.util.Collection;

/**
 * <p>
 * A resolver which resolves the default filters.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DefaultFilterResolverImpl implements DefaultFilterResolver {

    private final DefaultFilterCache filterCache;

    private static final class Instance {
        private static final DefaultFilterResolverImpl INSTANCE = new DefaultFilterResolverImpl();
    }

    private DefaultFilterResolverImpl() {
        filterCache = DefaultFilterCache.getInstance();
    }

    public static DefaultFilterResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Filter> getAll() {
        final Collection<Filter> filters = filterCache.getAll();

        if (filters.isEmpty()) {
            filterCache.load();

            return filterCache.getAll();
        }

        return filters;
    }
}
