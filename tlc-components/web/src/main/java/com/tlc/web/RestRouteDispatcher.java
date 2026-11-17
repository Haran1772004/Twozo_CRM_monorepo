package com.tlc.web;

import com.tlc.web.undertow.rest.RouteContext;

import java.util.Optional;

/**
 * Responsible for resolving and dispatching REST API routes.
 * <p>
 * A {@code RestRouteDispatcher} is used during request processing to locate the
 * appropriate {@link RouteContext} based on the incoming HTTP method and path.
 * Once a matching route is found, the caller can invoke the associated handler
 * method using the information stored in the context.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface RestRouteDispatcher {

    /**
     * Attempts to find a matching route for the current request.
     * <p>
     * The implementation should use the request information (HTTP method, path,
     * and other routing metadata) available in the surrounding routing context
     * to determine which {@link RouteContext} should be returned.
     * </p>
     *
     * @return An {@link Optional} containing the matching {@link RouteContext}, or an empty optional if no route matches the incoming request.
     */
    Optional<RouteContext> findRoute();
}
