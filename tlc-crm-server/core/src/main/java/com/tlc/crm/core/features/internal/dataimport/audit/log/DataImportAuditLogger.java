package com.tlc.crm.core.features.internal.dataimport.audit.log;

import com.tlc.crm.core.features.internal.dataimport.audit.log.impl.DataImportAuditLoggerImpl;
import com.tlc.crm.core.features.internal.dataimport.container.AuditLogContainer;

import java.util.Collection;

/**
 * <p>
 * Manages the audit entries for data imports, providing services for {@link DataImportAuditLogger}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface DataImportAuditLogger {

    /**
     * <p>
     * Gets the {@link DataImportAuditLogger} instance
     * </p>
     *
     * @return The {@link DataImportAuditLoggerImpl} instance
     */
    static DataImportAuditLogger getInstance() {
        return DataImportAuditLoggerImpl.getInstance();
    }

    /**
     * <p>
     * Processes the imported models for the audit log related operations
     * </p>
     *
     * @param orgId             Represents an organization id
     * @param userId            Represents an user id
     * @param auditLogContainer Which contains imported data loaded into the {@link AuditLogContainer}
     * @param invalidIndices    Which contains the indices of invalid models.
     */
    void log(final Long orgId, final Long userId, final AuditLogContainer auditLogContainer, final Collection<Integer> invalidIndices);
}
