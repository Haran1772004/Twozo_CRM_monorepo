package com.tlc.crm.entity.audit.log.model;

import com.tlc.l10n.temporal.model.DateTime;

import java.util.Collection;

/**
 * <p>
 * A model that represents the audit details.
 * </p>
 *
 * @author Abishek
 * @version 1.0
 */
public record AuditLog(Long userId, DateTime actionTime, AuditAction action, Collection<AuditAttribute> auditAttributes) {

    public AuditLog(final Long userId, final DateTime actionTime, final AuditAction auditAction) {
        this(userId, actionTime, auditAction, null);
    }
}
