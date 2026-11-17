package com.tlc.crm.core.api.analytics.cache.internal;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMCustomModularFieldColumnMapping;
import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.core.api.analytics.cache.module.CRMCustomModularFieldColumnMappingCache;
import com.tlc.crm.core.api.analytics.manager.CRMCustomModularFieldColumnMappingManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of {@link CRMCustomModularFieldColumnMappingCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMCustomModularFieldColumnMappingCacheImpl implements CRMCustomModularFieldColumnMappingCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMCustomModularFieldColumnMappingCacheImpl.class);
    private final Cache<Integer, Collection<CRMCustomModularFieldColumnMapping>> cacheByModuleId;
    private final Cache<Long, CRMCustomModularFieldColumnMapping> cacheById;
    private final CRMCustomModularFieldColumnMappingManager columnMappingManager;

    /**
     * <p>
     * Private constructor initializing the local caches.
     * </p>
     */
    private CRMCustomModularFieldColumnMappingCacheImpl() {
        this.cacheByModuleId = CacheManager.getInstance().createCache();
        this.cacheById = CacheManager.getInstance().createCache();
        this.columnMappingManager = CRMCustomModularFieldColumnMappingManager.INSTANCE();
    }

    private static class Instance {
        private static final CRMCustomModularFieldColumnMappingCacheImpl INSTANCE = new CRMCustomModularFieldColumnMappingCacheImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link CRMCustomModularFieldColumnMappingCacheImpl}.
     * </p>
     *
     * @return The {@link CRMCustomModularFieldColumnMappingCacheImpl} instance
     */
    public static CRMCustomModularFieldColumnMappingCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        columnMappingManager.fetchAll().forEach(mapping -> {
            cacheById.put(mapping.id(), mapping);

            final Integer moduleId = mapping.getModuleId();
            cacheByModuleId.computeIfAbsent(moduleId, k -> new ArrayList<>()).add(mapping);
        });

        LOGGER.info("CRM Analytics : CRMCustomModularFieldColumnMapping loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheByModuleId.clear();
        cacheById.clear();
        LOGGER.info("CRM Analytics : CRMCustomModularFieldColumnMapping cache cleared");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<CRMCustomModularFieldColumnMapping> get(final Integer moduleId) {
        return cacheByModuleId.get(moduleId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CRMCustomModularFieldColumnMapping getById(final Long columnMappingId) {
        return cacheById.get(columnMappingId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final Integer moduleId, final Collection<CRMCustomModularFieldColumnMapping> mappings) {
        cacheByModuleId.put(moduleId, mappings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putById(final Long columnMappingId, final CRMCustomModularFieldColumnMapping mapping) {
        cacheById.put(columnMappingId, mapping);
    }
}
