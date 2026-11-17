package com.tlc.crm.core.api.analytics.cache.internal;

import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;
import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.core.api.analytics.cache.module.CRMSystemModularFieldCache;
import com.tlc.crm.core.api.analytics.manager.config.field.SystemModularFieldManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

/**
 * Implementation of {@link CRMSystemModularFieldCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMSystemModularFieldCacheImpl implements CRMSystemModularFieldCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMSystemModularFieldCacheImpl.class);
    private final Cache<Integer, Collection<CRMSystemModularField>> cacheByModuleId;
    private final Cache<Long, CRMSystemModularField> cacheById;
    private final SystemModularFieldManager systemModularFieldManager;

    /**
     * <p>
     * Private constructor initializing the local caches.
     * </p>
     */
    private CRMSystemModularFieldCacheImpl() {
        this.cacheByModuleId = CacheManager.getInstance().createCache();
        this.cacheById = CacheManager.getInstance().createCache();
        this.systemModularFieldManager = SystemModularFieldManager.INSTANCE();
    }

    private static class Instance {
        private static final CRMSystemModularFieldCacheImpl INSTANCE = new CRMSystemModularFieldCacheImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link CRMSystemModularFieldCacheImpl}.
     * </p>
     *
     * @return The {@link CRMSystemModularFieldCacheImpl} instance
     */
    public static CRMSystemModularFieldCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        systemModularFieldManager.fetchAll().forEach(field -> {
            cacheById.put(field.id(), field);

            final Integer moduleId = field.getModuleId();
            Collection<CRMSystemModularField> existing = cacheByModuleId.get(moduleId);

            if (existing == null) {
                existing = new ArrayList<>();
                cacheByModuleId.put(moduleId, existing);
            }

            existing.add(field);
        });

        LOGGER.info("CRM Analytics : CRMSystemModularField loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheByModuleId.clear();
        cacheById.clear();
        LOGGER.info("CRM Analytics : CRMSystemModularField cache cleared");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CRMSystemModularField> get(final Integer moduleId) {
        return cacheByModuleId.get(moduleId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CRMSystemModularField getById(final Long fieldId) {
        return cacheById.get(fieldId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Integer moduleId, final Collection<CRMSystemModularField> fields) {
        cacheByModuleId.put(moduleId, fields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putById(final Long fieldId, final CRMSystemModularField field) {
        cacheById.put(fieldId, field);
    }
}
