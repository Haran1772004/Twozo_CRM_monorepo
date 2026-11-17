package com.tlc.crm.core.action.iam;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.Env;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.CrmPublicAction;
import com.tlc.crm.common.fields.QueryParamAttributes;
import com.tlc.crm.common.iam.api.manager.TemporaryUserLinkManager;
import com.tlc.crm.common.models.link.user.TemporaryUserLink;
import com.tlc.crm.common.iam.field.CrmUserFields;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.core.internal.status.CoreErrorCodes;
import com.tlc.crm.entity.fields.FieldKeys;
import com.tlc.iam.client.UserService;
import com.tlc.iam.client.internal.action.web.CookieBuilder;
import com.tlc.iam.client.models.PasswordSetRequest;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.ClientApplication;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.ApplicationConverter;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Manages user reset password related web actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
@WebAction(path = "/user/password/reset/mgmt")
public class PasswordResetAction extends CrmPublicAction {

    private static final String APPLICATION_NAME = "crm";
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetAction.class);

    private final UserService iamUserService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;
    private final TemporaryUserLinkManager temporaryUserLinkManager;
    private final Configurations configurations;
    private final ApplicationConverter applicationConverter;

    public PasswordResetAction() {
        iamUserService = UserService.getInstance();
        organizationService = OrganizationService.getInstance();
        crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
        temporaryUserLinkManager = TemporaryUserLinkManager.getInstance();
        this.configurations = ConfHolder.getInstance().getProperties("crm-user");
        applicationConverter = ApplicationConverter.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param crmRequest  Holds the {@link CrmRequest} details
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    @Override
    public void pProcess(final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final JsonObject requestJson = crmRequest.getRequestJson();
        final String type = requestJson.getString(FieldKeys.TYPE.getName());
        final JsonObject data = requestJson.getJsonObject(FieldKeys.DATA.getName());
        final JsonObject jsonObject = Json.object(Base64.getDecoder().decode(data.getString(CrmUserFields.TOKEN.getName())));
        final Long orgId = jsonObject.getLong(CrmUserFields.ORG_ID.getName());
        final Long id = jsonObject.getLong(CrmUserFields.EXPIRY_LINK_ID.getName());

        if ("setPassword".equals(type)) {
            setPassword(orgId, id, data, crmRequest, crmResponse);
        }
    }

    /**
     * <p>
     * Sets the password for the user based on the org id, user id and email
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param id          Represents the invitation id
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse} details
     */
    private void setPassword(final Long orgId, final Long id, final JsonObject data, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        final Optional<TemporaryUserLink> temporaryUserLink = temporaryUserLinkManager.get(orgId, id);

        temporaryUserLink.ifPresentOrElse(userLink -> {
            final long iamOrgId = organizationService.getIamOrgId(orgId);
            final Long iamUserId = crmUserService.getUser(orgId, userLink.getUser().id()).getIamId();

            crmResponse.convertIntoAsync();

            final EventCallback<User> userEventCallback = new EventCallback<>() {
                @Override
                public void onSuccess(final User user) {
                    final String email = data.getString(CrmUserFields.EMAIL.getName());

                    if (Objects.equals(email, user.getEmail())) {

                        if (Objects.equals(User.State.INVITED, user.getUserState())) {

                            if (Instant.now().toEpochMilli() > userLink.getExpireTime()) {
                                crmResponse.setTemporaryRedirectUrl(getUserInviteExpiryUrl(email,
                                        crmRequest.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED)));
                                crmResponse.send();
                            } else {
                                user.setUserState(User.State.ACTIVE);
                                user.setPassword(data.getString(CrmUserFields.PASSWORD.getName()));
                                final PasswordSetRequest passwordSetRequest = new PasswordSetRequest(user, APPLICATION_NAME.toUpperCase(), crmRequest.getUserAgent());

                                final EventCallback<JsonObject> eventCallback = new EventCallback<>() {

                                    @Override
                                    public void onSuccess(final JsonObject jsonObject) {
                                        setCookiesAndDispatch(orgId, user.id(), jsonObject, crmResponse);
                                    }

                                    @Override
                                    public void onFailure(final Throwable throwable) {
                                        sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.PASSWORD_RESET_ACTION_FAILED, "i18n.iam.user.password.reset.failed", throwable));
                                    }
                                };
                                iamUserService.setPassword(passwordSetRequest, eventCallback);
                            }
                        } else {
                            sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.USER_ALREADY_ACTIVATED, "i18n.iam.user.already.activated"));
                        }
                    } else {
                        sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.INVALID_EMAIL, "i18n.iam.user.invalid.email"));
                    }
                }

                @Override
                public void onFailure(Throwable cause) {
                    sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed"));
                }
            };
            iamUserService.fetchUser(iamOrgId, iamUserId, userEventCallback);
        }, () -> sendError(crmResponse, HttpStatus.OK, ErrorCode.get(CoreErrorCodes.INVALID_USER_INVITATION, "i18n.iam.user.invitation.link.invalid")));
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
        final String redirectUrl = configurations.getConfig(paramExists ? CrmUserFields.INVITATION_LOCAL_FAILURE_URL.getName() :
                CrmUserFields.INVITATION_SERVER_FAILURE_URL.getName());

        return String.format("%s%s", redirectUrl, queryParam);
    }

    /**
     * <p>
     * Sets the cookies to the response after fetching {@link ClientApplication} and dispatches it.
     * </p>
     *
     * @param orgId       Represents the organization id
     * @param userId      Represents the user id
     * @param jsonObject  Represents the json data
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void setCookiesAndDispatch(final Long orgId, final Long userId, final JsonObject jsonObject, final CrmResponse crmResponse) {
        if (jsonObject.containsKey(IamFields.ERROR)) {
            final Collection<String> errors = new ArrayList<>();

            jsonObject.getJsonArray(IamFields.ERROR).forEach(error -> errors.add(crmResponse.resolveI18nString((String) error)));
            crmResponse.put(CrmUserFields.ERRORS.getName(), errors);
            crmResponse.send();
        } else {
            final Event<String> applicationFetchEvent = new Event<>(IamEvents.APPLICATION_FETCH, APPLICATION_NAME);
            final EventCallback<byte[]> applicationFetchEventCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final byte[] data) {
                    try {
                        final String accessToken = jsonObject.getString(IamFields.ACCESS_TOKEN);
                        final ClientApplication clientApplication = applicationConverter.convertToApplication(Json.object(data));

                        crmResponse.addCookies(CookieBuilder.buildAuthCookie(clientApplication, accessToken));

                        if (Env.isDevMode()) {
                            crmResponse.addCookies(CookieBuilder.buildDevAuthCookie(clientApplication, accessToken));
                        }
                        crmResponse.put(CrmUserFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.iam.user.password.reset.success"));
                    } catch (Throwable throwable) {
                        LOGGER.error("Password reset: Password reset failed. Org Id:{}, IAM User Id:{}, Reason:{}", orgId, userId, throwable.getMessage());
                        sendError(crmResponse, null, ErrorCode.get(CoreErrorCodes.PASSWORD_RESET_ACTION_FAILED, "i18n.iam.user.password.reset.failed", throwable));
                    } finally {
                        crmResponse.send();
                    }
                }

                @Override
                public void onFailure(final Throwable cause) {
                    LOGGER.error("Password reset: Invalid application name, Reason : {}", cause.getMessage());
                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.INVALID_APP_NAME, "i18n.invalid.app.name", cause));
                }
            };

            EventNotifier.get().send(applicationFetchEvent, applicationFetchEventCallback);
        }
    }
}