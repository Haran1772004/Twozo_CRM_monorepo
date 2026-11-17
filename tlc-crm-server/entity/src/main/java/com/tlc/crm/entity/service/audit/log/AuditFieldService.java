package com.tlc.crm.entity.service.audit.log;

import com.tlc.crm.entity.audit.log.model.AuditCategory;
import com.tlc.crm.entity.audit.log.model.AuditField;
import com.tlc.crm.entity.service.impl.AuditFieldServiceImpl;

import java.util.Collection;

/**
 * <p>
 * Provides the cache service of the audit fields.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface AuditFieldService {

    static AuditFieldService getInstance() {
        return AuditFieldServiceImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link AuditField} for the given {@link AuditCategory}.
     * </p>
     *
     * @return Collection of {@link AuditField}.
     */
    Collection<AuditField> getByCategory(final AuditCategory auditCategory);
}
