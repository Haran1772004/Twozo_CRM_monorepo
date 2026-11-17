package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.identity.service.oidc.fields.OIDCFields;
import com.tlc.identity.service.oidc.model.OIDCProviderUserData;
import com.tlc.identity.service.oidc.provider.OIDCProvider;
import com.tlc.identity.service.oidc.provider.OIDCProvider.Action;
import com.tlc.identity.service.oidc.service.OIDCIdentityService;
import com.tlc.web.HttpMethod;
import com.tlc.web.HttpStatus;
import com.tlc.web.HttpUtil;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;
import com.tlc.web.client.event.EventNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * <p>
 * Handles the provider oauth based login requests
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
@WebAction(path = "/oidc/{application}/login/{provider}", method = HttpMethod.GET)
public class OIDCLoginAction extends AuthenticationAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(OIDCLoginAction.class);

    @Override
    public void pProcess(final IamHttpRequest request, final IamHttpResponse response) throws Exception {
        final OIDCIdentityService identityService = OIDCProvider.get(request.getParameter(OIDCFields.PROVIDER))
                .getIdentityService();
        final String code = request.getParameter(OIDCFields.CODE);

        if (Objects.isNull(code)) {
            redirectHandler.buildRedirectUrlWithState(request, response,
                    identityService.getUserConsentUri(Action.LOGIN));
            response.send();
        } else {
            final OIDCProviderUserData userData = identityService.getUserData(code, Action.LOGIN);
            final EventCallback<byte[]> userFetchEventCallBack = new EventCallback<>() {

                @Override
                public void onSuccess(final byte[] userBytes) {

                    if (Objects.isNull(userBytes)) {
                        redirectHandler.handleClientRedirect(request, response,
                                HttpUtil.buildAbsoluteUrl(iamConfigurations.getConfig(IamFields.REDIRECT_URL_SERVER_ACCOUNT_NOT_EXISTS)),
                                iamConfigurations.getConfig(IamFields.REDIRECT_URL_LOCAL_ACCOUNT_NOT_EXISTS));
                        response.send();
                    } else {
                        final EventCallback<byte[]> idProviderUserLinkEventCallBack = new EventCallback<>() {

                            @Override
                            public void onSuccess(final byte[] dataBytes) {
                                authenticate(Json.object(dataBytes), request, response, true);
                            }

                            @Override
                            public void onFailure(final Throwable cause) {
                                LoggerUtil.logError(LOGGER, cause, "OIDC Login: Provider User Account Link Process Failed, Reason : {}", cause.getMessage());
                                sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.get(IamErrorCodes.SIGNUP_FAILED, LOGIN_FAILED_I18N_KEY));
                            }
                        };

                        eventNotifier.send(new Event<>(IamEvents.LINK_ID_PROVIDER_USER,
                                Json.object().put(UserFields.USER, Json.object(userBytes)).put(UserFields.IDP_USER_ID, userData.userId()).getBytes()), idProviderUserLinkEventCallBack);
                    }
                }

                @Override
                public void onFailure(final Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "OIDC Login: Authentication Failed, Reason : {}", throwable.getMessage());
                    sendError(response, HttpStatus.UNAUTHORIZED, ErrorCode.get(IamErrorCodes.AUTHENTICATION_FAILED, LOGIN_FAILED_I18N_KEY, throwable));
                }
            };

            EventNotifier.get().send(new Event<>(IamEvents.FETCH_USER,
                    Json.object().put(UserFields.EMAIL, userData.emailAddress()).put(UserFields.PASSWORD_REQUIRED, false).getBytes()), userFetchEventCallBack);
        }
    }
}
