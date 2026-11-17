package com.tlc.web.undertow.cors;

import com.tlc.web.WebExchange;
import com.tlc.web.HttpStatus;
import com.tlc.web.fields.HeaderAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Collection;
import java.util.Objects;

/**
 * Abstract class for implementing {@link PreflightHandler} logic.
 * <p>
 * Provides common utilities or shared logic for handling CORS preflight requests,
 * such as header population, configuration parsing, or standardized error handling.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 * @see PreflightHandler
 */
public abstract class AbstractPreflightHandler implements PreflightHandler {

    /**
     * {@inheritDoc}
     *
     * @param exchange         The current web exchange (request and response).
     * @param body             A byte array representing the body content.
     * @param preflightConfigs The set of CORS access rules to evaluate against.
     */
    @Override
    public void handleAccess(final WebExchange exchange, final byte[] body, final Collection<PreflightConfig> preflightConfigs) {
        final String requestURI = exchange.getURI();

        preflightConfigs.stream().filter(preflightConfig -> Objects.equals(requestURI, preflightConfig.path()))
                .findAny().ifPresentOrElse(preflightConfig -> send(exchange, HttpStatus.NO_CONTENT),
                        () -> send(exchange, HttpStatus.FORBIDDEN));
    }

    /**
     * Sends a response with the specified HTTP status and completes the exchange.
     *
     * @param exchange   The current web exchange containing the request and response.
     * @param httpStatus The HTTP status to send in the response.
     */
    private void send(final WebExchange exchange, final HttpStatus httpStatus) {
        final Map<String, String> headerMap = new HashMap<>();

        headerMap.put(HeaderAttributes.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        headerMap.put(HeaderAttributes.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, OPTIONS");
        exchange.header(HeaderAttributes.ACCESS_CONTROL_REQUEST_HEADERS).findFirst()
                .ifPresent(headerValue -> headerMap.put(HeaderAttributes.ACCESS_CONTROL_ALLOW_HEADERS, headerValue));
        exchange.header(HeaderAttributes.ORIGIN).findFirst()
                .ifPresent(headerValue -> headerMap.put(HeaderAttributes.ACCESS_CONTROL_ALLOW_ORIGIN, headerValue));

        exchange.responseSender().send(headerMap, new byte[0], httpStatus);
    }

}
