package com.tlc.web.undertow.rest;

import com.tlc.web.Action;
import com.tlc.web.WebService;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;

/**
 * Defines the contract for registering and managing REST-based web service endpoints.
 *
 * <p>Main responsibilities include:</p>
 * <ul>
 *     <li>Registering REST endpoints based on {@link ApiRoute} metadata</li>
 *     <li>Associating an executable {@link Action} with the given route</li>
 * </ul>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface RestWebService extends WebService {

    /**
     * Registers a REST endpoint using the metadata provided by the {@link ApiRoute} annotation.
     *
     * @param restApiHandler Refers the {@code RestController}.
     * @param routeContext   The {@link RouteContext} containing method, action instance, and route configuration.
     */
    void register(final RestApiHandler restApiHandler, final RouteContext routeContext);

}
