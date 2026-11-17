package com.tlc.crm.core.api.analytics.resolver.internal;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMSystemModularFieldColumnMapping;
import com.tlc.crm.core.api.analytics.manager.CRMSystemModularFieldColumnMappingManager;
import com.tlc.crm.core.api.analytics.cache.module.CRMSystemModularFieldColumnMappingCache;
import com.tlc.crm.core.api.analytics.resolver.module.CRMSystemModularFieldColumnMappingResolver;

import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link CRMSystemModularFieldColumnMappingResolver} that handles cache-miss resolution.
 * Checks cache first; on cache miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMSystemModularFieldColumnMappingResolverImpl implements CRMSystemModularFieldColumnMappingResolver {

    private final CRMSystemModularFieldColumnMappingCache cache;
    private final CRMSystemModularFieldColumnMappingManager columnMappingManager;

    private CRMSystemModularFieldColumnMappingResolverImpl() {
        this.cache = CRMSystemModularFieldColumnMappingCache.getInstance();
        this.columnMappingManager = CRMSystemModularFieldColumnMappingManager.INSTANCE();
    }

    private static class Instance {
        private static final CRMSystemModularFieldColumnMappingResolverImpl INSTANCE = new CRMSystemModularFieldColumnMappingResolverImpl();
    }

    public static CRMSystemModularFieldColumnMappingResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public Collection<CRMSystemModularFieldColumnMapping> getBySourceId(final Long modularFieldId) {
        final Collection<CRMSystemModularFieldColumnMapping> cached = cache.get(modularFieldId);

        if (Objects.nonNull(cached) && !cached.isEmpty()) {
            return cached;
        }

        final Collection<CRMSystemModularFieldColumnMapping> data = columnMappingManager.fetchByModularFieldId(modularFieldId);

        if (!data.isEmpty()) {
            cache.put(modularFieldId, data);
        }

        return data;
    }

    @Override
    public CRMSystemModularFieldColumnMapping getById(final Long mappingId) {
        final CRMSystemModularFieldColumnMapping cached = cache.getById(mappingId);

        if (Objects.nonNull(cached)) {
            return cached;
        }

        final CRMSystemModularFieldColumnMapping data = columnMappingManager.fetchByMappingId(mappingId);

        if (Objects.nonNull(data)) {
            cache.putById(mappingId, data);
        }

        return data;
    }
}
