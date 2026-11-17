package com.tlc.crm.entity.resolver.audit.log;

import com.tlc.crm.entity.audit.log.model.AuditCategory;
import com.tlc.crm.entity.audit.log.model.AuditField;
import com.tlc.crm.entity.resolver.impl.AuditFieldResolverImpl;

import java.util.Collection;

/**
 * <p>
 * A resolver which resolves of the audit fields.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface AuditFieldResolver {

    static AuditFieldResolver getInstance() {
        return AuditFieldResolverImpl.getInstance();
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
