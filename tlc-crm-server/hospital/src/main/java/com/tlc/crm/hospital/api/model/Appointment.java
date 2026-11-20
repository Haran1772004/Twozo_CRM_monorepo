package com.tlc.crm.hospital.api.model;

import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.validator.annotation.Field;
import com.tlc.crm.entity.validator.annotation.FieldsValidator;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Represents an appointment between a patient and a doctor.
 *
 * @author haris
 * @version 1.0
 */
@FieldsValidator(groups = {Create.class, Update.class}, message = "i18n.entity.fields.validation.failed")
public class Appointment extends Entity {

    /** Entity type identifier used by the CRM entity framework. */
    public static final String ENTITY_TYPE = "Appointment";

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.appointment.invalid.id")
    private Long id;

    @Field(aliasName = "patientId", autoValidate = false)
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.appointment.patient.required")
    private Long patientId;

    @Field(aliasName = "doctorId", autoValidate = false)
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.appointment.doctor.required")
    private Long doctorId;

    @Field(aliasName = "startTime", autoValidate = false)
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.appointment.startTime.required")
    private DateTime startTime;

    /** Appointment duration in minutes. */
    @Field(aliasName = "duration", autoValidate = false)
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.appointment.duration.required")
    @Min(value = 1, groups = {Create.class, Update.class}, message = "i18n.appointment.duration.invalid")
    private Integer duration;

    @Field(aliasName = "status", autoValidate = false)
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.appointment.status.required")
    private Status status;

    private boolean notifiable;

    /** Creates an appointment model with no field values set. */
    public Appointment() {
        super(ENTITY_TYPE);
    }

    /** {@inheritDoc} */
    @Override
    public Long id() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public void setId(final Long id) {
        this.id = id;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return "Appointment";
    }

    /** {@inheritDoc} */
    @Override
    public Object identity() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean notifiable() {
        return notifiable;
    }

    /** {@inheritDoc} */
    @Override
    public void setNotifiable(final boolean notifiable) {
        this.notifiable = notifiable;
    }

    /**
     * Returns the identifier of the patient for this appointment.
     *
     * @return the patient identifier
     */
    public Long getPatientId() {
        return patientId;
    }

    /**
     * Sets the identifier of the patient for this appointment.
     *
     * @param patientId the patient identifier
     */
    public void setPatientId(final Long patientId) {
        this.patientId = patientId;
    }

    /**
     * Returns the identifier of the doctor for this appointment.
     *
     * @return the doctor identifier
     */
    public Long getDoctorId() {
        return doctorId;
    }

    /**
     * Sets the identifier of the doctor for this appointment.
     *
     * @param doctorId the doctor identifier
     */
    public void setDoctorId(final Long doctorId) {
        this.doctorId = doctorId;
    }

    /**
     * Returns the appointment start time.
     *
     * @return the start time
     */
    public DateTime getStartTime() {
        return startTime;
    }

    /**
     * Sets the appointment start time.
     *
     * @param startTime the start time
     */
    public void setStartTime(final DateTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Returns the appointment duration in minutes.
     *
     * @return the duration in minutes
     */
    public Integer getDuration() {
        return duration;
    }

    /**
     * Sets the appointment duration in minutes.
     *
     * @param duration the duration in minutes
     */
    public void setDuration(final Integer duration) {
        this.duration = duration;
    }

    /**
     * Returns the appointment status.
     *
     * @return the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Sets the appointment status.
     *
     * @param status the status
     */
    public void setStatus(final Status status) {
        this.status = status;
    }

    /** Describes the lifecycle state of an appointment. */
    public enum Status {
        /** The appointment is scheduled and occupies its time slot. */
        SCHEDULED,
        /** The appointment has taken place. */
        COMPLETED,
        /** The appointment was cancelled. */
        CANCELLED
    }
}
