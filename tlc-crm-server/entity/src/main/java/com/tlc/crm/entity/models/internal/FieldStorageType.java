package com.tlc.crm.entity.models.internal;

/**
 * <p>
 * It represents different types of field storage modes available
 * </p>
 *
 * @author Selvakumar
 * @version 1.0
 */
public enum FieldStorageType {

    /**
     * If a field is transient, it's value will never be saved into database, and it won't have any allocated column
     * information. This kind of entity should be solely used for internal purpose
     */
    TRANSIENT,

    /**
     * If a field is persistent, it's value might be saved into database, and it should have an allocated column
     * information
     */
    PERSISTENT;
}
