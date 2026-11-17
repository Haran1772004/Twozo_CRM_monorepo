package com.tlc.crm.entity.audit.log.model;

/**
 * <p>
 * A model that represents the audit action.
 * </p>
 *
 * @author Abishek
 * @version 1.0
 */
public enum AuditAction {

    /**
     * #ADDED   -> Describes an action that relates to addition of an entity.
     * #UPDATED -> Describes an action that relates to update of an entity.
     * #DELETED -> Describes an action that relates to deletion of an entity.
     * #REMOVED -> Describes an action that relates to removal of field value from an entity.
     */
    ADDED, UPDATED, DELETED, REMOVED, ARCHIVED, UNARCHIVED;

    public boolean needFieldChangesInfo() {
        return this == UPDATED;
    }
}
