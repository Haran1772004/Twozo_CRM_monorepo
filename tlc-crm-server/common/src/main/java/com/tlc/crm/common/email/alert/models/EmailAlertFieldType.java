package com.tlc.crm.common.email.alert.models;

/**
 * <p>
 * Holds the different types of email alert field
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public enum EmailAlertFieldType {

    TLC_MODEL,
    DATE_TIME,
    COLLECTION,
    STRING,
    NUMBER,
    DEFAULT_FIELD_TYPE,
    ;

    /**
     * <p>
     * Determines if the current field value type is default types
     * </p>
     *
     * @return true if the field value type is default type, false otherwise.
     */
    public boolean isDefaultFieldValueType() {
        return switch (this) {
            case NUMBER, STRING, COLLECTION -> true;
            default -> false;
        };
    }
}
