package com.tlc.crm.notifications.api.model;

import com.tlc.notification.model.identity.ReceiverIdentity;
import com.tlc.notification.model.identity.SenderIdentity;

import com.tlc.validator.TlcModel;

/**
 * <p>
 * Holds the user details of notification sender and receiver
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class NotificationUser implements SenderIdentity, ReceiverIdentity, TlcModel {

    private Long id;
    private long orgId;

    @Override
    public Long id() {
        return id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final long orgId) {
        this.orgId = orgId;
    }

    @Override
    public Object identity() {
        return null;
    }
}
