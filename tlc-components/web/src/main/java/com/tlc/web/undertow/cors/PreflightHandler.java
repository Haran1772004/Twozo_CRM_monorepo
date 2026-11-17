package com.tlc.web.undertow.cors;

import com.tlc.web.WebExchange;

import java.util.Collection;

/**
 * Defines the contract for handling CORS preflight requests.
 * <p>
 * Implementations are responsible for evaluating access rules
 * and preparing the necessary headers for valid OPTIONS requests.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface PreflightHandler {

    /**
     * Evaluates the given preflight configurations and determines
     * whether access should be granted for the incoming request.
     *
     * @param exchange         The current HTTP exchange (request and response).
     * @param body             A byte array representing the body content.
     * @param preflightConfigs The set of CORS access rules to evaluate against.
     */
    void handleAccess(final WebExchange exchange, final byte[] body, final Collection<PreflightConfig> preflightConfigs);

    /**
     * Determines whether access should be granted for the incoming request based on the provided
     * {@link WebExchange}, request body, and the applicable preflight configurations.
     *
     * <p>This method is typically used to evaluate domain-based or type-based access control
     * as part of a CORS or preflight validation process.
     *
     * @param exchange         The web exchange containing request and response details.
     * @param body             A byte array representing the body content.
     * @param preflightConfigs The collection of configured preflight rules to evaluate against.
     * @return {@code true} if access is permitted according to the configured rules; {@code false} otherwise.
     */
    boolean isAccessAllowed(final WebExchange exchange, final byte[] body, final Collection<PreflightConfig> preflightConfigs);

}
