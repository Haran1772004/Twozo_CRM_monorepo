package com.tlc.crm.hospital.sql.resource;

/** Table and column names for the hospital patient table. */
public final class MHOPATIENT {

    /** Name of the patient table. */
    public static final String TABLE_NAME = "MHOPatient";
    /** Patient primary-key column. */
    public static final String ID = "ID";
    /** Organization identifier column. */
    public static final String ORG_ID = "ORG_ID";
    /** Record owner's user identifier column. */
    public static final String OWNER_ID = "OWNER_ID";
    /** Patient full-name column. */
    public static final String FULL_NAME = "FULL_NAME";
    /** Patient date-of-birth column. */
    public static final String DATE_OF_BIRTH = "DATE_OF_BIRTH";
    /** Patient phone-number column. */
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
    private MHOPATIENT() {
    }
}
