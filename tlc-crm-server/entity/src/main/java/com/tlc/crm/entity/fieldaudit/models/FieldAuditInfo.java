package com.tlc.crm.entity.fieldaudit.models;

import com.tlc.commons.json.JsonObject;

/**
 * Context object that carries the source information for field audit tracking.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public record FieldAuditInfo(FieldAuditSource fieldAuditSource, Long sourceReferenceId, JsonObject metadata) {

    private static final FieldAuditInfo DEFAULT = new FieldAuditInfo(FieldAuditSource.MANUAL, null, null);

    public FieldAuditInfo(final FieldAuditSource changeSource) {
        this(changeSource, null, null);
    }

    /**
     * Returns the default context representing a manual user change.
     */
    public static FieldAuditInfo defaultContext() {
        return DEFAULT;
    }
}