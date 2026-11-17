package com.tlc.crm.core.api.deal.model;

import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.validator.TlcModel;
import com.tlc.validator.type.Group.Create;
import com.tlc.validator.type.Group.Delete;
import com.tlc.validator.type.Group.Update;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Model which represents the Participant
 *
 * @author Sundar
 * @version 1.0
 */
public class Participant implements TlcModel {

    @NotNull(groups = {Update.class, Delete.class}, message = "i18n.participant.invalid.notnull.id")
    @Min(value = 1, groups = {Update.class, Delete.class}, message = "i18n.participant.invalid.id")
    private Long id;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.participant.invalid.notnull.dealId")
    @Min(value = 1, groups = {Create.class, Update.class}, message = "i18n.participant.invalid.dealId")
    private Long dealId;

    @NotNull(groups = {Create.class, Update.class, Delete.class}, message = "i18n.participant.invalid.notnull.orgId")
    @Min(value = 1, groups = {Create.class, Update.class}, message = "i18n.participant.invalid.orgId")
    private Long orgId;

    @NotNull(groups = {Create.class, Update.class}, message = "i18n.participant.invalid.notnull.contactId")
    @Min(value = 1, groups = {Create.class, Update.class}, message = "i18n.participant.invalid.contactId")
    private Long contactId;

    private Contact contact;

    @Override
    public Long id() {
        return id;
    }

    public Long orgId() {
        return orgId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Long getDealId() {
        return dealId;
    }

    public void setDealId(Long dealId) {
        this.dealId = dealId;
    }

    public void setOrgId(Long orgId) {
        this.orgId = orgId;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(final Contact contact) {
        this.contact = contact;
    }

    @Override
    public Object identity() {
        return null;
    }
}