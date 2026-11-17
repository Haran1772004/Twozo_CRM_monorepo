package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.InboundEmail;

/**
 * <p>
 * Represents the inbound email.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class EntityInboundEmail extends EntityEmail {

    private boolean isRead;

    public boolean isRead() {
        return isRead;
    }

    public void setRead(final boolean isRead) {
        this.isRead = isRead;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link MessageCategory}
     */
    @Override
    public MessageCategory getMessageCategory() {
        return MessageCategory.INBOUND;
    }

    @Override
    public Email getEmail() {
        return new InboundEmail();
    }
}
