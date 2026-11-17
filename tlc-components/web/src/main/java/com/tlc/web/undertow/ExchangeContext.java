package com.tlc.web.undertow;

import com.tlc.web.undertow.rest.RouteContext;

import io.undertow.server.HttpServerExchange;

/**
 * Represents the context of an HTTP server exchange used for encapsulating details related to an HTTP
 * exchange, including the exchange object, mode of receiving data, whether the
 * data is form-based, and the message's raw byte content.
 *
 * @param exchange        The HTTP server exchange being processed.
 * @param requestReadMode The mode in which the receiver is operating (e.g., receiving data).
 * @param isFormData      Flag indicating whether the data is in form format (true if form data, false otherwise).
 * @param message         The raw byte array containing the message or content being processed.
 * @param routeContext    The {@link RouteContext} containing method, action instance, and route configuration.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
record ExchangeContext(HttpServerExchange exchange, RequestReadMode requestReadMode,
                       boolean isFormData, byte[] message, RouteContext routeContext) {

    ExchangeContext(final HttpServerExchange exchange, final RequestReadMode requestReadMode, final boolean isFormData) {
        this(exchange, requestReadMode, isFormData, null, null);
    }

    ExchangeContext(final HttpServerExchange exchange, final RequestReadMode requestReadMode, final boolean isFormData, final RouteContext routeContext) {
        this(exchange, requestReadMode, isFormData, null, routeContext);
    }

    ExchangeContext(final HttpServerExchange exchange, final RequestReadMode requestReadMode) {
        this(exchange, requestReadMode, false, null, null);
    }

}
