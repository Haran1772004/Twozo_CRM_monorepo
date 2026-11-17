package com.tlc.crm.core.api.analytics.resolver.internal;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMCustomModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.manager.CRMCustomModularFieldColumnMappingManager;
import com.tlc.crm.core.api.analytics.cache.module.CRMCustomModularFieldColumnMappingCache;
import com.tlc.crm.core.api.analytics.resolver.module.CRMCustomModularFieldColumnMappingResolver;

import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link CRMCustomModularFieldColumnMappingResolver} that handles cache-miss resolution.
 * Checks cache first; on cache miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMCustomModularFieldColumnMappingResolverImpl implements CRMCustomModularFieldColumnMappingResolver {

    private final CRMCustomModularFieldColumnMappingCache cache;
    private final CRMCustomModularFieldColumnMappingManager columnMappingManager;

    private CRMCustomModularFieldColumnMappingResolverImpl() {
        this.cache = CRMCustomModularFieldColumnMappingCache.getInstance();
        this.columnMappingManager = CRMCustomModularFieldColumnMappingManager.INSTANCE();
    }

    private static class Instance {
        private static final CRMCustomModularFieldColumnMappingResolverImpl INSTANCE = new CRMCustomModularFieldColumnMappingResolverImpl();
    }

    public static CRMCustomModularFieldColumnMappingResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public Collection<CRMCustomModularFieldColumnMapping> getByModuleId(final Integer moduleId) {
        final Collection<CRMCustomModularFieldColumnMapping> cached = cache.get(moduleId);

        if (Objects.nonNull(cached) && !cached.isEmpty()) {
            return cached;
        }

        final Collection<CRMCustomModularFieldColumnMapping> data = columnMappingManager.fetchByModuleId(moduleId);

        if (!data.isEmpty()) {
            cache.put(moduleId, data);
        }

        return data;
    }

    @Override
    public CRMCustomModularFieldColumnMapping getByColumnMappingId(final Long columnMappingId) {
        final CRMCustomModularFieldColumnMapping cached = cache.getById(columnMappingId);

        if (Objects.nonNull(cached)) {
            return cached;
        }

        final CRMCustomModularFieldColumnMapping data = columnMappingManager.fetchByColumnMappingId(columnMappingId);

        if (Objects.nonNull(data)) {
            cache.putById(columnMappingId, data);
        }

        return data;
    }
}
