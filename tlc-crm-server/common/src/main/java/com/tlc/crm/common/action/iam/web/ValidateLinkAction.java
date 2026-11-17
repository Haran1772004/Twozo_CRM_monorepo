package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.PublicEventAction;
import com.tlc.crm.common.fields.QueryParamAttributes;
import com.tlc.crm.common.iam.api.manager.TemporaryUserLinkManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.models.link.user.ForgotPasswordLink;
import com.tlc.crm.common.models.link.user.TemporaryUserLink;
import com.tlc.crm.common.models.link.user.UserInvitationLink;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * <p>
 * Handles verification of user links, such as invitations or forgot password links.
 * </p>
 *
 * @author Mohamed Yasar k
 * @version 1.0
 */
@WebAction(path = "/validateLink", method = HttpMethod.GET)
public class ValidateLinkAction extends PublicEventAction {

    private final TemporaryUserLinkManager temporaryUserLinkManager;
    private final Configurations configurations;

    public ValidateLinkAction() {
        this.temporaryUserLinkManager = TemporaryUserLinkManager.getInstance();
        this.configurations = ConfHolder.getInstance().getProperties("crm-user");
    }

    @Override
    public void publicProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject tokenJson = Json.object(Base64.getDecoder().decode(request.getParameter(QueryParamAttributes.TOKEN)));
        final long orgId = tokenJson.getLong(UserFields.ORG_ID);
        final long expiryLinkId = tokenJson.getLong(CrmUserFields.EXPIRY_LINK_ID.getName());
        final Optional<TemporaryUserLink> temporaryUserLink = temporaryUserLinkManager.get(orgId, expiryLinkId);

        temporaryUserLink.ifPresentOrElse(userLink -> {
            final TemporaryUserLink.Category category = userLink.getCategory();

            switch (category) {
                case INVITE -> {
                    final String email = request.getParameter(QueryParamAttributes.EMAIL);
                    final UserInvitationLink userInvitationLink = userLink.get();

                    if (Instant.now().toEpochMilli() > userInvitationLink.getExpireTime()) {
                        response.setTemporaryRedirectUrl(getUserInviteExpiryUrl(email, request.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED)));
                    } else {
                        setUserInviteLink(userInvitationLink.getUser(), email, request, userInvitationLink);
                        response.setTemporaryRedirectUrl(userInvitationLink.getUrl());
                    }
                }
                case FORGOT_PASSWORD -> {
                    final String email = tokenJson.getString(UserFields.EMAIL);
                    final ForgotPasswordLink forgotPasswordLink = userLink.get();

                    if (Instant.now().toEpochMilli() > forgotPasswordLink.getExpireTime()) {
                        response.setTemporaryRedirectUrl(getForgotPasswordExpiryUrl(email, request.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED)));
                    } else {
                        setForgotPasswordLink(forgotPasswordLink.getUser(), email, request, forgotPasswordLink);
                        response.setTemporaryRedirectUrl(forgotPasswordLink.getUrl());
                    }
                }
            }
        }, () -> response.setTemporaryRedirectUrl(getForgotPasswordExpiryUrl(tokenJson.getString(UserFields.EMAIL), request.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED))));
    }

    /**
     * <p>
     * Gets the re-direction url.
     * </p>
     *
     * @param email                 Represents the email
     * @param localRedirectionParam Represents the local re-directive param
     * @return re-direction url
     */
    private String getUserInviteExpiryUrl(final String email, final String localRedirectionParam) {
        final String queryParam = String.format("?email=%s", email);
        final boolean paramExists = Boolean.parseBoolean(localRedirectionParam);
        final String redirectUrl = HttpUtil.buildAbsoluteUrl(configurations.getConfig(paramExists ? CrmUserFields.INVITATION_LOCAL_FAILURE_URL.getName() :
                CrmUserFields.INVITATION_SERVER_FAILURE_URL.getName()));

        return String.format("%s%s", redirectUrl, queryParam);
    }

    /**
     * <p>
     * Sets the user verification link.
     * </p>
     *
     * @param crmUser            Represents the {@link CrmUser}
     * @param email              Represent the email
     * @param crmRequest         Represent the crmRequest
     * @param userInvitationLink Represents the {@link UserInvitationLink} details
     */
    private void setUserInviteLink(final CrmUser crmUser, final String email, final CrmRequest crmRequest, final UserInvitationLink userInvitationLink) {
        final String queryParam = String.format("?token=%s&email=%s", Base64.getEncoder().encodeToString(String.format("%s",
                String.format("{\"orgId\":%d,\"expiryLinkId\":\"%d\"}", crmUser.orgId(), userInvitationLink.id())).getBytes()), email);
        final boolean paramExists = Boolean.parseBoolean(crmRequest.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED));
        final String redirectUrl = HttpUtil.buildAbsoluteUrl(configurations.getConfig(paramExists ? CrmUserFields.INVITATION_LOCAL_SUCCESS_URL.getName()
                : CrmUserFields.INVITATION_SERVER_SUCCESS_URL.getName()));

        userInvitationLink.setUrl(String.format("%s%s", redirectUrl, queryParam));
    }

    /**
     * <p>
     * Gets the forget password expiry link
     * </p>
     *
     * @param email                 Represents the email
     * @param localRedirectionParam Represents the local re-directive param
     * @return re-direction url
     */
    private String getForgotPasswordExpiryUrl(final String email, final String localRedirectionParam) {
        final String queryParam = String.format("?email=%s", email);
        final boolean paramExists = Boolean.parseBoolean(localRedirectionParam);
        final String redirectUrl = HttpUtil.buildAbsoluteUrl(configurations.getConfig(paramExists ? CrmUserFields.RESET_PASSWORD_LOCAL_FAILURE_URL.getName() :
                CrmUserFields.RESET_PASSWORD_SERVER_FAILURE_URL.getName()));

        return String.format("%s%s", redirectUrl, queryParam);
    }

    /**
     * <p>
     * Sets the forget password link
     * </p>
     *
     * @param crmUser            Represents the {@link CrmUser}
     * @param email              Represent the email
     * @param crmRequest         Represent the crmRequest
     * @param forgotPasswordLink Represents the {@link ForgotPasswordLink}
     */
    private void setForgotPasswordLink(final CrmUser crmUser, final String email, final CrmRequest crmRequest, final ForgotPasswordLink forgotPasswordLink) {
        final String queryParam = String.format("?token=%s&email=%s", Base64.getEncoder().encodeToString(String.format("%s",
                String.format("{\"orgId\":%d, \"userId\":%d, \"expiryLinkId\":\"%d\", \"email\":\"%s\"}", crmUser.orgId(),
                        crmUser.id(), forgotPasswordLink.id(), email)).getBytes()), email);
        final boolean paramExists = Boolean.parseBoolean(crmRequest.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED));
        final String redirectUrl = HttpUtil.buildAbsoluteUrl(configurations.getConfig(paramExists ? CrmUserFields.RESET_PASSWORD_LOCAL_SUCCESS_URL.getName()
                : CrmUserFields.RESET_PASSWORD_SERVER_SUCCESS_URL.getName()));

        forgotPasswordLink.setUrl(String.format("%s%s", redirectUrl, queryParam));
    }
}