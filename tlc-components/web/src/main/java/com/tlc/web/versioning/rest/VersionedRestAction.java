package com.tlc.web.versioning.rest;

import com.tlc.web.Action;
import com.tlc.web.WebExchange;
import com.tlc.web.fields.VersionAttributes;
import com.tlc.web.undertow.rest.RouteContext;
import com.tlc.web.versioning.ApiVersion;
import com.tlc.web.versioning.ApiVersionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * <p>
 * A composite action that dispatches to the appropriate versioned REST action
 * based on the request's {@code X-API-Version} header.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class VersionedRestAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionedRestAction.class);

    private final Map<String, RouteContext> versionedRoutes;
    private final Map<String, ApiVersion> versionAnnotations;
    private final String defaultVersion;
    private final ApiVersionUtils apiVersionUtils;

    private VersionedRestAction(final Map<String, RouteContext> versionedRoutes,
                                final Map<String, ApiVersion> versionAnnotations,
                                final String defaultVersion) {
        this.versionedRoutes = Collections.unmodifiableMap(new TreeMap<>(versionedRoutes));
        this.versionAnnotations = Collections.unmodifiableMap(versionAnnotations);
        this.defaultVersion = defaultVersion;
        this.apiVersionUtils = ApiVersionUtils.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * <p>
     * Resolves the API version from the request header, sets the correct
     * {@link RouteContext} on the exchange via {@link WebExchange#setRestRouteContext},
     * and delegates to the correct versioned action's {@code process()}.
     * </p>
     */
    @Override
    public void process(final WebExchange exchange) {
        final String requestedVersion = apiVersionUtils.resolveVersion(
                exchange.header(VersionAttributes.VERSION_HEADER), defaultVersion);
        final RouteContext resolvedRoute = versionedRoutes.getOrDefault(requestedVersion,
                versionedRoutes.get(defaultVersion));

        if (Objects.isNull(resolvedRoute)) {
            LOGGER.error("API version '{}' not found for REST action. Available versions: {}",
                    requestedVersion, versionedRoutes.keySet());
            return;
        }

        exchange.setRestRouteContext(resolvedRoute);
        resolvedRoute.action().process(exchange);
    }

    /**
     * <p>
     * Gets the {@link ApiVersion} annotation for a specific version.
     * </p>
     *
     * @param version The version string.
     * @return The ApiVersion annotation, or null.
     */
    public ApiVersion getApiVersion(final String version) {
        return versionAnnotations.get(version);
    }

    /**
     * <p>
     * Returns any registered RouteContext (used for Undertow route registration metadata).
     * </p>
     *
     * @return Any RouteContext from the versioned routes.
     */
    public RouteContext anyRouteContext() {
        return versionedRoutes.values().iterator().next();
    }

    /**
     * <p>
     * Creates a new builder.
     * </p>
     *
     * @return A new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * Builder for {@link VersionedRestAction}.
     * </p>
     */
    public static class Builder {

        private final Map<String, RouteContext> versionedRoutes;
        private final Map<String, ApiVersion> versionAnnotations;
        private String defaultVersion;

        private Builder() {
            this.versionedRoutes = new TreeMap<>();
            this.versionAnnotations = new TreeMap<>();
            this.defaultVersion = VersionAttributes.DEFAULT_VERSION;
        }

        /**
         * <p>
         * Adds a versioned route.
         * </p>
         *
         * @param version      The version string.
         * @param routeContext The route context for this version.
         * @param apiVersion   The ApiVersion annotation.
         */
        public void addVersion(final String version, final RouteContext routeContext, final ApiVersion apiVersion) {
            versionedRoutes.put(version, routeContext);

            if (Objects.nonNull(apiVersion)) {
                versionAnnotations.put(version, apiVersion);
            }
        }

        /**
         * <p>
         * Checks if a version is registered in this builder.
         * </p>
         *
         * @param version The version string.
         * @return true if the version exists.
         */
        public boolean hasVersion(final String version) {
            return versionedRoutes.containsKey(version);
        }

        /**
         * <p>
         * Sets the default version.
         * </p>
         *
         * @param defaultVersion The default version string.
         */
        public void setDefaultVersion(final String defaultVersion) {

            if (Objects.nonNull(defaultVersion)) {
                this.defaultVersion = defaultVersion;
            }
        }

        /**
         * <p>
         * Builds the {@link VersionedRestAction}.
         * </p>
         *
         * @return The built VersionedRestAction.
         */
        public VersionedRestAction build() {
            return new VersionedRestAction(versionedRoutes, versionAnnotations, defaultVersion);
        }
    }
}
