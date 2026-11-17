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

    public Appointment() {
        super(ENTITY_TYPE);
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return "Appointment";
    }

    @Override
    public Object identity() {
        return null;
    }

    @Override
    public boolean notifiable() {
        return notifiable;
    }

    @Override
    public void setNotifiable(final boolean notifiable) {
        this.notifiable = notifiable;
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(final Long patientId) {
        this.patientId = patientId;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(final Long doctorId) {
        this.doctorId = doctorId;
    }

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(final DateTime startTime) {
        this.startTime = startTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(final Integer duration) {
        this.duration = duration;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public enum Status {
        SCHEDULED,
        COMPLETED,
        CANCELLED
    }
}
