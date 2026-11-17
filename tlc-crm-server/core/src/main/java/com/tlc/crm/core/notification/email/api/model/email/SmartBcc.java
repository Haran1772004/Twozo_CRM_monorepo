package com.tlc.crm.core.notification.email.api.model.email;

import java.util.Collection;

/**
 * <p>
 * A model which represents the smart bcc
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class SmartBcc extends EntityEmail {

    private Collection<String> emailIds;
    private Collection<Long> contactIds;
    private Long dealId;

    public SmartBcc() {
        this.setVariant(EmailVariant.of(EmailVariant.Type.SMART_BCC));
    }

    public Collection<String> getEmailIds() {
        return emailIds;
    }

    public Collection<Long> getContactIds() {
        return contactIds;
    }

    public Long getDealId() {
        return dealId;
    }

    public void setDealId(final Long dealId) {
        this.dealId = dealId;
    }

    public void setEmailIds(final Collection<String> emailIds) {
        this.emailIds = emailIds;
    }

    public void setContactIds(final Collection<Long> contactIds) {
        this.contactIds = contactIds;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @return {@link MessageCategory}
     */
    @Override
    public MessageCategory getMessageCategory() {
        return MessageCategory.OUTBOUND;
    }
}
