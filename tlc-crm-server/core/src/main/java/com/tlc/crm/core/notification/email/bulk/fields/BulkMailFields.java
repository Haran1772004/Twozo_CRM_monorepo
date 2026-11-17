package com.tlc.crm.core.notification.email.bulk.fields;

import com.tlc.crm.entity.models.FieldNameHolder;

import com.tlc.crm.core.notification.email.bulk.api.model.BulkMail;

/**
 * Represents the {@link BulkMail} related fields
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see FieldNameHolder
 */
public enum BulkMailFields implements FieldNameHolder {

    ORG_ID("orgId"),
    SENDER_ID("senderId"),
    SOURCE("source"),
    ID("id"),
    IDS("ids"),
    TYPE("type"),
    DATA("data"),
    NAME("name"),
    STATUS("status"),
    PERSONALIZE("personalize"),
    EDIT("edit"),
    FROM("from"),
    TO_ADDRESS("toAddress"),
    SUBJECT("subject"),
    EMAIL_ID("emailId"),
    TOTAL_BULK_MAIL_USAGE("totalBulkMailUsage"),
    DEAL_IDS("dealIds"),
    IS_PERSONALIZED("isPersonalize"),
    TOTAL_RECIPIENTS("totalRecipients"),
    CONTACT_EMAIL_ID("contactEmailId"),
    CONVERSATION_ID("conversationId"),
    CONTACT_ID("contactId"),
    BULK_MAIL("bulkMail"),
    UPDATED_TIME("updatedTime"),
    ENTITY_IDS("entityIds"),
    RECIPIENTS("recipients"),
    EMAIL("email"),
    CONTACT_EMAILS("contactEmails"),
    EMAILS("emails"),
    MESSAGE("message"),
    DRAFT_ACTION("draftAction"),
    DRAFT_ID("draftId"),
    START("start"),
    LIMIT("limit"),
    HAS_MORE("hasMore"),
    LIST("list"),
    TOTAL_RECORDS("totalRecords"),
    REASON("reason"),
    UPDATED_RECORDS("updatedRecords"),
    FAILED_RECORDS("failedRecords"),
    ACTION("action"),
    CREATED_RECORDS("createdRecords"),
    INDEX("index"),
    VALIDATION_ERRORS("validationErrors"),
    SENT("sent"),
    UNSUBSCRIBED("unSubscribed"),
    REPLIED("replied"),
    OPENED("opened"),
    CLICKED("clicked"),
    BOUNCED("bounced"),
    OPENED_NOT_CLICKED("openedNotClicked"),
    OPENED_NOT_REPLIED("openedNotReplied"),
    NOT_SENT("notSent"),
    NOT_OPENED("notOpened"),
    NO_EMAIL("noEmail"),
    METRICS("metrics"),
    CREATED_TIME("createdTime"),
    SCHEDULED_TIME("scheduledTime"),
    SEND_STATUS("sendStatus"),
    IS_CANCELLED("isCancelled"),
    IS_DELETED("isDeleted"),
    TEMPLATE_ID("templateId"),
    FROM_ADDRESS("fromAddress"),
    HAS_UNSUBSCRIBED_LINK("hasUnsubscribedLink"),
    VALUE("value"),
    USER_ID("userId"),
    IS_BULK("isBulk"),
    BULK_MAIL_ID("bulkMailId"),
    MESSAGE_CATEGORY("messageCategory"),
    ATTACHMENTS("attachments"),
    TIMESTAMP("timestamp"),
    IGNORE_UNSUBSCRIBED("ignoreUnsubscribed"),
    CONTACT("contact"),
    DEAL_ID("dealId"),
    IS_PERSONALISED("isPersonalised"),
    ATTACHMENT_IDS("attachmentIds"),
    IS_EMAIL_TRACKER_ENABLED("isEmailTrackerEnabled"),
    IS_LINK_TRACKER_ENABLED("isLinkTrackerEnabled"),
    IS_CONTACT_LINKING_ENABLED("isContactLinkingEnabled"),
    IS_DEAL_LINKING_ENABLED("isDealLinkingEnabled")
    ;

    private final String fieldName;

    BulkMailFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    /**
     * {@inheritDoc}
     *
     * @return name of field
     */
    @Override
    public String getName() {
        return this.fieldName;
    }
}
