package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.action.IamRedirectHandler;
import com.tlc.iam.client.internal.action.web.CookieBuilder;
import com.tlc.iam.common.ApplicationFields;
import com.tlc.iam.common.action.PublicAction;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.models.ClientApplication;
import com.tlc.iam.common.models.converter.ApplicationConverter;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.HttpStatus;
import com.tlc.web.HttpUtil;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <p>
 * Handles the user login related actions
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
abstract class AuthenticationAction extends PublicAction {

    protected static final String LOGIN_FAILED_I18N_KEY = "i18n.iam.accounts.login.failed";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationAction.class);
    private static final String LOGIN_SUCCESS_I18N_KEY = "i18n.iam.accounts.login.success";

    protected final EventNotifier eventNotifier;
    protected final Configurations iamConfigurations;
    protected final IamRedirectHandler redirectHandler;

    private final Configurations webConfigurations;
    private final ApplicationConverter applicationConverter;

    protected AuthenticationAction() {
        this.eventNotifier = EventNotifier.get();
        this.iamConfigurations = ConfHolder.getInstance().getProperties(IamFields.IAM);
        this.redirectHandler = IamRedirectHandler.getInstance();
        this.webConfigurations = ConfHolder.getInstance().getProperties(ApplicationFields.WEB);
        this.applicationConverter = ApplicationConverter.getInstance();
    }

    /**
     * <p>
     * Authenticates the user
     * </p>
     *
     * @param dataJson   Represents the data json with user
     * @param request    Represents the {@link IamHttpRequest}
     * @param response   Represents the {@link IamHttpResponse}
     */
    protected void authenticate(final JsonObject dataJson, final IamHttpRequest request,
                                final IamHttpResponse response, final boolean isRedirectRequired) {
        final String applicationName = request.getParameter(IamFields.APPLICATION);
        final EventCallback<byte[]> applicationFetchEventCallback = new EventCallback<>() {

            @Override
            public void onSuccess(byte[] applicationBytes) {
                final EventCallback<byte[]> loginEventCallback = new EventCallback<>() {

                    @Override
                    public void onSuccess(final byte[] tokenBytes) {

                        if (Objects.nonNull(tokenBytes) && tokenBytes.length > NumericalConstant.ZERO) {

                            try {
                                final String accessToken = Json.object(tokenBytes).getString(IamFields.ACCESS_TOKEN);
                                final JsonObject applicationJson = Json.object(applicationBytes)
                                        .put(ApplicationFields.COOKIE_DOMAIN, webConfigurations.getConfig(ApplicationFields.COOKIE_DOMAIN))
                                        .put(ApplicationFields.SAME_SITE_MODE, ApplicationFields.NONE);
                                final ClientApplication application = applicationConverter.convertToApplication(applicationJson);

                                response.addCookies(CookieBuilder.buildAuthCookie(application, accessToken));
                                applicationJson.put(ApplicationFields.COOKIE_DOMAIN, StringUtil.EMPTY_STRING);
                                response.addCookies(CookieBuilder.buildAuthCookie(applicationConverter.convertToApplication(applicationJson), accessToken));
                                response.addCookies(CookieBuilder.buildDevAuthCookie(application, accessToken));

                                if (isRedirectRequired) {
                                    redirectHandler.handleClientRedirect(request, response,
                                            HttpUtil.buildAbsoluteUrl(StringUtil.EMPTY_STRING),
                                            iamConfigurations.getConfig(IamFields.REDIRECT_URL_LOCAL_LOGIN_SUCCESS)
                                    );
                                } else {
                                    response.put(IamFields.MESSAGE, response.resolveI18nString(LOGIN_SUCCESS_I18N_KEY));
                                }

                                response.send();
                            } catch (final Throwable throwable) {
                                sendError(response, null, ErrorCode.get(IamErrorCodes.AUTHENTICATION_FAILED, LOGIN_FAILED_I18N_KEY, throwable));
                            }
                        } else {
                            sendError(response, HttpStatus.UNAUTHORIZED, ErrorCode.get(IamErrorCodes.AUTHENTICATION_FAILED, LOGIN_FAILED_I18N_KEY));
                        }
                    }

                    @Override
                    public void onFailure(final Throwable throwable) {
                        LoggerUtil.logError(LOGGER, throwable, "Authentication: Login action failed, Reason : {}", throwable.getMessage());
                        sendError(response, HttpStatus.UNAUTHORIZED,
                                IamErrorCodes.SESSION_LIMIT_REACHED.name().equals(throwable.getMessage()) ?
                                ErrorCode.get(IamErrorCodes.SESSION_LIMIT_REACHED, "i18n.iam.accounts.maximum.session.limit.reached") :
                                ErrorCode.get(IamErrorCodes.AUTHENTICATION_FAILED, LOGIN_FAILED_I18N_KEY));
                    }
                };
                final JsonObject data = request.getRequestJson().optJsonObject(IamFields.DATA);

                eventNotifier.send(new Event<>(IamEvents.AUTHENTICATE_USER,
                        buildUserLoginData(applicationName, dataJson.getJsonObject(UserFields.USER), request.getUserAgent().toJson(),
                                Objects.isNull(data) ? null : data.optString(UserFields.PASSWORD, StringUtil.EMPTY_STRING).trim(),
                                dataJson.optString(UserFields.IDP_USER_ID, null)).getBytes()), loginEventCallback);
            }

            @Override
            public void onFailure(final Throwable cause) {
                LoggerUtil.logError(LOGGER, cause, "Authentication: Invalid application name, Application Name: {}, Reason : {}", applicationName, cause.getMessage());
                sendError(response, null, ErrorCode.get(IamErrorCodes.INVALID_APP_NAME, "i18n.invalid.app.name", cause));
            }
        };

        eventNotifier.send(new Event<>(IamEvents.APPLICATION_FETCH, applicationName), applicationFetchEventCallback);
    }

    /**
     * <p>
     * Builds the {@link JsonObject} with data required for user login
     * </p>
     *
     * @param applicationName Represents the application name
     * @param user            Represents the user
     * @param userAgent       Represents the {@link com.tlc.web.UserAgent}
     * @param password        Represents the user password
     * @param idpUserId       Represents the provider user ID
     * @return the {@link JsonObject} with data required for user login
     */
    private JsonObject buildUserLoginData(final String applicationName, final JsonObject user, final JsonObject userAgent,
                                          final String password, final String idpUserId) {
        final JsonObject userLoginData = Json.object()
                .put(IamFields.APPLICATION, applicationName.toUpperCase())
                .put(UserFields.USER, user)
                .put(UserFields.USER_AGENT, userAgent);

        if (Objects.isNull(password)) {
            userLoginData.put(UserFields.IDP_USER_ID, idpUserId);
        } else {
            userLoginData.put(UserFields.PASSWORD, password);
        }

        return userLoginData;
    }
}
