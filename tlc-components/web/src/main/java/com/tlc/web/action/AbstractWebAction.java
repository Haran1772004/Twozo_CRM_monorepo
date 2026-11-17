package com.tlc.web.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodeUtil;
import com.tlc.commons.json.JsonObject;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.web.Action;
import com.tlc.web.FullBytesCallback;
import com.tlc.web.HttpStatus;
import com.tlc.web.WebExchange;
import com.tlc.web.action.req.HttpRequest;
import com.tlc.web.action.res.HttpResponse;
import com.tlc.web.versioning.VersionFallbackContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Abstract base class for all web actions across microservices.
 * </p>
 *
 * <p>
 * This class provides a unified foundation for handling HTTP requests in a consistent manner
 * across different microservices. It implements the template method pattern to standardize the request
 * processing lifecycle while allowing service-specific customization through abstract methods.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 * @see Action
 * @see FullBytesCallback
 * @see VersionFallbackContext
 */
public abstract class AbstractWebAction<REQ extends HttpRequest, RES extends HttpResponse> implements Action, FullBytesCallback {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractWebAction.class.getName());

    /**
     * {@inheritDoc}
     *
     * <p>
     * Entry point for processing web requests. This method initiates the asynchronous
     * reading of the request body, which upon completion triggers {@link #handle(WebExchange, byte[])}.
     * </p>
     *
     * @param exchange The web exchange containing request and response channels.
     */
    @Override
    public final void process(final WebExchange exchange) {
        exchange.requestReceiver().readBody(this);
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Callback method invoked when the request body has been fully read.
     * This method orchestrates the request processing lifecycle.
     * </p>
     *
     * @param exchange The web exchange containing request and response channels.
     * @param body     The raw request body as a byte array.
     */
    @Override
    public final void handle(final WebExchange exchange, final byte[] body) {
        final RES response = buildResponse(exchange);

        try {
            final REQ request = buildRequest(exchange, body);
            final String type = extractType(request);
            final Optional<VersionFallbackContext> optionalFallbackContext = exchange.getVersionFallbackContext();

            if (optionalFallbackContext.isPresent() && !optionalFallbackContext.get().isTypeHandled(type)) {
                final VersionFallbackContext fallbackContext = optionalFallbackContext.get();

                if (fallbackContext.isTypeHandled(type)) {
                    delegateToFallback(fallbackContext.getFallbackAction(), exchange, body);

                    return;
                }
            }
            process(request, response);
            //VersionMetaResponseBuilder.build(exchange, response); TODO - Version related response data format and data to be discussed with client side.
        } catch (Throwable throwable) {
            sendError(response, HttpStatus.OK, throwable);
        }
    }

    /**
     * <p>
     * Builds the service-specific request object from the raw request data.
     * </p>
     *
     * @param exchange The web exchange containing request metadata and headers.
     * @param body     The raw request body as a byte array.
     * @return The constructed service-specific request object.
     */
    protected abstract REQ buildRequest(WebExchange exchange, byte[] body);


    /**
     * <p>
     * Processes the incoming request.
     * </p>
     *
     * @param request  Http request.
     * @param response Http response.
     * @throws Exception In case any error occurs.
     */
    public abstract void process(final REQ request, final RES response) throws Exception;

    /**
     * <p>
     * Builds the service-specific response object.
     * </p>
     *
     * @param exchange The web exchange containing response metadata.
     * @return The constructed service-specific response object.
     */
    protected abstract RES buildResponse(WebExchange exchange);

    /**
     * <p>
     * Sends the error response back to the client.
     * </p>
     *
     * @param response The {@link RES} to send back to the client.
     * @param status      Represents the status of the response.
     * @param throwable   Represents the error that occurred.
     */
    protected void sendError(final RES response, final HttpStatus status, final Throwable throwable) {
        final String message = throwable.getMessage();
        final ErrorCode errorCode = ErrorCodeUtil.getOrDefaultErrorCode(throwable);

        LOGGER.error("{} request Failed, Reason : {}", ErrorCodeUtil.isErrorCode(throwable) ? "API" : "Web", message);
        LOGGER.error("Stack: ", throwable);

        response.setStatus(Objects.isNull(status) ? HttpStatus.OK : status);
        response.setErrorSummary(new ErrorSummary(new ErrorDescriptor(errorCode)));
        response.setErrorCode(errorCode);
        response.send();
    }

    /**
     * {@inheritDoc}
     */
    protected JsonObject getRequestJson(final REQ request) {
        return request.getRequestJson();
    }

    /**
     * <p>
     * Extracts the operation type from the request.
     * </p>
     *
     * @param request The service-specific request object.
     * @return The operation type string, or null if not present.
     */
    private String extractType(final REQ request) {
        final JsonObject json = getRequestJson(request);

        if (Objects.nonNull(json)) {
            return json.optString("type", null);
        }

        return null;
    }

    /**
     * <p>
     * Delegates the request to the fallback action.
     * </p>
     *
     * @param fallback The fallback action to delegate to.
     * @param exchange The web exchange.
     * @param body     The already-read request body.
     */
    private void delegateToFallback(final Action fallback, final WebExchange exchange, final byte[] body) {

        try {
            exchange.setVersionFallbackContext(null);

            if (fallback instanceof FullBytesCallback callback) {
                callback.handle(exchange, body);
            } else {
                fallback.process(exchange);
            }
        } finally {
            exchange.setVersionFallbackContext(null);
        }
    }
}
