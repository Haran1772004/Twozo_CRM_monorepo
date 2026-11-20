package com.tlc.crm.hospital.sql.resource;

/** Table and column names for the hospital appointment table. */
public final class MHOAPPOINTMENT {

    /** Name of the appointment table. */
    public static final String TABLE_NAME = "MHOAppointment";
    /** Appointment primary-key column. */
    public static final String ID = "ID";
    /** Organization identifier column. */
    public static final String ORG_ID = "ORG_ID";
    /** Record owner's user identifier column. */
    public static final String OWNER_ID = "OWNER_ID";
    /** Identifier of the appointment's patient. */
    public static final String PATIENT_ID = "PATIENT_ID";
    /** Identifier of the appointment's doctor. */
    public static final String DOCTOR_ID = "DOCTOR_ID";
    /** Appointment start timestamp column. */
    public static final String START_TIME = "START_TIME";
    /** Appointment duration, in minutes. */
    public static final String DURATION = "DURATION";
    /** Appointment status column. */
    public static final String STATUS = "STATUS";
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
    private MHOAPPOINTMENT() {
    }
}
