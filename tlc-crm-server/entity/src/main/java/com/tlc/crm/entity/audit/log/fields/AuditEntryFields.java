package com.tlc.crm.entity.audit.log.fields;

import com.tlc.crm.entity.audit.log.model.AuditLog;

/**
 * <p>
 * Provides the fields for {@link AuditLog}.
 * </P>
 *
 * @author Sathish
 * @version 1.0
 */
public enum AuditEntryFields {

    ID("id"),
    ORG_ID("orgId"),
    USER_ID("userId"),
    USER_IDS("userIds"),
    NAME("name"),
    ACTION_TIME("actionTime"),
    ACTION("action"),
    SUB_ACTION("subAction"),
    TYPE("type"),
    ENTITY_ID("entityId"),
    ENTITY("entity"),
    ENTITY_TYPE("entityType"),
    AUDIT_ATTRIBUTE("auditAttribute"),
    FIELD_NAME("fieldName"),
    OLD_VALUE("oldValue"),
    NEW_VALUE("newValue"),
    AUDIT_LOG("audit_log"),
    AUDIT_ENTITY("audit_entity"),
    ICON_NAME("iconName"),
    DATE("date"),
    DATA("data"),
    TOTAL_UPDATE_COUNT("totalUpdatesCount"),
    LOGS_COUNT("logsCount"),
    UPDATES_COUNT("updatesCount"),
    STORE_TYPE("storeType"),
    ;

    private final String name;

    AuditEntryFields(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
