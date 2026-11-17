package com.tlc.crm.entity.service.impl;

import com.tlc.crm.entity.filter.api.models.Filter;
import com.tlc.crm.entity.resolver.filter.DefaultFilterResolver;
import com.tlc.crm.entity.service.filter.DefaultFilterService;

import java.util.Collection;

/**
 * <p>
 * Provides the cache service of the default filters.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class DefaultFilterServiceImpl implements DefaultFilterService {

    private final DefaultFilterResolver filterResolver;

    private static final class Instance {
        private static final DefaultFilterServiceImpl INSTANCE = new DefaultFilterServiceImpl();
    }

    private DefaultFilterServiceImpl() {
        filterResolver = DefaultFilterResolver.getInstance();
    }

    public static DefaultFilterServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<Filter> getAll() {
        return filterResolver.getAll();
    }
}
