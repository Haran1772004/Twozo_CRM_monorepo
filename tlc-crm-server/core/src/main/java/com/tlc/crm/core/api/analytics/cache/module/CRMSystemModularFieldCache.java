package com.tlc.crm.core.api.analytics.cache.module;

import com.tlc.crm.common.cache.LocalCache;
import com.tlc.crm.core.api.analytics.cache.internal.CRMSystemModularFieldCacheImpl;
import com.tlc.crm.core.api.analytics.entity.config.modules.CRMSystemModularField;

import java.util.Collection;

/**
 * Cache interface for {@link CRMSystemModularField} objects using local cache.
 *
 * @author Karthick V
 * @version 1.0
 */
public interface CRMSystemModularFieldCache extends LocalCache {

    /**
     * <p>Retrieves the singleton instance of {@link CRMSystemModularFieldCache}.</p>
     *
     * @return The {@link CRMSystemModularFieldCache} instance
     */
    static CRMSystemModularFieldCache getInstance() {
        return CRMSystemModularFieldCacheImpl.getInstance();
    }

    /**
     * <p>Synchronously retrieves cached system modular fields for the given module ID.</p>
     *
     * @param moduleId The module identifier
     * @return The collection of cached {@link CRMSystemModularField} objects
     */
    Collection<CRMSystemModularField> get(final Integer moduleId);

    /**
     * <p>Synchronously retrieves a single {@link CRMSystemModularField} by its ID.</p>
     *
     * @param fieldId The field identifier
     * @return The cached {@link CRMSystemModularField} object, or null if not found
     */
    CRMSystemModularField getById(final Long fieldId);

    /**
     * <p>Synchronously puts system modular fields into cache for the given module ID.</p>
     *
     * @param moduleId The module identifier
     * @param fields   The collection of {@link CRMSystemModularField} to cache
     */
    void put(final Integer moduleId, final Collection<CRMSystemModularField> fields);

    /**
     * <p>Synchronously puts a single {@link CRMSystemModularField} into cache by its ID.</p>
     *
     * @param fieldId The field identifier
     * @param field   The {@link CRMSystemModularField} to cache
     */
    void putById(final Long fieldId, final CRMSystemModularField field);
}
