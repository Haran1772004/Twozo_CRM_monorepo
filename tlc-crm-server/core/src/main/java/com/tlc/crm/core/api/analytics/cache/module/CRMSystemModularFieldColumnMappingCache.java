package com.tlc.crm.core.api.analytics.cache.module;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.core.api.analytics.cache.internal.CRMSystemModularFieldColumnMappingCacheImpl;
import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMSystemModularFieldColumnMapping;

import java.util.Collection;

/**
 * Cache interface for {@link CRMSystemModularFieldColumnMapping} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMSystemModularFieldColumnMappingCache extends LocalCache {

    /**
     * <p>Retrieves the singleton instance of {@link CRMSystemModularFieldColumnMappingCache}.</p>
     *
     * @return The {@link CRMSystemModularFieldColumnMappingCache} instance
     */
    static CRMSystemModularFieldColumnMappingCache getInstance() {
        return CRMSystemModularFieldColumnMappingCacheImpl.getInstance();
    }

    /**
     * <p>Retrieves cached system modular field column mappings for the given source/module ID.</p>
     *
     * @param modularFieldId The modular field identifier
     * @return The collection of {@link CRMSystemModularFieldColumnMapping} from cache
     */
    Collection<CRMSystemModularFieldColumnMapping> get(final Long modularFieldId);

    /**
     * <p>Retrieves a single {@link CRMSystemModularFieldColumnMapping} by its ID.</p>
     *
     * @param mappingId The mapping identifier
     * @return The {@link CRMSystemModularFieldColumnMapping} from cache
     */
    CRMSystemModularFieldColumnMapping getById(final Long mappingId);

    /**
     * <p>Puts system modular field column mappings into cache for the given source/module ID.</p>
     *
     * @param modularFieldId The modular field identifier
     * @param mappings The collection of {@link CRMSystemModularFieldColumnMapping} to cache
     */
    void put(final Long modularFieldId, final Collection<CRMSystemModularFieldColumnMapping> mappings);

    /**
     * <p>Puts a single {@link CRMSystemModularFieldColumnMapping} into cache by its ID.</p>
     *
     * @param mappingId The mapping identifier
     * @param mapping   The {@link CRMSystemModularFieldColumnMapping} to cache
     */
    void putById(final Long mappingId, final CRMSystemModularFieldColumnMapping mapping);
}
