package com.tlc.crm.core.api.analytics.cache.internal;

import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMSystemModularFieldColumnMapping;
import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.core.api.analytics.cache.module.CRMSystemModularFieldColumnMappingCache;
import com.tlc.crm.core.api.analytics.manager.CRMSystemModularFieldColumnMappingManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Implementation of {@link CRMSystemModularFieldColumnMappingCache} using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public class CRMSystemModularFieldColumnMappingCacheImpl implements CRMSystemModularFieldColumnMappingCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMSystemModularFieldColumnMappingCacheImpl.class);
    private final Cache<Long, Collection<CRMSystemModularFieldColumnMapping>> cacheByModularFieldId;
    private final Cache<Long, CRMSystemModularFieldColumnMapping> cacheById;
    private final CRMSystemModularFieldColumnMappingManager columnMappingManager;

    /**
     * <p>
     * Private constructor initializing the local caches.
     * </p>
     */
    private CRMSystemModularFieldColumnMappingCacheImpl() {
        this.cacheByModularFieldId = CacheManager.getInstance().createCache();
        this.cacheById = CacheManager.getInstance().createCache();
        this.columnMappingManager = CRMSystemModularFieldColumnMappingManager.INSTANCE();
    }

    private static class Instance {
        private static final CRMSystemModularFieldColumnMappingCacheImpl INSTANCE = new CRMSystemModularFieldColumnMappingCacheImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link CRMSystemModularFieldColumnMappingCacheImpl}.
     * </p>
     *
     * @return The {@link CRMSystemModularFieldColumnMappingCacheImpl} instance
     */
    public static CRMSystemModularFieldColumnMappingCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        columnMappingManager.fetchAll().forEach(mapping -> {
            cacheById.put(mapping.id(), mapping);

            final Long modularFieldId = mapping.getModularFieldId();
            Collection<CRMSystemModularFieldColumnMapping> existing = cacheByModularFieldId.get(modularFieldId);

            if (existing == null) {
                existing = new ArrayList<>();
                cacheByModularFieldId.put(modularFieldId, existing);
            }

            existing.add(mapping);
        });

        LOGGER.info("CRM Analytics : CRMSystemModularFieldColumnMapping loaded into cache successfully");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cacheByModularFieldId.clear();
        cacheById.clear();
        LOGGER.info("CRM Analytics : CRMSystemModularFieldColumnMapping cache cleared");
    }

    /**
     * {@inheritDoc}
     *
     * @param modularFieldId The source/module identifier
     * @return The collection of {@link CRMSystemModularFieldColumnMapping} from cache
     */
    @Override
    public Collection<CRMSystemModularFieldColumnMapping> get(final Long modularFieldId) {
        return cacheByModularFieldId.get(modularFieldId);
    }

    /**
     * {@inheritDoc}
     *
     * @param id The mapping identifier
     * @return The {@link CRMSystemModularFieldColumnMapping} from cache
     */
    @Override
    public CRMSystemModularFieldColumnMapping getById(final Long id) {
        return cacheById.get(id);
    }

    /**
     * {@inheritDoc}
     *
     * @param modularFieldId The source/module identifier
     * @param mappings       The collection of {@link CRMSystemModularFieldColumnMapping} to cache
     */
    @Override
    public void put(final Long modularFieldId, final Collection<CRMSystemModularFieldColumnMapping> mappings) {
        cacheByModularFieldId.put(modularFieldId, mappings);
    }

    /**
     * {@inheritDoc}
     *
     * @param id      The mapping identifier
     * @param mapping The {@link CRMSystemModularFieldColumnMapping} to cache
     */
    @Override
    public void putById(final Long id, final CRMSystemModularFieldColumnMapping mapping) {
        cacheById.put(id, mapping);
    }
}
