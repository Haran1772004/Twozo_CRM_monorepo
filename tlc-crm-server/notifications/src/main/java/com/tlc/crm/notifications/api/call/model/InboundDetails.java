package com.tlc.crm.notifications.api.call.model;

import com.tlc.crm.common.iam.api.model.CrmUser;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents details of an inbound call, including the contact name and associated CRM users.
 *
 * @author Sathish
 * @version 1.0
 */
public class InboundDetails {

    private String contactName;
    private String receiverName;

    private final Collection<CrmUser> users;

    public InboundDetails() {
        this.users = new ArrayList<>();
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(final String contactName) {
        this.contactName = contactName;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(final String receiverName) {
        this.receiverName = receiverName;
    }

    public Collection<CrmUser> getUsers() {
        return users;
    }

    public void addUser(final CrmUser user) {
        this.users.add(user);
    }
}
