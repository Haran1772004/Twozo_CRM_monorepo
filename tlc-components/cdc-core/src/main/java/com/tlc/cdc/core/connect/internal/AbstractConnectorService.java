package com.tlc.cdc.core.connect.internal;

import com.tlc.cdc.core.connect.ConnectorConfig;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base implementation for Kafka Connect connector services. This class provides common functionality for managing Kafka Connect
 * connectors and serves as a base for both <b>source</b> and <b>sink</b> connector service implementations.
 *
 * @param <C> The type of {@link ConnectorConfig} used by the connector.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractConnectorService<C extends ConnectorConfig> implements ConnectorService<C> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractConnectorService.class);

    protected AbstractConnectorService() {
    }

    /**
     * Sends an HTTP request to the given Kafka Connect REST endpoint.
     *
     * @param url         The full Kafka Connect REST endpoint URL.
     * @param httpMethod  The HTTP method to use (POST, PUT, GET, etc.).
     * @param requestBody The request payload as a {@link JsonObject}, or {@code null} if not required.
     * @return The response body as a {@link JsonObject}.
     */
    protected final JsonObject connect(final String url, final HttpMethod httpMethod, final JsonObject requestBody) {
        final Map<String, String> headers = new HashMap<>();
        final Map<String, String> requestParams = new HashMap<>();

        headers.put("Content-Type", "application/json");
        requestParams.put("requestBody", requestBody.toString());

        try {
            return HttpUtil.makeRequest(new HttpRequest(new URL(url), requestParams, httpMethod, headers));
        } catch (Exception exception) {
            LOGGER.error("Kafka Connect: Unable to establish a connection with Kafka Connect for connector registration. Reason: {}", exception.getMessage(), exception);
            return Json.emptyObject();
        }
    }

    /**
     * Deletes a Kafka Connect connector by name.
     *
     * @param url           The Kafka Connect REST base URL.
     * @param connectorName The name of the connector to delete.
     */
    protected final void delete(final String url, final String connectorName) {
        try {
            HttpUtil.makeRequest(new HttpRequest(new URL(String.join("/", url, connectorName)), null, HttpMethod.DELETE, null));
        } catch (Exception exception) {
            LOGGER.error("Kafka Connect: Failed to delete the connector. Reason: {}", exception.getMessage(), exception);
        }
    }

    /**
     * Restarts a Kafka Connect connector.
     *
     * @param url           The Kafka Connect REST base URL.
     * @param connectorName The name of the connector to restart.
     * @return The restart response as a {@link JsonObject}.
     */
    protected final JsonObject restart(final String url, final String connectorName) {
        try {
            return HttpUtil.makeRequest(new HttpRequest(new URL(String.join("/", url, connectorName, "restart")), null, HttpMethod.POST, null));
        } catch (Exception exception) {
            LOGGER.error("Kafka Connect: Failed to restart the connectors. Reason: {}", exception.getMessage(), exception);
            return Json.emptyObject();
        }
    }

    /**
     * Fetches the current status of a Kafka Connect connector.
     *
     * @param url           The Kafka Connect REST base URL.
     * @param connectorName The name of the connector.
     * @return The connector status as a {@link JsonObject}.
     */
    protected final JsonObject status(final String url, final String connectorName) {
        try {
            return HttpUtil.makeRequest(new HttpRequest(new URL(String.join("/", url, connectorName, "status")), null, HttpMethod.GET, null));
        } catch (Exception exception) {
            LOGGER.error("Kafka Connect: Failed to check the connector status. Reason: {}", exception.getMessage(), exception);
            return Json.emptyObject();
        }
    }
}
