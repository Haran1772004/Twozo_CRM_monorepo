package com.tlc.crm.core.api.analytics.cache.module;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.core.api.analytics.cache.internal.CRMCustomModularFieldColumnMappingCacheImpl;
import com.tlc.crm.core.api.analytics.entity.integ.reports.CRMCustomModularFieldColumnMapping;

import java.util.Collection;

/**
 * Cache interface for {@link CRMCustomModularFieldColumnMapping} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMCustomModularFieldColumnMappingCache extends LocalCache {

    /**
     * <p>Retrieves the singleton instance of {@link CRMCustomModularFieldColumnMappingCache}.</p>
     *
     * @return The {@link CRMCustomModularFieldColumnMappingCache} instance
     */
    static CRMCustomModularFieldColumnMappingCache getInstance() {
        return CRMCustomModularFieldColumnMappingCacheImpl.getInstance();
    }

    /**
     * <p>Synchronously retrieves cached custom modular field column mappings for the given module ID.</p>
     *
     * @param moduleId The module identifier
     * @return The collection of cached {@link CRMCustomModularFieldColumnMapping} objects
     */
    Collection<CRMCustomModularFieldColumnMapping> get(final Integer moduleId);

    /**
     * <p>Synchronously retrieves a single {@link CRMCustomModularFieldColumnMapping} by its ID.</p>
     *
     * @param columnMappingId The column mapping identifier
     * @return The cached {@link CRMCustomModularFieldColumnMapping} object, or null if not found
     */
    CRMCustomModularFieldColumnMapping getById(final Long columnMappingId);

    /**
     * <p>Puts custom modular field column mappings into cache for the given module ID.</p>
     *
     * @param moduleId The module identifier
     * @param mappings The collection of {@link CRMCustomModularFieldColumnMapping} to cache
     */
    void put(final Integer moduleId, final Collection<CRMCustomModularFieldColumnMapping> mappings);

    /**
     * <p>Puts a single {@link CRMCustomModularFieldColumnMapping} into cache by its ID.</p>
     *
     * @param columnMappingId The column mapping identifier
     * @param mapping         The {@link CRMCustomModularFieldColumnMapping} to cache
     */
    void putById(final Long columnMappingId, final CRMCustomModularFieldColumnMapping mapping);
}
