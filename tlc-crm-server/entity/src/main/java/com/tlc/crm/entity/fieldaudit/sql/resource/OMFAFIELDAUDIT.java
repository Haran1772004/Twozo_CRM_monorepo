package com.tlc.crm.entity.fieldaudit.sql.resource;

/**
 * SQL constants for the {@code OMFieldAudit} table (Field Audit rows).
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class OMFAFIELDAUDIT {

    public static final String TABLE = "OMFAFieldAudit";

    public static final String ID = "ID";
    public static final String ORG_ID = "ORG_ID";
    public static final String SOURCE_ID = "SOURCE_ID";
    public static final String ENTITY_ID = "ENTITY_ID";
    public static final String FIELD_ID = "FIELD_ID";
    public static final String PREVIOUS_VALUE = "PREVIOUS_VALUE";
    public static final String NEW_VALUE = "NEW_VALUE";
    public static final String UPDATED_BY = "UPDATED_BY";
    public static final String UPDATED_AT = "UPDATED_AT";
    public static final String EDIT_SOURCE_ID = "EDIT_SOURCE_ID";
    public static final String EDIT_REFERENCE_ID = "EDIT_REFERENCE_ID";
    public static final String METADATA = "METADATA";

    private OMFAFIELDAUDIT() {
    }
}