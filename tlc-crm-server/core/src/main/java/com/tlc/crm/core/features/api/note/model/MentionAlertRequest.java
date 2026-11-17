package com.tlc.crm.core.features.api.note.model;

import com.tlc.crm.common.email.alert.annotation.EmailAlertField;
import com.tlc.crm.common.email.alert.annotation.EmailAlertTemplate;
import com.tlc.crm.common.email.alert.models.EmailAlertFieldType;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.core.api.contact.model.Company;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.deal.model.Deal;
import com.tlc.crm.core.features.api.activity.model.ActivityHolder;
import com.tlc.crm.entity.features.api.note.model.Message;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Objects;

/**
 * <p
 * Represents the model for note email notifications
 * </p>
 *
 * @author  Mohammed Asif J
 * @version 1.0
 */
@EmailAlertTemplate(templates = {EmailNotificationTemplate.CONTACT_NOTE_MENTION, EmailNotificationTemplate.DEAL_NOTE_MENTION,
        EmailNotificationTemplate.COMPANY_NOTE_MENTION, EmailNotificationTemplate.ACTIVITY_NOTE_MENTION, EmailNotificationTemplate.CONTACT_NOTE_COMMENT_MENTION,
        EmailNotificationTemplate.DEAL_NOTE_COMMENT_MENTION, EmailNotificationTemplate.COMPANY_NOTE_COMMENT_MENTION})
public class MentionAlertRequest implements EmailNotifiableModel, TlcModel {

    private Long id;
    private Message message;
    private ActivityHolder activityHolder;

    @EmailAlertField(
            name = "mentionedUserId",
            isUserField = true,
            fieldType = EmailAlertFieldType.NUMBER,
            recipientTemplates = {EmailNotificationTemplate.CONTACT_NOTE_MENTION, EmailNotificationTemplate.DEAL_NOTE_MENTION,
                    EmailNotificationTemplate.COMPANY_NOTE_MENTION, EmailNotificationTemplate.ACTIVITY_NOTE_MENTION, EmailNotificationTemplate.CONTACT_NOTE_COMMENT_MENTION,
                    EmailNotificationTemplate.DEAL_NOTE_COMMENT_MENTION, EmailNotificationTemplate.COMPANY_NOTE_COMMENT_MENTION},
            placeholderTemplates = {EmailNotificationTemplate.CONTACT_NOTE_MENTION, EmailNotificationTemplate.DEAL_NOTE_MENTION,
                    EmailNotificationTemplate.COMPANY_NOTE_MENTION, EmailNotificationTemplate.ACTIVITY_NOTE_MENTION, EmailNotificationTemplate.CONTACT_NOTE_COMMENT_MENTION,
                    EmailNotificationTemplate.DEAL_NOTE_COMMENT_MENTION, EmailNotificationTemplate.COMPANY_NOTE_COMMENT_MENTION}
    )
    private Long mentionedUserId;
    private Collection<Contact> contacts;
    private Deal deal;
    private Company company;

    public ActivityHolder getActivityHolder() {
        return activityHolder;
    }

    public void setActivityHolder(ActivityHolder activityHolder) {
        this.activityHolder = activityHolder;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(final Message message) {
        this.message = message;
    }

    public Collection<Contact> getContacts() {
        return contacts;
    }

    public void setContacts(final Collection<Contact> contacts) {
        this.contacts = contacts;
    }

    public Deal getDeal() {
        return deal;
    }

    public void setDeal(final Deal deal) {
        this.deal = deal;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(final Company company) {
        this.company = company;
    }

    public Long getMentionedUserId() {
        return mentionedUserId;
    }

    public void setMentionedUserId(final Long mentionedUserId) {
        this.mentionedUserId = mentionedUserId;
    }

    /**
     * @return
     */
    @Override
    public Long id() {

        if (Objects.nonNull(message)) {
            return message.id();
        }
        return this.id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * @return
     */
    @Override
    public Long orgId() {
        return message.orgId();
    }

    /**
     * @return
     */
    @Override
    public Object identity() {
        return message.identity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDateTimeField() {
        return true;
    }
}