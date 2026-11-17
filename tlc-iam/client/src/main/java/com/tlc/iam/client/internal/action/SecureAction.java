package com.tlc.iam.client.internal.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.iam.client.models.AuthRequest;
import com.tlc.iam.common.action.WebActionProcessor;
import com.tlc.iam.common.action.req.IamHttpRequest;
import com.tlc.iam.common.action.res.IamHttpResponse;
import com.tlc.iam.common.fields.IamFields;
import com.tlc.iam.common.models.User;
import com.tlc.iam.common.status.IamErrorCodes;
import com.tlc.web.HttpStatus;
import com.tlc.web.client.event.EventCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes the requests that are accessing the secured resources
 *
 * @author Selvakumar
 * @version 1.0
 */
public abstract class SecureAction extends WebActionProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecureAction.class);

    /**
     * Does the base level operations like authentication, authorization before processing the http request
     *
     * @param request  Http request
     * @param response Http response
     */
    @Override
    public final void process(final IamHttpRequest request, final IamHttpResponse response) {
        final AuthRequest authRequest = new AuthRequest(request.cookie(IamFields.ACCESS_TOKEN), request.ipAddress());
        final EventCallback<User> authCallback = new EventCallback<>() {
            @Override
            public void onSuccess(final User user) {
                try {
                    request.setUser(user);
                    sProcess(request, response);
                } catch (final Throwable throwable) {
                    LOGGER.error("Web request Failed, Reason : {}", throwable.getMessage());
                    LOGGER.debug("Stack : ", throwable);
                    response.setErrorCode(ErrorCode.get(ErrorCodes.UNKNOWN_ERROR, throwable.getMessage()));
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("Authorization failed, Reason : {}", throwable.getMessage());
                sendError(response, HttpStatus.UNAUTHORIZED, ErrorCode.get(IamErrorCodes.AUTHORIZATION_FAILED, "i18n.iam.accounts.authorization.failed", throwable));
            }
        };

        TokenResolverImpl.getInstance().resolve(authRequest, authCallback);
    }

    /**
     * Handles the secured/authenticated request
     *
     * @param request Http request
     * @param response Http response
     */
    public abstract void sProcess(IamHttpRequest request, IamHttpResponse response);

}
