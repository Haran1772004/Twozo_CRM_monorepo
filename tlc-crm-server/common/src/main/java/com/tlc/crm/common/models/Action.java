package com.tlc.crm.common.models;

/**
 * <p>
 * Dictates the course of action.
 * </p>
 *
 * @author Ajay.
 * @version 1.0
 */
public enum Action {
    CREATE,
    UPDATE,
    DELETE,
    PARTIAL_UPDATE,
    GET,
    APPLY,
    MERGE;

    public boolean isCreateOrUpdate() {
        return CREATE.equals(this) || UPDATE.equals(this);
    }

    public boolean isCreate() {
        return CREATE == this;
    }

    public boolean isUpdate() {
        return UPDATE == this;
    }
}
