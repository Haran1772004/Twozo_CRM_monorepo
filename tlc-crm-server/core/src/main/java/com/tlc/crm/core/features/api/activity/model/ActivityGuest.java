package com.tlc.crm.core.features.api.activity.model;

import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.entity.models.type.Email;
import com.tlc.validator.TlcModel;
import jakarta.validation.Valid;

import java.io.Serializable;

/**
 * <p>
 * Model which represents the {@link ActivityGuest}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public class ActivityGuest implements TlcModel, Serializable {

    private long orgId;
    private long id;

    @EmailAlertField(
            fieldType = EmailAlertFieldType.TLC_MODEL
    )
    private Contact contact;

    @EmailAlertField (
        fieldType = EmailAlertFieldType.TLC_MODEL
    )
    @Valid
    private Email email;

    public Contact getContact() {
        return contact;
    }

    public void setContact(final Contact contact) {
        this.contact = contact;
    }

    public Email getEmail() {
        return email;
    }

    public void setEmail(final Email email) {
        this.email = email;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}
