package com.tlc.crm.core.api.analytics.resolver.internal;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.crm.core.api.analytics.manager.config.field.SystemModularFieldManager;
import com.tlc.crm.core.api.analytics.cache.module.CRMSystemModularFieldCache;
import com.tlc.crm.core.api.analytics.resolver.module.CRMSystemModularFieldResolver;

import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link CRMSystemModularFieldResolver} that handles cache-miss resolution.
 * Checks cache first; on cache miss, fetches from DB and populates cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMSystemModularFieldResolverImpl implements CRMSystemModularFieldResolver {

    private final CRMSystemModularFieldCache cache;
    private final SystemModularFieldManager systemModularFieldManager;

    private CRMSystemModularFieldResolverImpl() {
        this.cache = CRMSystemModularFieldCache.getInstance();
        this.systemModularFieldManager = SystemModularFieldManager.INSTANCE();
    }

    private static class Instance {
        private static final CRMSystemModularFieldResolverImpl INSTANCE = new CRMSystemModularFieldResolverImpl();
    }

    public static CRMSystemModularFieldResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    @Override
    public Collection<CRMSystemModularField> getBySourceId(final Integer moduleId) {
        final Collection<CRMSystemModularField> cached = cache.get(moduleId);

        if (Objects.nonNull(cached) && !cached.isEmpty()) {
            return cached;
        }

        final Collection<CRMSystemModularField> data = systemModularFieldManager.fetchByModuleId(moduleId);

        if (!data.isEmpty()) {
            cache.put(moduleId, data);
        }

        return data;
    }

    @Override
    public CRMSystemModularField getById(final Long fieldId) {
        final CRMSystemModularField cached = cache.getById(fieldId);

        if (Objects.nonNull(cached)) {
            return cached;
        }

        final CRMSystemModularField data = systemModularFieldManager.fetchByFieldId(fieldId);

        if (Objects.nonNull(data)) {
            cache.putById(fieldId, data);
        }

        return data;
    }
}
