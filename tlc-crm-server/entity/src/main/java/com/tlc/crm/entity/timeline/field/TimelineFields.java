package com.tlc.crm.entity.timeline.field;

import com.tlc.crm.entity.models.FieldNameHolder;

import java.util.Objects;

/**
 * Represents the fields that are related to timeline
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum TimelineFields implements FieldNameHolder {

    ID("id"),
    OWNER_ID("ownerId"),
    CATEGORY("category"),
    AUDITED_TIME("auditedTime"),
    ACTION("action"),
    NEW_VALUE("newValue"),
    OLD_VALUE("oldValue"),
    FIELD_NAME("fieldName"),
    DEAL_TITLE("dealTitle"),
    ACTIVITY_TYPE("activityType"),
    IS_ACTIVITY_DONE("isActivityDone"),
    ACTIVITY_NOTE("activityNote"),
    ACTIVITY_NAME("activityName"),
    FILE("file"),
    TIMELINE("timeline"),
    NOTE("note"),
    CONTACT_ID("contactId"),
    DEAL_ID("dealId"),
    COMPANY_ID("companyId"),
    FILTER("filter"),
    START("start"),
    LIMIT("limit"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    UPDATED_TIMELINE("updatedTimeline"),
    ATTACHMENT_ID("attachmentId"),
    CONTACT_NAME("contactName"),
    TYPE("type"),
    DATE("date"),
    COMPANY_NAME("companyName"),
    DATA("data"),
    ICON("icon"),
    CONTACT("contact"),
    DEAL("deal"),
    COMPANY("company"),
    ACTIVITY("activity"),
    CALENDAR("calendar"),
    NAME("name"),
    SALES_OWNER("salesOwner"),
    COPY("copy"),
    OLD_VALUES("oldValues"),
    TIMELINE_ID("timelineId"),
    WEB_FORM("webForm"),
    NOTE_ID("noteId"),
    ACTIVITY_ID("activityId"),
    WEB_FORM_ID("webFormId"),
    LOST("lost"),
    MAIL("mail"),
    FROM("from"),
    TO("to"),
    SUBJECT("subject"),
    CONTENT("content"),
    CONVERSATION_ID("conversationId"),
    OUTBOUND("outbound"),
    INBOUND("inbound"),
    CONTACTS("contacts"),
    EMAIL_ADDRESS("emailAddress"),
    SYNCED_ADDRESS("syncedAddress"),
    FILE_NAME("fileName"),
    USER("user"),
    FACEBOOK("facebook"),
    WHATSAPP("whatsapp"),
    INSTAGRAM("instagram"),
    ARCHIVE("archive"),
    MISSED_INCOMING_CALL("missedIncomingCall"),
    UNANSWERED_OUTGOING_CALL("unansweredOutgoingCall"),
    INCOMING_CALL("incomingCall"),
    OUTGOING_CALL("outgoingCall"),
    CALL("call"),
    AGENDA("agenda"),
    CALL_DURATION("callDuration"),
    PRODUCT("product"),
    INVOICE("invoice"),
    INVOICE_ID("invoiceId"),
    INVOICE_NUMBER("invoiceNumber"),
    ;

    private final String name;

    TimelineFields(final String name) {
        this.name = Objects.requireNonNull(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return this.name;
    }
}