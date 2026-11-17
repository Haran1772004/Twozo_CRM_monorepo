package com.tlc.crm.entity.resolver.impl;

import com.tlc.crm.entity.audit.log.manager.AuditManager;
import com.tlc.crm.entity.audit.log.model.AuditCategory;
import com.tlc.crm.entity.audit.log.model.AuditField;
import com.tlc.crm.entity.cache.audit.log.AuditFieldCache;
import com.tlc.crm.entity.resolver.audit.log.AuditFieldResolver;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * A resolver which resolves of the audit fields.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class AuditFieldResolverImpl implements AuditFieldResolver {

    private final AuditFieldCache auditFieldCache;

    private static final class Instance {
        private static final AuditFieldResolverImpl INSTANCE = new AuditFieldResolverImpl();
    }

    private AuditFieldResolverImpl() {
        auditFieldCache = AuditFieldCache.getInstance();
    }

    public static AuditFieldResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<AuditField> getByCategory(final AuditCategory auditCategory) {
        final Collection<AuditField> auditFields = auditFieldCache.getByCategory(auditCategory);

        if (Objects.isNull(auditFields)) {
            final Map<String, Collection<AuditField>> fields = AuditManager.getInstance().getAllFields();

            auditFieldCache.putAuditFields(fields);
            return fields.get(auditCategory.getName());
        }

        return auditFields;
    }
}
