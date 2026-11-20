package com.tlc.crm.hospital.api.model;

import com.tlc.crm.entity.models.Entity;
import com.tlc.crm.entity.validator.annotation.Field;
import com.tlc.crm.entity.validator.annotation.FieldsValidator;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents a hospital patient.
 *
 * @author haris
 * @version 1.0
 */
@FieldsValidator(groups = {Create.class, Update.class}, message = "i18n.entity.fields.validation.failed")
public class Patient extends Entity {

    /** Entity type identifier used by the CRM entity framework. */
    public static final String ENTITY_TYPE = "Patient";

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.patient.invalid.id")
    private Long id;

    @Field(aliasName = "fullName")
    @NotNull(groups = {Create.class, Update.class}, message = "i18n.patient.name.required")
    @Size(max = 255, groups = {Create.class, Update.class}, message = "i18n.patient.name.length.exceeded")
    private String fullName;

    @Field(aliasName = "dateOfBirth", autoValidate = false)
    private Date dateOfBirth;

    @Field(aliasName = "phone")
    @Size(max = 32, groups = {Create.class, Update.class}, message = "i18n.patient.phone.length.exceeded")
    private String phone;

    private boolean notifiable;

    /** Creates a patient model with no field values set. */
    public Patient() {
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
        return fullName;
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
     * Returns the patient's full name.
     *
     * @return the full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Sets the patient's full name.
     *
     * @param fullName the full name
     */
    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    /**
     * Returns the patient's date of birth.
     *
     * @return the date of birth, or {@code null} if it is not set
     */
    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    /**
     * Sets the patient's date of birth.
     *
     * @param dateOfBirth the date of birth, or {@code null} to clear it
     */
    public void setDateOfBirth(final Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    /**
     * Returns the patient's phone number.
     *
     * @return the phone number, or {@code null} if it is not set
     */
    public String getPhone() {
        return phone;
    }

    /**
     * Sets the patient's phone number.
     *
     * @param phone the phone number, or {@code null} to clear it
     */
    public void setPhone(final String phone) {
        this.phone = phone;
    }
}
