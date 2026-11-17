package com.tlc.web.undertow.rest;

import com.tlc.web.Action;
import com.tlc.web.undertow.rest.annotation.ApiRoute;

import java.lang.reflect.Method;

/**
 * Represents a resolved route mapping within the REST router.
 *
 * <p>This context is stored in the routing table and retrieved during
 * request dispatch to identify the correct endpoint to invoke.</p>
 *
 * @param annotation The {@link ApiRoute} annotation defining the route path and HTTP method.
 * @param method     The {@code Method} associated with the route.
 * @param action     The instantiated action/controller containing the method.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record RouteContext(ApiRoute annotation, Method method, Action action) {

}
