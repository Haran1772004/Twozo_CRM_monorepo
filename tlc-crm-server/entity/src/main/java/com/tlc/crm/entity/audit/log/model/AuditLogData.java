package com.tlc.crm.entity.audit.log.model;

import com.tlc.validator.TlcModel;

/**
 * <p>
 * Holds the audit log related details
 * </p>
 *
 * @param userId         Represents the user id
 * @param entityId       Represents the entity id
 * @param entityName     Represents the entity name
 * @param oldModel       Represents the old {@link TlcModel} details
 * @param newModel       Represents the new {@link TlcModel} details
 * @param auditSource    Represents the {@link AuditSource} details
 * @param auditAction    Represents the {@link AuditAction} details
 * @param auditAttribute Represents the audit attribute
 * @author pandiarajkumarG
 * @version 1.0
 */
public record AuditLogData(Long userId, Long entityId, String entityName, TlcModel oldModel, TlcModel newModel,
                           AuditSource auditSource, AuditAction auditAction, AuditAttribute auditAttribute) {

    public AuditLogData(final Long userId, final Long entityId, final String entityName, final AuditSource auditSource, final AuditAction auditAction, final AuditAttribute auditAttribute) {
        this(userId, entityId, entityName, null, null, auditSource, auditAction, auditAttribute);
    }

    public AuditLogData(final Long userId, final Long entityId, final TlcModel oldModel, final TlcModel newModel, final AuditSource auditSource, final AuditAction auditAction) {
        this(userId, entityId, null, oldModel, newModel, auditSource, auditAction, null);
    }

    public AuditLogData(final Long userId, final Long entityId, final TlcModel newModel, final AuditSource auditSource, final AuditAction auditAction){
        this(userId, entityId, null, null, newModel, auditSource, auditAction, null);
    }
}
