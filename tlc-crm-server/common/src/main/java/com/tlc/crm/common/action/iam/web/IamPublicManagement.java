package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.executor.task.RunnableTask;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.pub.PublicEventAction;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.fields.QueryParamAttributes;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.iam.api.manager.TemporaryUserLinkManager;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.models.link.user.ForgotPasswordLink;
import com.tlc.crm.common.models.link.user.TemporaryUserLink;
import com.tlc.crm.common.iam.field.CrmUserFields;

import com.tlc.crm.common.iam.field.IamFields;
import com.tlc.crm.common.iam.internal.status.TemporaryUserLinkErrorCode;
import com.tlc.crm.common.iam.service.OrganizationService;
import com.tlc.crm.common.models.Action;

import com.tlc.iam.client.UserService;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.crm.notification.app.Event.Type.POST;
import static com.tlc.crm.notification.app.event.NotificationEvent.EMAIL_ALERT_EVENT;

/**
 * Public action handler
 *
 * @author Selvakumar
 * @version 1.0
 */
@WebAction(path = "/accounts/{application}/mgmt")
public class IamPublicManagement extends PublicEventAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(IamPublicManagement.class);

    private static final String FORGOT_PASSWORD = "forgotPassword";
    private static final String RESET_PASSWORD = "resetPassword";

    private final Configurations configurations;
    private final TemporaryUserLinkManager temporaryUserLinkManager;
    private final UserService iamUserService;
    private final OrganizationService organizationService;
    private final com.tlc.crm.common.iam.service.UserService crmUserService;

    public IamPublicManagement() {
        this.configurations = ConfHolder.getInstance().getProperties("crm-user");
        this.temporaryUserLinkManager = TemporaryUserLinkManager.getInstance();
        this.iamUserService = UserService.getInstance();
        this.organizationService = OrganizationService.getInstance();
        this.crmUserService = com.tlc.crm.common.iam.service.UserService.getInstance();
    }

    @Override
    public void publicProcess(final CrmRequest request, final CrmResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(IamFields.DATA);
        final String type = requestJson.getString(IamFields.TYPE);

        switch (type) {
            case FORGOT_PASSWORD -> forgotPassword(data, request, response);
            case RESET_PASSWORD -> resetPassword(data, request, response);
        }
    }

    /**
     * <p>
     * Sends the forgot password re-set lint to the respective email from the given request payload.
     * </p>>
     *
     * @param data        Represents the request {@link JsonObject} data
     * @param crmRequest  Represents the {@link CrmRequest}
     * @param crmResponse Represents the {@link CrmResponse}
     */
    private void forgotPassword(final JsonObject data, final CrmRequest crmRequest, final CrmResponse crmResponse) {
        crmResponse.convertIntoAsync();

        final Event<byte[]> event = new Event<>(IamEvents.CHECK_USER_EXISTENCE, data.getBytes());

        /*
         * Checks whether the user exists or not.
         */
        final EventCallback<Boolean> userExistenceCallback = new EventCallback<>() {
            @Override
            public void onSuccess(final Boolean isUserExists) {

                if (Objects.nonNull(isUserExists) && Boolean.TRUE.equals(isUserExists)) {

                    /*
                     * Fetches the user related information.
                     */
                    final EventCallback<User> userExistenceCallback = new EventCallback<>() {

                        /**
                         * <p>
                         *     {@inheritDoc}
                         * </p>
                         *
                         * @param user Response from the consumer
                         */
                        @Override
                        public void onSuccess(final User user) {
                            final Long orgId = organizationService.getOrgId(user.orgId());
                            final CrmUser crmUser = crmUserService.getUser(orgId, crmUserService.getUserId(orgId, user.id()));
                            final Long crmOrgId = crmUser.orgId();
                            final Long userId = crmUser.id();

                            try {
                                final ForgotPasswordLink forgotPasswordLink = new ForgotPasswordLink();

                                forgotPasswordLink.setOrgId(crmOrgId);
                                crmUser.setUser(user);
                                forgotPasswordLink.setUser(crmUser);
                                temporaryUserLinkManager.save(forgotPasswordLink);
                                setVerificationLink(crmUser, crmRequest, forgotPasswordLink);

                                final EventData eventData = new EventData();

                                eventData.setOrgId(crmOrgId);
                                eventData.setUserId(userId);
                                eventData.setValues(List.of(forgotPasswordLink));
                                eventData.setApiType(FORGOT_PASSWORD);
                                eventData.setAction(Action.CREATE);

                                crmResponse.put(CrmUserFields.MESSAGE.getName(), crmResponse.resolveI18nString("i18n.user.reset.password.link.sent"));

                                SecureTaskHandler.getInstance().secureProcess(orgId, userId, () ->
                                        triggerEvent(EMAIL_ALERT_EVENT, eventData, POST));
                            } catch (Throwable throwable) {
                                LOGGER.error("Forgot Password: The forgot password action has failed, Reason: {}", throwable.getMessage());
                                sendError(crmResponse, null, ErrorCode.get(TemporaryUserLinkErrorCode.FORGOT_PASSWORD_ACTION_FAILED, "i18n.user.forgot.password.process.failed", throwable));
                            } finally {
                                crmResponse.send();
                            }
                        }

                        /**
                         * <p>
                         *     {@inheritDoc}
                         * </p>
                         *
                         * @param throwable Cause of the failure
                         */
                        @Override
                        public void onFailure(final Throwable throwable) {
                            LOGGER.error("Forgot Password: The forgot password action has failed, {}", throwable.getMessage());
                            sendError(crmResponse, null, ErrorCode.get(TemporaryUserLinkErrorCode.USER_DETAIL_RETRIEVE_FAILED, "i18n.user.detail.retrieve.failed", throwable));
                        }
                    };

                    iamUserService.fetchUserByEmail(data.getString(IamFields.EMAIL), userExistenceCallback);
                } else {
                    sendError(crmResponse, null, ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, "i18n.user.email.account.not.found"));
                }
            }

            /**
             * <p>
             *     {@inheritDoc}
             * </p>
             *
             * @param throwable Cause of the failure
             */
            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("Forgot Password: The forgot password action has failed, {}", throwable.getMessage());
                sendError(crmResponse, null, ErrorCode.get(TemporaryUserLinkErrorCode.USER_EXISTS_CHECK_FAILED, "i18n.user.not.found", throwable));
            }
        };

        EventNotifier.get().send(event, userExistenceCallback);
    }

    /**
     * <p>
     * Sets the verification link for user link expiry log.
     * </p>
     *
     * @param crmUser            Represents the {@link CrmUser}
     * @param forgotPasswordLink Represents the {@link ForgotPasswordLink}
     */
    private void setVerificationLink(final CrmUser crmUser, final CrmRequest crmRequest, final ForgotPasswordLink forgotPasswordLink) {
        final String email = crmUser.getEmail();
        final boolean paramExists = Boolean.parseBoolean(crmRequest.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED));
        final String queryParam = String.format("?token=%s&email=%s&localRedirectionRequired=%s", Base64.getEncoder().encodeToString(String
                .format("%s", String.format("{\"orgId\":%d, \"userId\":%d, \"expiryLinkId\":\"%d\", \"email\":\"%s\"}", crmUser.orgId(),
                        crmUser.id(), forgotPasswordLink.id(), email)).getBytes()), email, paramExists);
        final String redirectUrl = HttpUtil.buildAbsoluteUrl(configurations.getConfig(paramExists ? CrmUserFields.LOCAL_VALIDATE_LINK_URL.getName()
                : CrmUserFields.SERVER_VALIDATE_LINK_URL.getName()));

        forgotPasswordLink.setUrl(String.format("%s%s", redirectUrl, queryParam));
    }

    /**
     * Resets the password for the given user
     *
     * @param data     Request data
     * @param response CrmResponse response
     */
    private void resetPassword(final JsonObject data, final CrmRequest crmRequest, final CrmResponse response) {
        final JsonObject tokenJson = Json.object(Base64.getDecoder().decode(data.getString(CrmUserFields.TOKEN.getName())));
        final long orgId = tokenJson.getLong(UserFields.ORG_ID);
        final long expiryLinkId = tokenJson.getLong(CrmUserFields.EXPIRY_LINK_ID.getName());
        final Optional<TemporaryUserLink> temporaryUserLink = temporaryUserLinkManager.get(orgId, expiryLinkId);

        temporaryUserLink.ifPresent(userLink -> {
            final ForgotPasswordLink forgotPasswordLink = userLink.get();
            final String email = tokenJson.getString(UserFields.EMAIL);

            if (Objects.isNull(forgotPasswordLink)) {
                response.setTemporaryRedirectUrl(getForgotPasswordExpiryUrl(email, crmRequest.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED)));
            } else {
                if (Instant.now().toEpochMilli() > forgotPasswordLink.getExpireTime()) {
                    response.setTemporaryRedirectUrl(getForgotPasswordExpiryUrl(email, crmRequest.getParameter(QueryParamAttributes.LOCAL_REDIRECTION_REQUIRED)));
                } else {
                    response.convertIntoAsync();

                    fetchUserAndResetPassword(orgId, expiryLinkId, forgotPasswordLink, data, response);
                }
            }
        });
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
    private String getForgotPasswordExpiryUrl(final String email, final String localRedirectionParam) {
        final String queryParam = String.format("?&email=%s", email);
        final boolean paramExists = Boolean.parseBoolean(localRedirectionParam);
        final String redirectUrl;

        if (paramExists) {
            redirectUrl = configurations.getConfig(CrmUserFields.RESET_PASSWORD_LOCAL_FAILURE_URL.getName());
        } else {
            redirectUrl = HttpUtil.buildAbsoluteUrl(configurations.getConfig(CrmUserFields.RESET_PASSWORD_SERVER_FAILURE_URL.getName()));
        }
        return String.format("%s%s", redirectUrl, queryParam);
    }

    /**
     * <p>
     * Retrieves the user associated with the provided expiry link, and resets the user's password using the provided details.
     * </p>
     *
     * @param orgId              Represents an organization id.
     * @param expiryLinkId       Represents the link expiry details for the password reset.
     * @param forgotPasswordLink Represents the details related to the forgot password expiry.
     * @param data               Represents the request {@link JsonObject} containing the new password and related information.
     * @param response           Represents the response used to send back the results of the operation.
     */
    private void fetchUserAndResetPassword(final long orgId, final long expiryLinkId, final ForgotPasswordLink forgotPasswordLink,
                                           final JsonObject data, final CrmResponse response) {
        final long iamOrgId = organizationService.getIamOrgId(orgId);
        final Long iamUserId = crmUserService.getUser(orgId, forgotPasswordLink.getUser().id()).getIamId();

        /*
         * Fetches the events related to user information.
         */
        final EventCallback<User> userEventCallback = new EventCallback<>() {

            /**
             * <p>
             *     {@inheritDoc}
             * </p>
             *
             * @param user Response from the consumer
             */
            @Override
            public void onSuccess(final User user) {
                final String password = data.getString(UserFields.PASSWORD);
                final String confirmPassword = data.getString(CrmUserFields.CONFIRM_PASSWORD.getName());

                if (password.equals(confirmPassword) && password.matches(User.PASSWORD_PATTERN)
                        && confirmPassword.matches(User.PASSWORD_PATTERN)) {
                    user.setPassword(password);
                    resetPasswordIfUserExists(orgId, expiryLinkId, user, response);
                } else {
                    sendError(response, null, ErrorCode.get(TemporaryUserLinkErrorCode.PASSWORD_MISMATCH, "i18n.user.password.mismatch"));
                }
            }

            /**
             * <p>
             *     {@inheritDoc}
             * </p>
             *
             * @param cause Cause of the failure
             */
            @Override
            public void onFailure(final Throwable cause) {
                sendError(response, null, ErrorCode.get(IamErrorCodes.USER_RETRIEVAL_FAILED, "i18n.iam.user.retrieval.failed", cause));
            }
        };
        iamUserService.fetchUser(iamOrgId, iamUserId, userEventCallback);
    }

    /**
     * <p>
     * Processes and resets the password if the existence check for a user based on the provided expiry link.
     * </p>
     *
     * @param orgId        Represents the organization to which the user belongs.
     * @param expiryLinkId Represents the password reset link associated with the user.
     * @param user         Represents the {@link User} containing the user's information.
     * @param response     Represents the {@link CrmResponse} to be used for sending back the result of the user existence check.
     */
    private void resetPasswordIfUserExists(final long orgId, final long expiryLinkId, final User user, final CrmResponse response) {
        final JsonObject checkUserJson = Json.object();

        checkUserJson.put(UserFields.ORG_ID, user.orgId());
        checkUserJson.put(UserFields.ID, user.id());
        final Event<byte[]> event = new Event<>(IamEvents.CHECK_USER_EXISTENCE, checkUserJson.getBytes());

        /*
         * Checks whether the user exists or not.
         */
        final EventCallback<Boolean> userExistenceCallback = new EventCallback<>() {

            /**
             * <p>
             *     {@inheritDoc}
             * </p>
             *
             * @param isUserExists Response from the consumer
             */
            @Override
            public void onSuccess(final Boolean isUserExists) {

                if (Objects.nonNull(isUserExists) && Boolean.TRUE.equals(isUserExists)) {
                    resetPassword(orgId, expiryLinkId, user, response);
                } else {
                    sendError(response, null, ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, "i18n.iam.accounts.user.not.found"));
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Reset Password: User existence check failed, reason : {}", throwable.getMessage());
                sendError(response, null, ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, "i18n.iam.accounts.user.not.found", throwable));
            }
        };

        EventNotifier.get().send(event, userExistenceCallback);
    }

    /**
     * <p>
     * Resets the password for a user identified by the specified expiry link.
     * </p>
     *
     * @param orgId        Represents an organization to which the user belongs.
     * @param expiryLinkId Represents the password reset link associated with the user.
     * @param user         Represents the {@link User} containing the user's information.
     * @param response     Represents the {@link CrmResponse} to be used for sending back the result of the password reset operation.
     */
    private void resetPassword(final long orgId, final long expiryLinkId, final User user, final CrmResponse response) {

        /*
         * Callback to handle password reset events.
         * This processes requests related to resetting the user's password.
         */
        final EventCallback<JsonObject> passwordResetCallback = new EventCallback<>() {

            /**
             * <p>
             *     {@inheritDoc}
             * </p>
             *
             * @param jsonObject Response from the consumer
             */
            @Override
            public void onSuccess(final JsonObject jsonObject) {
                final String errorKey = CrmUserFields.ERROR.getName();

                if (jsonObject.containsKey(errorKey)) {
                    final JsonArray errorJsonArray = jsonObject.getJsonArray(errorKey);
                    final List<String> errors = new ArrayList<>();
                    final int size = errorJsonArray.size();

                    for (int index = 0; index < size; index++) {
                        errors.add(response.resolveI18nString(errorJsonArray.getString(index)));
                    }
                    response.put(CrmUserFields.ERRORS.getName(), errors);
                } else {
                    temporaryUserLinkManager.delete(orgId, expiryLinkId);
                    response.put(CrmUserFields.MESSAGE.getName(), response.resolveI18nString("i18n.iam.user.password.reset.success"));
                }
                response.send();
            }

            /**
             * <p>
             *     {@inheritDoc}
             * </p>
             *
             * @param throwable Cause of the failure
             */
            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("Reset Password: Password reset action failed, Reason : {}", throwable.getMessage());
                sendError(response, null, ErrorCode.get(IamErrorCodes.PASSWORD_RESET_ACTION_FAILED, "i18n.iam.accounts.password.reset.failed", throwable));
            }
        };

        iamUserService.resetPassword(user, passwordResetCallback);
    }

    /**
     * <p>
     * To ensure secure processing of user tasks. It manages the execution of tasks
     * in a secure context.
     * </p>
     *
     * @author KowsalyaSP
     * @version 1.0
     * @see SecureUserTaskProcessor
     */
    private static class SecureTaskHandler implements SecureUserTaskProcessor {

        private SecureTaskHandler() {

        }

        private static class Instance {
            private static final SecureTaskHandler INSTANCE = new SecureTaskHandler();
        }

        /**
         * <p>
         * Gets the {@link SecureTaskHandler} Instance
         * </p>
         *
         * @return {@link SecureTaskHandler}
         */
        public static SecureTaskHandler getInstance() {
            return Instance.INSTANCE;
        }

        /**
         * <p>
         * {@inheritDoc}
         * </p>
         *
         * @param orgId  Represents the organization ID
         * @param userId Represents the userID
         * @param task   the task to be executed, with the security context value loaded before execution
         */
        @Override
        public void secureProcess(final long orgId, final long userId, final RunnableTask task) {
            SecureUserTaskProcessor.super.secureProcess(orgId, userId, task);
        }
    }
}
