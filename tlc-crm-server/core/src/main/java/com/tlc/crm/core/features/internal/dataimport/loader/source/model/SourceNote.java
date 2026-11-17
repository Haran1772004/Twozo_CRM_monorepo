package com.tlc.crm.core.features.internal.dataimport.loader.source.model;

import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.internal.dataimport.validator.annotation.SourceNoteValidator;
import com.tlc.crm.entity.features.api.note.model.Note;
import com.tlc.validator.type.Group;

/**
 * Represents the {@link SourceNote} inheriting attributes from the {@link Note} and
 * establishing connections with associated sources while facilitating validation.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@SourceNoteValidator(groups = {Group.Create.class}, message = "i18n.import.note.invalid.source")
public final class SourceNote extends Note {

    private Contact contact;
    private Company company;
    private Deal deal;

    public Contact getContact() {
        return contact;
    }

    public void setContact(final Contact contact) {
        this.contact = contact;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(final Company company) {
        this.company = company;
    }

    public Deal getDeal() {
        return deal;
    }

    public void setDeal(final Deal deal) {
        this.deal = deal;
    }

}
