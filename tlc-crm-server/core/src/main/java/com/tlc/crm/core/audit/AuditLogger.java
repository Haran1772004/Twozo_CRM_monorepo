package com.tlc.crm.core.audit;

import com.tlc.crm.core.audit.impl.AuditLoggerImpl;
import com.tlc.crm.entity.audit.log.model.AuditLogData;

import java.util.Collection;

/**
 * <p>
 * Services that provides the audit log related data
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
public interface AuditLogger {

    /**
     * <p>
     * Gets the {@link AuditLogger} instance
     * </p>
     *
     * @return The {@link AuditLoggerImpl} instance
     */
    static AuditLogger getInstance() {
        return AuditLoggerImpl.getInstance();
    }

    /**
     * <p>
     * Stores the given models into audit log
     * </p>
     *
     * @param orgId        Represents an organization id
     * @param auditLogData Represents tha {@link AuditLogData} details
     */
    void log(final long orgId, final Collection<AuditLogData> auditLogData);
}
