package com.tlc.crm.hospital.sql.resource;

/** Table and column names for the hospital doctor table. */
public final class MHODOCTOR {

    /** Name of the doctor table. */
    public static final String TABLE_NAME = "MHODoctor";
    /** Doctor primary-key column. */
    public static final String ID = "ID";
    /** Organization identifier column. */
    public static final String ORG_ID = "ORG_ID";
    /** Record owner's user identifier column. */
    public static final String OWNER_ID = "OWNER_ID";
    /** Doctor full-name column. */
    public static final String FULL_NAME = "FULL_NAME";
    /** Doctor specialty column. */
    public static final String SPECIALTY = "SPECIALTY";
    /** Doctor phone-number column. */
    public static final String PHONE = "PHONE";
    /** Record creation timestamp column. */
    public static final String CREATED_TIME = "CREATED_TIME";
    /** Record modification timestamp column. */
    public static final String MODIFIED_TIME = "MODIFIED_TIME";
    /** Identifier of the user who created the record. */
    public static final String CREATED_BY = "CREATED_BY";
    /** Identifier of the user who last updated the record. */
    public static final String UPDATED_BY = "UPDATED_BY";
    /** Timestamp of the record's most recent assignment. */
    public static final String LAST_ASSIGNED_AT = "LAST_ASSIGNED_AT";

    /** Prevents instantiation of this constants class. */
    private MHODOCTOR() {
    }
}
