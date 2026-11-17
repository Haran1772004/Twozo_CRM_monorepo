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

    public Patient() {
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
        return fullName;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(final String fullName) {
        this.fullName = fullName;
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(final Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(final String phone) {
        this.phone = phone;
    }
}
