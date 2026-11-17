package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.commons.util.StringUtil;
import com.tlc.iam.client.internal.action.web.CookieBuilder;
import com.tlc.iam.common.ApplicationFields;
import com.tlc.iam.common.models.ClientApplication;
import com.tlc.iam.common.models.converter.ApplicationConverter;
import com.tlc.iam.common.action.PublicAction;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.HttpCookie;
import com.tlc.web.HttpMethod;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Handles the logout action
 *
 * @author Selvakumar
 * @version 1.0
 */
@WebAction(path = "/accounts/{application}/logout", method = HttpMethod.GET)
public final class LogoutAction extends PublicAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutAction.class);

    private final Configurations webConfigurations;

    public LogoutAction() {
        this.webConfigurations = ConfHolder.getInstance().getProperties(ApplicationFields.WEB);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pProcess(final IamHttpRequest request, final IamHttpResponse response) {
        final HttpCookie cookie = request.cookie(IamFields.ACCESS_TOKEN);

        if (Objects.nonNull(cookie)) {
            final JsonObject logoutData = Json.object();

            logoutData.put(IamFields.ACCESS_TOKEN, cookie.getValue());

            final Event<byte[]> logoutEvent = new Event<>(IamEvents.SIGN_OUT, logoutData.getBytes());
            final EventCallback<Boolean> logoutCallback = new EventCallback<>() {

                @Override
                public void onSuccess(Boolean logoutSuccess) {
                    doLogout(request, response);
                }

                @Override
                public void onFailure(Throwable throwable) {
                    LOGGER.error("Logout action failed. App : {}, Error : {}", request.getParameter(IamFields.APPLICATION), throwable.getMessage());
                    doLogout(request, response);

                }
            };

            EventNotifier.get().send(logoutEvent, logoutCallback);
        } else {
            doLogout(request, response);
        }
    }

    /**
     * Does the logout action
     *
     * @param request Http request
     * @param response Http response
     */
    private void doLogout(final IamHttpRequest request, final IamHttpResponse response) {
        final String applicationName = request.getParameter(IamFields.APPLICATION);
        final ApplicationConverter applicationConverter = ApplicationConverter.getInstance();
        final Event<String> applicationFetchEvent = new Event<>(IamEvents.APPLICATION_FETCH, applicationName);
        final EventCallback<byte[]> applicationFetchCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] data) {
                final JsonObject applicationJson = Json.object(data);

                applicationJson.put(ApplicationFields.COOKIE_DOMAIN, webConfigurations.getConfig(ApplicationFields.COOKIE_DOMAIN));
                applicationJson.put(ApplicationFields.SAME_SITE_MODE, ApplicationFields.NONE);
                final ClientApplication clientApplication = applicationConverter.convertToApplication(applicationJson);

                response.addCookies(CookieBuilder.buildAuthCookie(clientApplication, StringUtil.EMPTY_STRING));
                applicationJson.put(ApplicationFields.COOKIE_DOMAIN, StringUtil.EMPTY_STRING);
                response.addCookies(CookieBuilder.buildAuthCookie(applicationConverter.convertToApplication(applicationJson), StringUtil.EMPTY_STRING));
                response.addCookies(CookieBuilder.buildDevAuthCookie(clientApplication, StringUtil.EMPTY_STRING));
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