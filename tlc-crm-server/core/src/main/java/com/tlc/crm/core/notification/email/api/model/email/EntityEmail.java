package com.tlc.crm.core.notification.email.api.model.email;

import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.conversation.api.model.EntityConversation;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.email.metrics.OutboundMetrics;

import com.tlc.crm.core.conversation.api.model.ConversableModel;
import com.tlc.crm.email.notification.email.EmailNotificationModel;
import com.tlc.crm.email.notification.email.api.model.TwozoEmail;

import com.tlc.crm.email.notification.email.api.model.address.FromAddressInfo;
import com.tlc.crm.email.notification.model.MessageDispatchType;
import com.tlc.crm.entity.fields.source.Source;

import com.tlc.crm.notification.app.AppNotificationEventType;
import com.tlc.crm.notification.app.Notifiable;
import com.tlc.crm.notification.app.CrmModelType;
import com.tlc.email.common.model.type.Email;
import com.tlc.file.common.models.Attachment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * <p>
 * Represents the attributes of entity related email.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see TwozoEmail
 */
@Notifiable(types = {AppNotificationEventType.EMAIL_RECEIVED, AppNotificationEventType.EMAIL_REPLIED}, modelName = CrmModelType.EMAIL, needHandling = true)
public class EntityEmail extends TwozoEmail implements EmailNotificationModel, ConversableModel {

    /*
      This ID represents the CRM-related email ID(Outbound/Inbound) and does not refer to the email service conversation ID.
     */
    private Long crmId;
    private Long orgId;
    private Long userId;
    private ContactRecipients contactRecipients;
    private Collection<Attachment> entityAttachments;
    private Long entityId;
    private Source source;
    private Contact contact;
    private boolean isSyncedState;
    private CrmUser user;

    /*
       This ID represents the CRM-related thread id and does not refer to the email service thread ID.
     */
    private Long threadId;
    /*
      This ID represents the CRM-related email conversation id and does not refer to the email service conversation ID.
    */
    private Long conversationId;
    private boolean hasAttachments;
    private MessageDispatchType messageDispatchType;
    private OutboundMetrics outboundMetrics;
    private Long parentConversationId;
    private EmailVariant variant = EmailVariant.of(EmailVariant.Type.SYNC_EMAIL);

    public Long getCrmId() {
        return crmId;
    }

    public void setCrmId(final Long crmId) {
        this.crmId = crmId;
    }

    @Override
    public Long orgId() {
        return orgId;
    }

    public void setOrgId(final Long orgId) {
        this.orgId = orgId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(final Long userId) {
        this.userId = userId;
    }

    public ContactRecipients getContactRecipients() {
        return contactRecipients;
    }

    public void setContactRecipients(final ContactRecipients contactRecipients) {
        this.contactRecipients = contactRecipients;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(final Long entityId) {
        this.entityId = entityId;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(final Source source) {
        this.source = source;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(final Contact contact) {
        this.contact = contact;
    }

    public void setSyncedState(final boolean syncedState) {
        isSyncedState = syncedState;
    }

    public Collection<Attachment> getEntityAttachments() {
        return entityAttachments;
    }

    public void setEntityAttachments(final Collection<Attachment> entityAttachments) {
        this.entityAttachments = entityAttachments;
    }

    public Collection<Long> getEntityAttachmentIds() {
        return Objects.nonNull(entityAttachments) && !entityAttachments.isEmpty() ? entityAttachments.stream().map(Attachment::id).filter(Objects::nonNull).toList() : Collections.emptyList();
    }

    public void addEntityAttachment(final Attachment attachment) {
        this.entityAttachments = this.entityAttachments == null ? new ArrayList<>() : entityAttachments;
        this.entityAttachments.add(attachment);
    }

    public boolean isSynced() {
        return isSyncedState;
    }

    public void setVariant(final EmailVariant emailVariant) {
        this.variant = emailVariant;
    }

    public EmailVariant getEmailVariant() {
        return variant;
    }

    public boolean hasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(final boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    public MessageDispatchType getMessageDispatchType() {
        return messageDispatchType;
    }

    public void setMessageDispatchType(final MessageDispatchType messageDispatchType) {
        this.messageDispatchType = messageDispatchType;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(final Long threadId) {
        this.threadId = threadId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(final Long conversationId) {
        this.conversationId = conversationId;
    }

    public void setOutboundMetrics(final OutboundMetrics outboundMetrics) {
        this.outboundMetrics = outboundMetrics;
    }

    public FromAddressInfo getSenderFromAddressInfo() {
        return getSender() instanceof FromAddressInfo fromAddressInfo ? fromAddressInfo : null;
    }

    public OutboundMetrics getOutboundMetrics() {
        return outboundMetrics;
    }

    @Override
    public Object identity() {
        return null;
    }

    /**
     * Returns the current instance cast to the specified type.
     *
     * @param <T> The type to cast the current instance to.
     * @return The current instance cast as the specified type.
     */
    public <T> T getAsType() {
        return (T) this;
    }

    /**
     * <p>
     * Gets the {@link EntityEmail} based on {@link MessageCategory}.
     * </p>
     *
     * @return {@link EntityEmail}
     */
    public static EntityEmail getMailBasedOnCategory(final MessageCategory messageCategory) {
        return switch (messageCategory) {
            case OUTBOUND -> new EntityOutboundEmail();
            case DRAFT -> new EntityDraftEmail();
            case SCHEDULED -> new EntityScheduledEmail();
            case INBOUND -> new EntityInboundEmail();
            default -> new EntityEmail();
        };
    }

    public Long getParentConversationId() {
        return parentConversationId;
    }

    public void setParentConversationId(final Long parentConversationId) {
        this.parentConversationId = parentConversationId;
    }

    public Email getEmail() {
        return new Email();
    }

    public void setUser(final CrmUser user) {
        this.user = user;
    }

    public CrmUser getUser() {
        return user;
    }

    /**
     * <p>
     * Represents the {@link Category} of {@link EntityConversation}
     * </p>
     *
     */
    public enum Category {
        OUTBOUND_MAIL,
        INBOUND_MAIL,
    }
}