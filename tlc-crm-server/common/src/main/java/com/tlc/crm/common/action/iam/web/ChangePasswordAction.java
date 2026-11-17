package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.Env;

import com.tlc.iam.client.internal.action.web.CookieBuilder;
import com.tlc.iam.common.action.PublicAction;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.ClientApplication;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.models.converter.ApplicationConverter;
import com.tlc.iam.common.status.IamErrorCodes;

import com.tlc.web.HttpCookie;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the HTTP requests that are relevant to change password action
 *
 * @author karthick v
 * @version 1.0
 */
@WebAction(path = "/accounts/{application}/changePassword")
public class ChangePasswordAction extends PublicAction {

    private static final String UPDATE_PASSWORD_FAILED_I18N = "i18n.iam.accounts.password.update.failed";
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangePasswordAction.class);
    private static final String EMPTY_STRING = "";
    private static final String ERROR_KEY = "error";
    private static final String STATUS_KEY = "status";
    /**
     * {@inheritDoc}
     *
     * @param request  Http request
     * @param response Http response
     */
    @Override
    public void pProcess(final IamHttpRequest request, final IamHttpResponse response) {
        final JsonObject requestJson = request.getRequestJson();
        final JsonObject data = requestJson.getJsonObject(UserFields.DATA);
        final HttpCookie cookie = request.cookie(IamFields.ACCESS_TOKEN);

        data.put(IamFields.ACCESS_TOKEN, cookie.getValue());

        if (data.getString(UserFields.OLD_PASSWORD).matches(User.PASSWORD_PATTERN) &&
                data.getString(UserFields.NEW_PASSWORD).matches(User.PASSWORD_PATTERN)) {
            final Event<byte[]> event = new Event(IamEvents.CHECK_USER_EXISTENCE, data.getBytes());
            final EventCallback<Boolean> userExistenceCallback = new EventCallback<>() {

                @Override
                public void onSuccess(final Boolean userExists) {
                    if (userExists) {
                        final Event<byte[]> passwordUpdateEvent = new Event<>(IamEvents.CHANGE_PASSWORD, data.getBytes());
                        final EventCallback<byte[]> passwordUpdateCallback = new EventCallback<>() {

                            @Override
                            public void onSuccess(final byte[] bytes) {
                                final JsonObject data = Json.object(bytes);

                                if (data.containsKey(ERROR_KEY)) {
                                    sendError(response, null, ErrorCode.get(IamErrorCodes.PASSWORD_CHANGE_ACTION_FAILED, response.resolveI18nString(data.getString(ERROR_KEY))));
                                } else if (data.containsKey(STATUS_KEY) && data.getBoolean(STATUS_KEY)) {
                                    response.put(IamFields.MESSAGE, response.resolveI18nString("i18n.iam.accounts.password.update.success"));
                                    logout(request, response);
                                } else {
                                    sendError(response, null, ErrorCode.get(IamErrorCodes.PASSWORD_CHANGE_ACTION_FAILED, UPDATE_PASSWORD_FAILED_I18N));
                                }
                            }

                            @Override
                            public void onFailure(final Throwable throwable) {
                                LOGGER.error("Password change Failed, Reason : {}", throwable.getMessage());
                                sendError(response, null, ErrorCode.get(IamErrorCodes.PASSWORD_CHANGE_ACTION_FAILED, UPDATE_PASSWORD_FAILED_I18N, throwable));
                            }
                        };

                        EventNotifier.get().send(passwordUpdateEvent, passwordUpdateCallback);
                    } else {
                        sendError(response, null, ErrorCode.get(IamErrorCodes.USER_NOT_FOUND, "i18n.iam.accounts.user.not.found"));
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LOGGER.error("Password Change Failed, Reason : {}", throwable.getMessage());
                    sendError(response, null, ErrorCode.get(IamErrorCodes.PASSWORD_CHANGE_ACTION_FAILED, UPDATE_PASSWORD_FAILED_I18N, throwable));
                }
            };

            EventNotifier.get().send(event, userExistenceCallback);
        } else {
            sendError(response, null, ErrorCode.get(IamErrorCodes.INVALID_PASSWORD_PATTERN, "i18n.iam.accounts.invalid.password.pattern"));
        }
    }

    /**
     * <p>
     * Does the logout action
     * </p>
     *
     * @param request  Represents the Http request
     * @param response Represents the Http response
     */
    private void logout(final IamHttpRequest request, final IamHttpResponse response) {
        final String applicationName = request.getParameter(IamFields.APPLICATION);
        final ApplicationConverter applicationConverter = ApplicationConverter.getInstance();
        final Event<String> applicationFetchEvent = new Event<>(IamEvents.APPLICATION_FETCH, applicationName);
        final EventCallback<byte[]> applicationFetchCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] data) {
                final ClientApplication clientApplication = applicationConverter.convertToApplication(Json.object(data));

                response.addCookies(CookieBuilder.buildAuthCookie(clientApplication, EMPTY_STRING));

                if (Env.isDevMode()) {
                    response.addCookies(CookieBuilder.buildDevAuthCookie(clientApplication, EMPTY_STRING));
                }
                response.send();
            }

            @Override
            public void onFailure(final Throwable cause) {
                LOGGER.error("Invalid application name, Reason : {}", cause.getMessage());
                sendError(response, null, ErrorCode.get(IamErrorCodes.INVALID_APP_NAME, "i18n.invalid.app.name", cause));
            }
        };

        EventNotifier.get().send(applicationFetchEvent, applicationFetchCallback);
    }
}