package com.tlc.crm.core.api.contact.model;

import java.util.Collection;

/**
 * <p>
 * A model representing the contact information along with associated deal IDs.
 * </p>
 *
 * <p>
 * Provides a way to store and access a contact's details as well as
 * a collection of deal IDs linked to that contact.
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class ContactDealInfo {

    private Contact contact;
    private Collection<Long> dealIds;

    public Collection<Long> getDealIds() {
        return dealIds;
    }

    public Contact getContact() {
        return contact;
    }

    public void setDealIds(final Collection<Long> dealIds) {
        this.dealIds = dealIds;
    }

    public void setContact(final Contact contact) {
        this.contact = contact;
    }
}
