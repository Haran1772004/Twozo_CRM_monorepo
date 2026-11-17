package com.tlc.crm.email.notification.email.fields;

import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Represents fields related to mail
 * </p>
 *
 * @author NandiniRakAs
 * @version 1.0
 */
public enum EmailFields implements FieldNameHolder {

    ID("id"),
    START("start"),
    LIMIT("limit"),
    SYNCED_ADDRESS("syncedAddress"),
    CODE("code"),
    INBOX("inbox"),
    SENT("sent"),
    TYPE("type"),
    DATA("data"),
    IS_EMAIL_TRACKER_ENABLED("isEmailTrackerEnabled"),
    IS_LINK_TRACKER_ENABLED("isLinkTrackerEnabled"),
    IS_NOTIFICATION_ENABLED("isNotificationEnabled"),
    PROVIDER("provider"),
    ORG_ID("orgId"),
    USER_ID("userId"),
    STATE("state"),
    RELATED_CONTACTS("relatedContacts"),
    REASON("reason"),
    UPDATED_RECORDS("updatedRecords"),
    FAILED_RECORDS("failedRecords"),
    CREATED_RECORDS("createdRecords"),
    ERRORS("errors"),
    IS_CONTACT_CREATION_ENABLED("isContactCreationEnabled"),
    LIFE_CYCLE_STAGE_ID("lifecycleStageId"),
    IS_DEAL_LINKING_ENABLED("isDealLinkingEnabled"),
    IS_CONTACT_LINKING_ENABLED("isContactLinkingEnabled"),
    SETTINGS("settings"),
    SYNC_CONFIG("syncConfig"),
    PROVIDER_ACTION("providerAction"),
    SYNC_TYPE("syncType"),
    STATUS("status"),
    DATE_CONFIG("dateConfig"),
    LABELS("labels"),
    ALL_LABELS("allLabels"),
    SELECTED_LABELS("selectedLabels"),
    SYNC_START_FROM("syncStartFrom"),
    START_DATE("startDate"),
    END_DATE("endDate"),
    DRAFT("draft"),
    ARCHIVE("archive"),
    DELETE("delete"),
    EMAIL_ADDRESS("emailAddress"),
    DELETED_RECORDS("deletedRecord"),
    EMAIL_ID("emailId"),
    LAST_SYNC_TIME("lastSyncTime"),
    IS_IN_PROGRESS("isInProgress"),
    IS_DONE("isDone"),
    EMAIL_SYNC_INFO("emailSyncInfo"),
    EMAIL_SYNC_SETTINGS("emailSyncSettings"),
    FROM_ADDRESSES("fromAddresses"),
    ERROR("error"),
    DATE_INTERVAL("dateInterval"),
    IS_SYNC_ENABLED("isSyncEnabled"),
    CONNECTION_STATUS("connectionStatus"),
    EMAIL_SYNC_CONFIG("emailSyncConfig"),
    SYNC_INTERVAL("syncInterval"),
    THREAD_ID("threadId"),
    SYNC_STATUS("syncStatus"),
    CONVERSATION_ID("conversationId"),
    OUTBOUND_TYPE("outboundType"),
    ENTITY_CONVERSATION_ID("entityConversationId"),
    IS_SCHEDULED("isScheduled"),
    SYNC_SERVICE("sync-service"),
    MESSAGE("message"),
    USER_NAME("userName"),
    PASSWORD("password"),
    HOST("host"),
    PORT("port"),
    IS_VERIFIED("isVerified"),
    IDS("ids"),
    VARIANT("variant"),
    IS_DEFAULT("isDefault"),
    SYNC_EMAILS("syncEmails"),
    DISCONNECT_MODE("mode"),
    DEFAULT_SYNC_ID("defaultSyncId"),
    IS_ACTIVE_ONLY("isActiveOnly"),
    ;

    private final String fieldName;

    EmailFields(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getName() {
        return this.fieldName;
    }
}
