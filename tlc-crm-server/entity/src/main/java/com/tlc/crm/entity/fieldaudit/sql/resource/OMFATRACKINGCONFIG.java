package com.tlc.crm.entity.fieldaudit.sql.resource;

/**
 * SQL constants for the {@code OMFATrackingConfig} table (per-source tracking config).
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public final class OMFATRACKINGCONFIG {

    public static final String TABLE = "OMFATrackingConfig";

    public static final String ID = "ID";
    public static final String ORG_ID = "ORG_ID";
    public static final String SOURCE_ID = "SOURCE_ID";
    public static final String TRACKED_FIELDS = "TRACKED_FIELDS";
    public static final String UPDATED_AT = "UPDATED_AT";
    public static final String UPDATED_BY = "UPDATED_BY";

    private OMFATRACKINGCONFIG() {
    }
}