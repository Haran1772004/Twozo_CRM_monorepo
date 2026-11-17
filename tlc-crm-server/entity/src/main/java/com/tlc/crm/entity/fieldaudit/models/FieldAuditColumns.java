package com.tlc.crm.entity.fieldaudit.models;

import com.tlc.crm.entity.fieldaudit.keys.FieldAuditKeys;

/**
 * Supported columns for field audit records.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum FieldAuditColumns {

    FIELD_NAME("fieldName", "i18n.field.audit.field.name", FieldAuditKeys.SINGLE_STRING, false),
    PREVIOUS_VALUE("previousValue", "i18n.field.audit.previous.value", FieldAuditKeys.FIELD_VALUE, false),
    CHANGED_VALUE("changedValue", "i18n.field.audit.changed.value", FieldAuditKeys.FIELD_VALUE, false),
    UPDATED_BY("updatedBy", "i18n.field.audit.updated.by", FieldAuditKeys.SINGLE_STRING, false),
    UPDATED_ON("updatedOn", "i18n.field.audit.updated.on", FieldAuditKeys.DATE_TIME, true),
    UPDATED_VIA("updatedVia", "i18n.field.audit.updated.via", FieldAuditKeys.SINGLE_STRING, false),
    ;

    private final String columnName;
    private final String displayName;
    private final String type;
    private final boolean isSortEnabled;

    FieldAuditColumns(final String columnName, final String displayName, final String type, final boolean isSortEnabled) {
        this.columnName = columnName;
        this.displayName = displayName;
        this.type = type;
        this.isSortEnabled = isSortEnabled;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getType() {
        return type;
    }

    public boolean isSortEnabled() {
        return isSortEnabled;
    }
}