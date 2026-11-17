package com.tlc.crm.core.notification.email.inbox.fields;

import com.tlc.crm.entity.models.FieldNameHolder;

/**
 * <p>
 * Represents fields related to team inbox
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public enum TeamInboxFields implements FieldNameHolder {

    ID("id"),
    ORG_ID("orgId"),
    EMAIL_ADDRESS("emailAddress"),
    ACCESS_TO("accessTo"),
    USERS("users"),
    TERRITORIES("territories"),
    TEAMS("teams"),
    EMAIL_VERIFICATION_STATUS("emailVerificationStatus"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    TEAM_INBOXES("teamInboxes"),
    AUTO_FORWARDING_SYNC_TIME("autoForwardingSyncTime"),
    CREATED_RECORDS("createdRecords"),
    FAILED_RECORDS("failedRecords"),
    VALIDATION_ERRORS("validationErrors"),
    RESPONSE("response"),
    VISIBLE_TO("visibleTo"),
    VISIBILITY_STATUS("visibilityStatus"),
    EVERYONE("everyOne"),
    SELECTED("selected"),
    TERRITORY_ID("territoryId"),
    TERRITORY_NAME("territoryName"),
    TEAM_NAME("teamName"),
    TEAM_ID("teamId"),
    TEAM_INBOX("teamInbox"),
    NAME("name"),
    REDIRECT_URL("redirectUrl"),
    IS_VERIFICATION_MAIL_SENT("isVerificationEmailSent"),
    EMAIL_ID("emailId"),
    TOKEN("token"),
    EMAILS("emails"),
    AUTO_FORWARDING_ADDRESS("autoForwardingAddress"),
    LOCAL_VALIDATE_LINK_URL("local.redirectUrl.validateLink"),
    SERVER_VALIDATE_LINK_URL("server.redirectUrl.validateLink"),
    VERIFICATION_LOCAL_SUCCESS_URL("local.redirectUrl.success"),
    VERIFICATION_SERVER_SUCCESS_URL("server.redirectUrl.success"),
    VERIFICATION_LOCAL_FAILURE_URL("local.redirectUrl.failure"),
    VERIFICATION_SERVER_FAILURE_URL("server.redirectUrl.failure"),
    EMAIL("email"),
    ADD_ON_SERVICES("addOnServices"),
    DOMAIN_VERIFICATION_STATUS("domainVerificationStatus"),
    ;

    private final String fieldName;

    TeamInboxFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getName() {
        return this.fieldName;
    }
}