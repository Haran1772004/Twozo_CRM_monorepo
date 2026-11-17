package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.crm.core.notification.email.template.api.model.placeholder.MailPlaceHolder;

import com.tlc.crm.email.notification.email.EmailNotificationModel;
import com.tlc.crm.email.notification.email.tracking.model.EmailTrackingDetail;

import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.common.model.type.OutboundEmail.OutboundType;

import java.util.ArrayList;
import java.util.Collection;

import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;

/**
 * <p>
 * Represents the outbound email.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class EntityOutboundEmail extends EntityEmail {

    private Boolean isBounced;
    private OutboundType outboundType;
    private Long draftedConversationId;
    private boolean isDraftEnabled;
    private Collection<MailPlaceHolder> mailPlaceHolders;
    private Boolean isScheduled;
    private Long templateId;
    private Collection<EmailTrackingDetail> emailTrackingDetails;
    private boolean isUnsubscribed;

    public boolean isBounced() {
        return isBounced;
    }

    public void setBounced(final boolean isBounced) {
        this.isBounced = isBounced;
    }

    public OutboundType getOutboundType() {
        return outboundType;
    }

    public void setOutboundType(final OutboundType outboundType) {
        this.outboundType = outboundType;
    }

    public Long getDraftedConversationId() {
        return draftedConversationId;
    }

    public void setDraftedConversationId(final Long draftedConversationId) {
        this.draftedConversationId = draftedConversationId;
    }

    public boolean isDraftEnabled() {
        return isDraftEnabled;
    }

    public void setDraftEnabled(final boolean isDraftEnabled) {
        this.isDraftEnabled = isDraftEnabled;
    }

    public Collection<MailPlaceHolder> getMailPlaceHolders() {
        return mailPlaceHolders;
    }

    public void setMailPlaceHolders(final Collection<MailPlaceHolder> mailPlaceHolders) {
        this.mailPlaceHolders = mailPlaceHolders;
    }

    public Boolean isScheduled() {
        return isScheduled;
    }

    public void setScheduled(final Boolean scheduled) {
        isScheduled = scheduled;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(final Long templateId) {
        this.templateId = templateId;
    }

    public Collection<EmailTrackingDetail> getEmailTrackingDetails() {
        return emailTrackingDetails;
    }

    public void setEmailTrackingDetails(final Collection<EmailTrackingDetail> emailTrackingDetails) {
        this.emailTrackingDetails = emailTrackingDetails;
    }

    public void addEmailTrackingDetail(final EmailTrackingDetail emailTrackingDetail) {
        this.emailTrackingDetails = this.emailTrackingDetails == null ? new ArrayList<>() : emailTrackingDetails;

        this.emailTrackingDetails.add(emailTrackingDetail);
    }

    public boolean isUnsubscribed() {
        return isUnsubscribed;
    }

    public void setUnsubscribed(final boolean unsubscribed) {
        isUnsubscribed = unsubscribed;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link MessageCategory}
     */
    @Override
    public MessageCategory getMessageCategory() {
        return OUTBOUND;
    }

    /**
     * {@inheritDoc}
     *
     * @return {@link Email}
     */
    @Override
    public Email getEmail() {
        final OutboundEmail email = new OutboundEmail();

        email.setOutboundType(this.outboundType);

        return email;
    }
}
