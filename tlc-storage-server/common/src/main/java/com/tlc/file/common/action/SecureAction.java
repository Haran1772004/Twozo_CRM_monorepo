package com.tlc.file.common.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;

import com.tlc.file.common.action.req.FileRequest;
import com.tlc.file.common.action.res.FileResponse;
import com.tlc.file.common.fields.WebFields;
import com.tlc.file.common.status.FileErrorCodes;

import com.tlc.iam.client.internal.action.TokenResolverImpl;
import com.tlc.iam.client.models.AuthRequest;

import com.tlc.iam.common.models.User;

import com.tlc.web.HttpStatus;
import com.tlc.web.client.event.EventCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes the requests that are accessing the secured resources
 *
 * @author ThalaimalaiPandiyanT
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
    public final void process(final FileRequest request, final FileResponse response) {
        final AuthRequest authRequest = new AuthRequest(request.cookie(WebFields.ACCESS_TOKEN), request.ipAddress());
        final EventCallback<User> authCallback = new EventCallback<>() {

            @Override
            public void onSuccess(final User user) {
                try {
                    request.setUser(user);

                    RequestService.get().getRequestHandler().handle(() -> {

                        try {
                            sProcess(request, response);
                        } catch (Throwable throwable) {
                            LOGGER.error("{} request Failed, Reason : {}", "API", throwable.getMessage());
                            LOGGER.error("Stack: ", throwable);
                            response.setErrorCode(ErrorCode.get(ErrorCodes.UNKNOWN_ERROR));
                            response.send();
                        }

                        if (!response.isAsync()) {
                            response.send();
                        }
                    });
                } catch (final Throwable throwable) {
                    LOGGER.error("Web request Failed, Reason : {}", throwable.getMessage());
                    LOGGER.debug("Stack: ", throwable);
                    response.setErrorCode(ErrorCode.get(ErrorCodes.UNKNOWN_ERROR));
                    response.send();
                }
            }

            @Override
            public void onFailure(final Throwable throwable) {
                LOGGER.error("Authorization failed, Reason : {}", throwable.getMessage());
                sendError(response, HttpStatus.UNAUTHORIZED, ErrorCode.get(FileErrorCodes.AUTHORIZATION_FAILED, "i18n.iam.accounts.authorization.failed"));
            }
        };

        TokenResolverImpl.getInstance().resolve(authRequest, authCallback);
    }

    /**
     * Handles the secured/authenticated request.
     *
     * @param request  Http request.
     * @param response Http response.
     */
    public abstract void sProcess(final FileRequest request, final FileResponse response);

}
