package com.tlc.web.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.web.HttpMethod;
import com.tlc.web.fields.VersionAttributes;
import com.tlc.web.status.WebErrorCodes;
import com.tlc.web.undertow.rest.RouteContext;
import com.tlc.web.undertow.rest.annotation.ApiRoute;
import com.tlc.web.undertow.rest.annotation.RestApiHandler;
import com.tlc.web.versioning.ApiVersion;
import com.tlc.web.versioning.rest.VersionedRestAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Registry for managing versioned REST actions during application startup.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 * @see VersionedRestAction
 */
public final class VersionedRestActionRegistry {

    private static final VersionedRestActionRegistry INSTANCE = new VersionedRestActionRegistry();

    private final Map<RouteKey, VersionedRestAction.Builder> registryBuilders;
    private final Map<RouteKey, String> declaredDefaultVersions;
    private final Map<String, RestApiHandler> handlersByPath;

    private VersionedRestActionRegistry() {
        this.registryBuilders = new ConcurrentHashMap<>();
        this.declaredDefaultVersions = new ConcurrentHashMap<>();
        this.handlersByPath = new ConcurrentHashMap<>();
    }

    public static VersionedRestActionRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * <p>
     * Registers a versioned REST route.
     * </p>
     *
     * @param restApiHandler The {@link RestApiHandler} annotation from the class.
     * @param apiVersion     The {@link ApiVersion} annotation from the class.
     * @param apiRoute       The {@link ApiRoute} annotation from the method.
     * @param routeContext   The {@link RouteContext} for this version.
     */
    public void register(final RestApiHandler restApiHandler, final ApiVersion apiVersion,
                         final ApiRoute apiRoute, final RouteContext routeContext) {
        final String version = apiVersion.value();
        final RouteKey routeKey = new RouteKey(restApiHandler.path(), apiRoute.path(), apiRoute.method());

        handlersByPath.putIfAbsent(restApiHandler.path(), restApiHandler);

        registryBuilders.computeIfAbsent(routeKey, k -> VersionedRestAction.builder())
                .addVersion(version, routeContext, apiVersion);

        if (apiVersion.isDefaultVersion()) {
            resolveDefaultVersion(routeKey, restApiHandler.path(), version);
        }
    }

    /**
     * <p>
     * Builds all registered versioned REST actions, grouped by {@link RestApiHandler}.
     * Applies declared default versions before building.
     * </p>
     *
     * @return Map of RestApiHandler → Collection of built {@link VersionedRestAction} instances.
     */
    public Map<RestApiHandler, Collection<VersionedRestAction>> buildAll() {
        final Map<RestApiHandler, Collection<VersionedRestAction>> result = new HashMap<>();

        for (final Map.Entry<RouteKey, VersionedRestAction.Builder> entry : registryBuilders.entrySet()) {
            final RouteKey routeKey = entry.getKey();
            final RestApiHandler restApiHandler = handlersByPath.get(routeKey.restPath());
            final VersionedRestAction.Builder builder = entry.getValue();
            final String defaultVersion = declaredDefaultVersions.getOrDefault(routeKey, VersionAttributes.DEFAULT_VERSION);

            if (!declaredDefaultVersions.containsKey(routeKey) && !builder.hasVersion(VersionAttributes.DEFAULT_VERSION)) {
                throw ErrorCode.get(WebErrorCodes.VERSIONED_ACTION_NOT_FOUND, "Default version for this path not found");
            }

            builder.setDefaultVersion(defaultVersion);

            result.computeIfAbsent(restApiHandler, k -> new ArrayList<>())
                    .add(entry.getValue().build());
        }

        return result;
    }

    /**
     * <p>
     * Clears the registry.
     * </p>
     */
    public void clear() {
        registryBuilders.clear();
        declaredDefaultVersions.clear();
        handlersByPath.clear();
    }

    /**
     * <p>
     * Resolves and validates the default version for a given route key when
     * {@link ApiVersion#isDefaultVersion()} is {@code true}.
     * </p>
     *
     * @param routeKey The composite route key.
     * @param restPath The REST handler path, used for error messages.
     * @param version  The resolved version string from {@link ApiVersion#value()}.
     * @throws IllegalStateException if a second class on the same path declares isDefaultVersion = true.
     */
    private void resolveDefaultVersion(final RouteKey routeKey, final String restPath, final String version) {
        final String existing = declaredDefaultVersions.put(routeKey, version);

        if (Objects.nonNull(existing)) {
            throw ErrorCode.get(WebErrorCodes.VERSIONED_ACTION_NOT_FOUND,
                    String.format("isDefaultVersion already declared for REST path '%s' route '%s %s' as version '%s'. " +
                            "isDefaultVersion must be set on exactly one class per path.",
                            restPath, routeKey.method(), routeKey.apiRoutePath(), existing));
        }
    }

    /**
     * <p>
     * Composite key for REST handler path, API route path, and HTTP method.
     * </p>
     *
     * @param restPath     The REST handler path.
     * @param apiRoutePath The API route path.
     * @param method       The HTTP method.
     */
    public record RouteKey(String restPath, String apiRoutePath, HttpMethod method) {

    }
}
