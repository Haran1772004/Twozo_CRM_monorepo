package com.tlc.crm.entity.service.impl;

import com.tlc.crm.entity.audit.log.model.AuditCategory;
import com.tlc.crm.entity.audit.log.model.AuditField;
import com.tlc.crm.entity.resolver.audit.log.AuditFieldResolver;
import com.tlc.crm.entity.service.audit.log.AuditFieldService;

import java.util.Collection;

/**
 * <p>
 * Provides the cache service of the audit fields.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class AuditFieldServiceImpl implements AuditFieldService {

    private final AuditFieldResolver auditFieldResolver;

    private static final class Instance {
        private static final AuditFieldServiceImpl INSTANCE = new AuditFieldServiceImpl();
    }

    private AuditFieldServiceImpl() {
        auditFieldResolver = AuditFieldResolver.getInstance();
    }

    public static AuditFieldServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<AuditField> getByCategory(final AuditCategory auditCategory) {
        return auditFieldResolver.getByCategory(auditCategory);
    }
}
