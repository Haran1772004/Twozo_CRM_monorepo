package com.tlc.crm.common.iam.field;

/**
 * <p>
 * Represents the fields related to {@link com.tlc.crm.common.iam.api.model.CrmUser}
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public enum CrmUserFields {

    ID("id"),
    ORG_ID("orgId"),
    IAM_ID("iamId"),
    USERS("users"),
    FIRST_NAME("firstName"),
    LAST_NAME("lastName"),
    EMAIL("email"),
    PHONE("phone"),
    ROLE("role"),
    TOTAL_RECORDS("totalRecords"),
    HAS_MORE("hasMore"),
    START("start"),
    LIMIT("limit"),
    DATA("data"),
    TYPE("type"),
    MESSAGE("message"),
    ROLE_ID("roleId"),
    TEAM_ID("teamId"),
    TERRITORY_ID("territoryId"),
    USER("user"),
    CRITERIA("criteria"),
    VALUE("value"),
    NAME("name"),
    SOURCE_USER_IDS("sourceUserIds"),
    TARGET_USER_ID("targetUserId"),
    REQUEST_ID("requestId"),
    JOB_TITLE("jobTitle"),
    PIPELINE_ID("pipelineId"),
    TEAM_IDS("teamIds"),
    REPORTING_MANAGER_ID("reportingManagerId"),
    VERIFICATION_CODE("verificationCode"),
    ERRORS("errors"),
    CREATED_RECORDS("createdRecords"),
    UPDATED_RECORDS("updatedRecords"),
    ORGANIZATION("organization"),
    REPORTING_MANAGER("reportingManager"),
    STATE("state"),
    LAST_LOGIN_TIME("lastLoginTime"),
    PROFILE("profile"),
    SUMMARY("summary"),
    TEAMS("teams"),
    TERRITORIES("territories"),
    PIPELINE("pipeline"),
    MOBILE("mobile"),
    FAILED_RECORDS("failedRecords"),
    THUMBNAIL("thumbnail"),
    SORT("sort"),
    FIELD("field"),
    USER_IDS("userIds"),
    TERRITORY_IDS("territoryIds"),
    STATE_ID("stateId"),
    ERROR("error"),
    IS_ACTIVATABLE("isActivatable"),
    IS_DEACTIVATABLE("isDeactivatable"),
    IS_TRANSFERABLE("isTransferable"),
    IS_EDITABLE("isEditable"),
    CONFIG("config"),
    TOTAL_ACCOUNT_ADMIN("totalAccountAdmin"),
    ROLE_TYPE("roleType"),
    PASSWORD("password"),
    USER_ID("userId"),
    TOKEN("token"),
    INVITATION_ID("invitationId"),
    CRM_USER("crm-user"),
    INVITATION_SERVER_SUCCESS_URL("server.redirectUrl.invitation.success"),
    INVITATION_LOCAL_SUCCESS_URL("local.redirectUrl.invitation.success"),
    INVITATION_SERVER_FAILURE_URL("server.redirectUrl.invitation.failure"),
    INVITATION_LOCAL_FAILURE_URL("local.redirectUrl.invitation.failure"),
    RESET_PASSWORD_SERVER_SUCCESS_URL("server.redirectUrl.resetPassword.success"),
    RESET_PASSWORD_LOCAL_SUCCESS_URL("local.redirectUrl.resetPassword.success"),
    RESET_PASSWORD_SERVER_FAILURE_URL("server.redirectUrl.resetPassword.failure"),
    RESET_PASSWORD_LOCAL_FAILURE_URL("local.redirectUrl.resetPassword.failure"),
    LOCAL_VALIDATE_LINK_URL("local.redirectUrl.validateLink"),
    SERVER_VALIDATE_LINK_URL("server.redirectUrl.validateLink"),
    PREFERENCE("preference"),
    PREFERENCES("preferences"),
    CAN_RE_INVITE("canReInvite"),
    SOURCE_USER_ID("sourceUserId"),
    CONFIRM_PASSWORD("confirmPassWord"),
    EXPIRY_LINK_ID("expiryLinkId"),
    IS_ADMIN("isAdmin"),
    EXCLUDE_USERS_WITHOUT_PHONE("excludeUsersWithoutPhone"),
    ;

    private final String name;

    CrmUserFields(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
