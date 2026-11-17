package com.tlc.crm.entity.cache.internal;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.entity.audit.log.manager.AuditManager;
import com.tlc.crm.entity.audit.log.model.AuditCategory;
import com.tlc.crm.entity.audit.log.model.AuditField;
import com.tlc.crm.entity.cache.audit.log.AuditFieldCache;

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
public class AuditFieldCacheImpl implements AuditFieldCache {

    private final Cache<String, Collection<AuditField>> auditFieldCache;

    private static final class Instance {
        private static final AuditFieldCacheImpl INSTANCE = new AuditFieldCacheImpl();
    }

    private AuditFieldCacheImpl() {
        auditFieldCache = CacheManager.getInstance().createCache();
    }

    public static AuditFieldCacheImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<AuditField> getByCategory(final AuditCategory auditCategory) {
        return auditFieldCache.get(auditCategory.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putAuditFields(final Map<String, Collection<AuditField>> auditFields) {
        auditFieldCache.putAll(auditFields);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void load() {
        auditFieldCache.putAll(AuditManager.getInstance().getAllFields());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        auditFieldCache.clear();
    }
}
