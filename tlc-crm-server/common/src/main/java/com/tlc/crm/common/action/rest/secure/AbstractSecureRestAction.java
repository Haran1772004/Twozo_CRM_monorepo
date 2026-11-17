package com.tlc.crm.common.action.rest.secure;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeUtil;

import com.tlc.crm.common.action.CrmRequest;
import com.tlc.crm.common.action.CrmResponse;
import com.tlc.crm.common.action.secure.SecureRequestAuthenticator;
import com.tlc.crm.common.action.secure.CrmSecureAction;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import com.tlc.web.HttpStatus;
import com.tlc.web.WebExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

/**
 * Base class for all secure REST actions in the CRM application.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractSecureRestAction extends CrmSecureAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSecureRestAction.class);

    /**
     * {@inheritDoc}
     *
     * @param request  Refers the {@code Request}.
     * @param response Refers the {@code Response} to be sent.
     */
    @Override
    public void sProcess(final CrmRequest request, final CrmResponse response) throws Exception {
        final WebExchange webExchange = request.exchange();

        webExchange.routeDispatcher().findRoute().ifPresentOrElse(routeContext -> {
            try {
                switch (webExchange.httpMethod()) {
                    case POST, GET, PUT, PATCH, DELETE, OPTIONS ->
                            routeContext.method().invoke(routeContext.action(), request, response);
                    default -> sendError(response, null, ErrorCode.get(CommonErrorCodes.ACCESS_DENIED));
                }

                if (!response.isAsync()) {
                    response.send();
                }
            } catch (Throwable throwable) {
                final Throwable cause = throwable.getCause();

                if (cause instanceof ErrorCode errorCode) {
                    sendError(response, HttpStatus.INTERNAL_SERVER_ERROR, errorCode);
                } else {
                    sendError(response, null, throwable);
                }
            }
        }, () -> sendError(response, null, ErrorCode.get(CommonErrorCodes.ACCESS_DENIED)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<SecureRequestAuthenticator> getRequestAuthenticators() {
        return List.of(ApiKeyAuthenticator.get());
    }

    /**
     * <p>
     * Sends the error response back to the client.
     * </p>
     *
     * @param response  The {@code RES} to send back to the client.
     * @param status    Represents the status of the response.
     * @param throwable Represents the error that occurred.
     */
    protected final void sendError(final CrmResponse response, final HttpStatus status, final Throwable throwable) {
        final String message;

        if (throwable instanceof ErrorCode errorCode) {
            message = ErrorCodeUtil.extractErrorMessage(errorCode);
        } else {
            message = throwable.getMessage();
        }

        final ErrorCode errorCode = ErrorCodeUtil.getOrDefaultErrorCode(throwable);

        LOGGER.error("{} request Failed, Reason : {}", ErrorCodeUtil.isErrorCode(throwable) ? "API" : "Web", message);
        LOGGER.error("Stack: ", throwable);

        response.setStatus(Objects.isNull(status) ? HttpStatus.OK : Objects.equals("Invalid JsonObject", message) ? HttpStatus.BAD_REQUEST : status);
        response.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode)));
        response.setErrorCode(errorCode);
        response.send();
    }
}
