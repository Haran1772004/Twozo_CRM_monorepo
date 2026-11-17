package com.tlc.crm.core.notification.email.bulk.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.bulk.api.model.metrics.Metrics.MetricsType;
import com.tlc.crm.core.notification.email.bulk.status.BulkMailErrorCodes;

import com.tlc.crm.email.notification.email.EmailNotificationModel;

import com.tlc.email.common.model.type.Email.MessageCategory;
import jakarta.validation.Valid;

import java.util.Collection;

/**
 * Represents the {@link MailInfo} that implements the {@link EntityEmail} and encapsulates the bulk mail information.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EntityEmail
 */
public final class MailInfo implements EmailNotificationModel {

    private Long id;
    private Long orgId;
    private Long senderId;

    @Valid
    private EntityOutboundEmail outboundEmail;
    private Status status;
    private boolean isPersonalized;
    private Collection<MetricsType> metricsTypes;
    private MessageCategory messageCategory;

    public boolean isPersonalized() {
        return isPersonalized;
    }

    public void setPersonalized(final boolean personalized) {
        isPersonalized = personalized;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(final Status status) {
        this.status = status;
    }

    public Collection<MetricsType> getMetricsTypes() {
        return metricsTypes;
    }

    public void setMetricsTypes(final Collection<MetricsType> metricsTypes) {
        this.metricsTypes = metricsTypes;
    }

    public void setOutboundEmail(EntityOutboundEmail outboundEmail) {
        this.outboundEmail = outboundEmail;
    }

    public EntityOutboundEmail getOutboundEmail() {
        return outboundEmail;
    }

    public void setMessageCategory(final MessageCategory messageCategory) {
        this.messageCategory = messageCategory;
    }

    public MessageCategory getMessageCategory() {
        return messageCategory;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(final Long senderId) {
        this.senderId = senderId;
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

    /**
     * Represents the {@link Status} of {@link BulkMail}
     *
     * @author KowsalyaSP
     */
    public enum Status {

        /**
         * Mails that have been successfully sent are marked as "subscribed".
         */
        SUBSCRIBED((short) 1),

        /**
         * Mails are categorized as "unsubscribed", when contact or email address is unsubscribed.
         */
        UNSUBSCRIBED((short) 2),

        /**
         * Mails are categorized as "non-primary", when the contact emails lack a primary email.
         */
        NO_EMAIL((short) 3),

        /**
         * Mails are categorized as "drafted" when the number of emails sent exceeds the daily limit.
         */
        DRAFTED((short) 4);

        private final short id;

        Status(final short id) {
            this.id = id;
        }

        public short id() {
            return id;
        }

        public static Status get(final short id) {
            return switch (id) {
                case 1 -> SUBSCRIBED;
                case 2 -> UNSUBSCRIBED;
                case 3 -> NO_EMAIL;
                case 4 -> DRAFTED;
                default -> throw ErrorCode.get(BulkMailErrorCodes.INVALID_BULK_MAIL_STATUS);
            };
        }
    }
}