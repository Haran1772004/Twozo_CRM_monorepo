package com.tlc.crm.entity.cache.audit.log;

import com.tlc.crm.common.cache.LocalCache;

import com.tlc.crm.entity.audit.log.model.AuditCategory;
import com.tlc.crm.entity.audit.log.model.AuditField;
import com.tlc.crm.entity.cache.internal.AuditFieldCacheImpl;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the cache service of the audit fields.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface AuditFieldCache extends LocalCache {

    static AuditFieldCache getInstance() {
        return AuditFieldCacheImpl.getInstance();
    }

    /**
     * <p>
     * Fetches all the {@link AuditField} are available in the cache.
     * </p>
     *
     * @return Collection of {@link AuditField}.
     */
    Collection<AuditField> getByCategory(final AuditCategory auditCategory);

    /**
     * <p>
     * Stores the {@link AuditField} into the cache.
     * </p>
     *
     * @param auditFields Map of Audit category and their fields.
     */
    void putAuditFields(final Map<String, Collection<AuditField>> auditFields);
}
