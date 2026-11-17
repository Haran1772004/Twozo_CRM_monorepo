package com.tlc.crm.common.action.iam.web;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.StringUtil;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.events.IamEvents;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.fields.UserFields;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebAction;
import com.tlc.web.client.event.Event;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Handles the HTTP requests that are relevant to login action
 *
 * @author Selvakumar
 * @version 1.0
 */
@WebAction(path = "/accounts/{application}/login")
public final class LoginAction extends AuthenticationAction {

    private final static Logger LOGGER = LoggerFactory.getLogger(LoginAction.class);

    /**
     * Handles the user authentication process
     *
     * @param request  Http request
     * @param response Http response
     */
    @Override
    public void pProcess(final IamHttpRequest request, final IamHttpResponse response) {
        final JsonObject data = request.getRequestJson().getJsonObject(IamFields.DATA);
        final String email = data.optString(UserFields.EMAIL, StringUtil.EMPTY_STRING).trim();

        if (data.optString(UserFields.PASSWORD, StringUtil.EMPTY_STRING).trim().isBlank()) {
            sendError(response, null, ErrorCode.get(IamErrorCodes.LOGIN_PASSWORD_REQUIRED,
                    "i18n.iam.accounts.login.password.required"));
        }

        if (email.isBlank()) {
            sendError(response, null, ErrorCode.get(IamErrorCodes.LOGIN_USER_ID_REQUIRED,
                    "i18n.iam.accounts.login.userId.required"));
        }
        final EventCallback<byte[]> userFetchEventCallBack = new EventCallback<>() {

            @Override
            public void onSuccess(final byte[] userBytes) {
                if (Objects.isNull(userBytes)) {
                    sendError(response, HttpStatus.UNAUTHORIZED, ErrorCode.get(IamErrorCodes.AUTHENTICATION_FAILED, LOGIN_FAILED_I18N_KEY));
                } else {
                    final JsonObject userJson = Json.object(userBytes);

                    if (userJson.containsKey(UserFields.IDP_USER_ID) && !userJson.getBoolean(UserFields.HAS_ACTIVE_PASSWORD)) {
                        sendError(response, null, ErrorCode.get(IamErrorCodes.IDP_USER_ACCOUNT_EXISTS,
                                "i18n.iam.accounts.login.provider.account.exists"));
                    } else {
                        authenticate(Json.object().put(UserFields.USER, userJson), request, response, false);
                    }
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LoggerUtil.logError(LOGGER, throwable, "Login: Authentication Failed, Reason : {}", throwable.getMessage());
                sendError(response, HttpStatus.UNAUTHORIZED, ErrorCode.get(IamErrorCodes.AUTHENTICATION_FAILED, LOGIN_FAILED_I18N_KEY, throwable));
            }
        };

        eventNotifier.send(new Event<>(IamEvents.FETCH_USER,
                Json.object().put(UserFields.EMAIL, email).put(UserFields.PASSWORD_REQUIRED, true).getBytes()), userFetchEventCallBack);
    }
}