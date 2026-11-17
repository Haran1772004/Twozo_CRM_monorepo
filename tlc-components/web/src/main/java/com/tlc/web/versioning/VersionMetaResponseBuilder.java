package com.tlc.web.versioning;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.web.WebExchange;
import com.tlc.web.action.res.HttpResponse;
import com.tlc.web.fields.VersionAttributes;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
/**
 * <p>
 * Utility for adding API versioning metadata to responses.
 * </p>
 *
 * <p>
 * This class provides centralized handling for including deprecation warnings,
 * version information, and successor version details in API responses.
 * It reads the {@link DeprecationContext} stored on the exchange by
 * {@code VersionedAction} during version resolution and enriches the response
 * body with relevant versioning metadata.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public final class VersionMetaResponseBuilder {

    private VersionMetaResponseBuilder() {
    }

    /**
     * <p>
     * Enriches the response with API versioning metadata based on the
     * {@link DeprecationContext} stored on the exchange during version resolution.
     * </p>
     *
     * <p>
     * This method should be called after the main action processing is complete to add
     * version-related information to the response body.
     * </p>
     *
     * @param exchange The web exchange containing version context.
     * @param response The HttpResponse object to enrich.
     */
    public static void build(final WebExchange exchange, final HttpResponse response) {

        if (Objects.isNull(response)) {
            return;
        }

        final JsonObject versionMetadata = build(exchange);

        if (!Json.isEmpty(versionMetadata)) {
            response.put("versionInfo", versionMetadata);
        }
    }

    /**
     * <p>
     * Builds version metadata from the {@link DeprecationContext} stored on the exchange.
     * </p>
     *
     * @param exchange The web exchange to extract version context from.
     * @return JsonObject containing version metadata, or {@code null} if no deprecation context is present.
     */
    private static JsonObject build(final WebExchange exchange) {
        final Optional<DeprecationContext> optionalDeprecationContext = exchange.getDeprecationContext();

        if (optionalDeprecationContext.isEmpty()) {
            return null;
        }

        final DeprecationContext deprecationContext = optionalDeprecationContext.get();
        final JsonObject metadata = Json.object();
        final JsonObject deprecationInfo = Json.object();

        deprecationInfo.put(VersionAttributes.IS_DEPRECATED_KEY, true);
        deprecationInfo.put(VersionAttributes.MESSAGE_KEY, deprecationContext.message());

        if (Objects.nonNull(deprecationContext.successorVersion())) {
            deprecationInfo.put(VersionAttributes.SUCCESSOR_VERSION_KEY, deprecationContext.successorVersion());
        }

        if (deprecationContext.isRedirected()) {
            deprecationInfo.put(VersionAttributes.REDIRECTED_FROM_KEY, deprecationContext.deprecatedVersion());
            deprecationInfo.put(VersionAttributes.REDIRECTED_TO_KEY, deprecationContext.redirectedToVersion());
        }

        metadata.put(VersionAttributes.DEPRECATION_KEY, deprecationInfo);

        final Stream<String> versionHeader = exchange.header(VersionAttributes.VERSION_HEADER);

        if (Objects.nonNull(versionHeader)) {
            versionHeader.findFirst().ifPresent(value -> metadata.put(VersionAttributes.API_VERSION_KEY, value));
        }

        return metadata;
    }
}