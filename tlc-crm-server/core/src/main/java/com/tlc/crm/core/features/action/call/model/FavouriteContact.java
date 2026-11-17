package com.tlc.crm.core.features.action.call.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.validator.TlcModel;

/**
 * <p>
 * Represents a favourite call contact entity
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class FavouriteContact implements TlcModel {

    private Long id;
    private Long orgId;
    private Long userId;
    private Contact contact;

    public FavouriteContact() {
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(final Contact contact) {
        this.contact = contact;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    @Override
    public Long id() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    @Override
    public Object identity() {
        throw ErrorCode.get(CoreErrorCodes.UNSUPPORTED_OPERATION);
    }
}
