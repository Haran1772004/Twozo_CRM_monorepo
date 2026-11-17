package com.tlc.iam.common.events;

public final class IamEvents {

    public static final String AUTHENTICATE_USER = "iam.user.authentication";
    public static final String AUTHORIZE_USER = "iam.user.authorization";
    public static final String SIGN_OUT = "iam.user.logoff";
    public static final String CHECK_USER_EXISTENCE = "iam.user.existence.check";
    public static final String FETCH_USER = "iam.user.fetch";
    public static final String USER_UPDATE = "iam.user.update";
    public static final String FETCH_USER_SESSIONS = "i18n.iam.user.sessions.fetch";
    public static final String CREATE_ORGANIZATION = "iam.organization.create";
    public static final String FETCH_ORGANIZATION = "iam.organization.fetch";
    public static final String ORGANIZATION_UPDATE = "iam.organization.update";
    public static final String CHECK_ORG_EXISTENCE = "iam.organization.existence.check";
    public static final String RESET_PASSWORD = "iam.user.password.reset";
    public static final String CHANGE_PASSWORD = "iam.user.password.change";
    public static final String FETCH_USERS = "iam.users.fetch";
    public static final String ORG_STATE_MODIFIER = "iam.organization.state.modifier";
    public static final String USER_DEACTIVATE = "iam.user.delete";
    public static final String USER_UPSERT = "iam.user.upsert";
    public static final String USER_PARTIAL_BULK_UPDATE = "iam.user.partial.bulk.update";
    public static final String DP_INFO_FETCH = "iam.user.display.picture.fetch";
    public static final String DP_VIEW = "iam.user.display.picture.view";
    public static final String DP_UPLOAD = "iam.user.display.picture.upload";
    public static final String DP_DELETE = "iam.user.display.picture.delete";
    public static final String USER_STATE_UPDATE = "iam.user.state.update";
    public static final String USER_PARTIAL_UPDATE = "iam.user.partial.update";
    public static final String ACCOUNT_DETAILS_UPDATE = "iam.account.details.update";
    public static final String APPLICATION_FETCH = "iam.application.fetch";
    public static final String FETCH_USER_PAGE = "iam.fetch.user.page";
    public static final String SET_PASSWORD = "iam.user.password.set";
    public static final String CHECK_EMAIL_EXISTENCE = "iam.user.email.check.existence";
    public static final String SEARCH_USER_PAGE = "iam.search.user.page";
    public static final String ORG_SIGNUP_UPDATE = "iam.organization.signup.update";
    public static final String LINK_ID_PROVIDER_USER = "iam.user.link.id.provider.user";
    public static final String GENERATE_API_KEY = "iam.api.key.generate";
    public static final String FETCH_API_KEY = "iam.api.key.fetch";
    public static final String DELETE_API_KEY = "iam.api.key.delete";
    public static final String API_KEY_AUTHENTICATION = "iam.api.key.authenticate";
    public static final String ACTIVE_USERS_CHECK = "iam.user.active.users.check";
    public static final String DEACTIVATED_USERS_CHECK = "iam.user.deactivated.users.check";
    public static final String FETCH_USER_PAGE_BY_STATE = "iam.fetch.user.page.by.state";
    public static final String FETCH_ACTIVE_USERS_THUMBNAIL = "iam.fetch.active.user.thumbnail";
}
