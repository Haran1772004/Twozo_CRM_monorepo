package com.tlc.web.versioning;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.web.Action;
import com.tlc.web.WebExchange;

import com.tlc.web.fields.HeaderAttributes;
import com.tlc.web.fields.VersionAttributes;
import com.tlc.web.status.WebErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * <p>
 * A composite action that dispatches to the appropriate versioned action
 * based on the request's API version header.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public final class VersionedAction implements Action {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionedAction.class);

    private final String path;
    private final String defaultVersion;
    private final Map<String, VersionedActionEntry> versionedActions;
    private final ApiVersionUtils apiVersionUtils;

    private VersionedAction(final String path, final Map<String, VersionedActionEntry> versionedActions,
                            final String defaultVersion) {
        this.path = path;
        this.versionedActions = Collections.unmodifiableMap(new TreeMap<>(versionedActions));
        this.defaultVersion = defaultVersion;
        this.apiVersionUtils = ApiVersionUtils.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final WebExchange exchange) {
        final VersionedActionEntry resolvedVersionEntry = resolveVersion(
                exchange, apiVersionUtils.resolveVersion(exchange.header(VersionAttributes.VERSION_HEADER), defaultVersion));

        if (Objects.isNull(resolvedVersionEntry)) {
            return;
        }
        exchange.header(VersionAttributes.VERSION_HEADER, resolvedVersionEntry.version());

        if (isFallbackRequired(resolvedVersionEntry)) {
            final String fallbackVersion = resolveFallbackVersion(resolvedVersionEntry);
            final VersionedActionEntry fallbackEntry = versionedActions.get(fallbackVersion);

            if (Objects.nonNull(fallbackEntry)) {
                final Set<String> handledTypes = Objects.nonNull(resolvedVersionEntry.apiVersion())
                        ? Set.of(resolvedVersionEntry.apiVersion().handles()) : Set.of();
                exchange.setVersionFallbackContext(new VersionFallbackContext(fallbackEntry, handledTypes));
            }
        }

        resolvedVersionEntry.action().process(exchange);
    }

    /**
     * <p>
     * Returns the path this versioned action handles.
     * </p>
     *
     * @return The path string
     */
    public String getPath() {
        return path;
    }

    /**
     * <p>
     * Creates a new builder for VersionedAction.
     * </p>
     *
     * @param path The path this action handles
     * @return A new builder
     */
    public static Builder builder(final String path) {
        return new Builder(path);
    }

    /**
     * <p>
     * Gets the action for a specific version.
     * </p>
     *
     * @param version The version string
     * @return Optional containing the action if found
     */
    public Optional<Action> getAction(final String version) {
        final VersionedActionEntry entry = versionedActions.get(version);

        return Objects.isNull(entry) ? Optional.empty() : Optional.ofNullable(entry.action);
    }

    /**
     * <p>
     * Resolves the best matching action entry for the requested version.
     * </p>
     *
     * @param requestedVersion Requested version string from the header.
     * @return The best matching VersionedActionEntry or the default version entry if no match is found.
     */
    private VersionedActionEntry resolveAction(final String requestedVersion) {
        final VersionedActionEntry entry = versionedActions.get(requestedVersion);

        if (Objects.nonNull(entry)) {
            return entry;
        }

        String bestMatch = null;

        for (final String version : versionedActions.keySet()) {
            if (isVersionCompatible(version, requestedVersion)) {
                if (Objects.isNull(bestMatch) || compareVersions(version, bestMatch) > NumericalConstant.ZERO) {
                    bestMatch = version;
                }
            }
        }

        if (Objects.nonNull(bestMatch)) {
            return versionedActions.get(bestMatch);
        }

        return versionedActions.get(defaultVersion);
    }

    /**
     * <p>
     * Resolves deprecation by either redirecting to successor version or handling deprecation warnings.
     * </p>
     *
     * @param exchange         The WebExchange to add headers to.
     * @param requestedVersion The originally requested Versioned.
     * @return The actual VersionedActionEntry to use (either successor or original).
     */
    private VersionedActionEntry resolveVersion(final WebExchange exchange, final String requestedVersion) {
        final VersionedActionEntry requestedEntry = resolveAction(requestedVersion);

        if (Objects.isNull(requestedEntry)) {
            handleVersionNotFound(exchange, requestedVersion);

            return null;
        }

        if (Objects.isNull(requestedEntry.apiVersion()) || !requestedEntry.apiVersion().deprecated()) {
            return requestedEntry;
        }
        final String successorVersion = requestedEntry.apiVersion().successor();

        if (successorVersion.isEmpty()) {
            //handleDeprecation(exchange, requestedEntry);

            return requestedEntry;
        } else {
            final VersionedActionEntry successorEntry = versionedActions.get(successorVersion);

            if (Objects.isNull(successorEntry)) {
                //handleDeprecation(exchange, requestedEntry); TODO - Need not to handle deprecation related cases for now.

                return requestedEntry;
            }
            //handleDeprecationWithRedirect(exchange, requestedEntry, successorEntry);

            return successorEntry;
        }
    }

    /**
     * <p>
     * Resolves the fallback version for a sparse versioned action entry.
     * </p>
     *
     * <p>
     * Resolution order:
     * 1. If {@code fallbackVersion} is set on the entry's {@link ApiVersion}, use it.
     * 2. Otherwise, use the default version (from {@code isDefaultVersion = true}).
     * 3. If neither exists, falls back to "1".
     * </p>
     *
     * @param entry The sparse versioned action entry.
     * @return The resolved fallback version string.
     */
    private String resolveFallbackVersion(final VersionedActionEntry entry) {
        if (Objects.nonNull(entry.apiVersion()) && !entry.apiVersion().fallbackVersion().isEmpty()) {
            return entry.apiVersion().fallbackVersion();
        }

        return defaultVersion;
    }

    /**
     * <p>
     * Checks if fallback is required for the given entry.
     * </p>
     *
     * <p>
     * Fallback is required in two distinct cases:
     * <ul>
     *     <li>If {@code handles()} is empty and {@code fallbackVersion} is explicitly set — redirect to that version.</li>
     *     <li>If {@code handles()} is non-empty — fallback is triggered for non-handled type requests.</li>
     * </ul>
     * </p>
     *
     * @param entry The versioned action entry.
     * @return true if fallback context should be set.
     */
    private boolean isFallbackRequired(final VersionedActionEntry entry) {
        if (Objects.isNull(entry.apiVersion())) {
            return false;
        }
        final ApiVersion apiVersion = entry.apiVersion();

        if (apiVersion.handles().length > 0) {
            return true;
        }

        return !apiVersion.fallbackVersion().isEmpty();
    }

    /**
     * <p>
     * Determines if the available version is compatible with the requested version.
     * </p>
     *
     * @param available Available version string from the registry.
     * @param requested Requested version string from the header.
     * @return whether available version is compatible with the requested version.
     */
    private boolean isVersionCompatible(final String available, final String requested) {

        return Objects.equals(available, requested);
    }

    /**
     * <p>
     * Compares two version strings to determine their order.
     * </p>
     *
     * @param versionOne Version string of the first version (e.g. "1.0", "2.1.3")
     * @param versionTwo Version string of the second version (e.g. "1.0", "2.1.3").
     * @return a positive integer if versionOne is greater than versionTwo,
     * a negative integer if versionOne is less than versionTwo, or zero if they are equal.
     */
    private int compareVersions(final String versionOne, final String versionTwo) {
        final String[] partsOne = versionOne.split("\\.");
        final String[] partsTwo = versionTwo.split("\\.");
        final int maxLength = Math.max(partsOne.length, partsTwo.length);

        for (int i = NumericalConstant.ZERO; i < maxLength; i++) {
            final int numOne = i < partsOne.length ? parseVersionPart(partsOne[i]) : NumericalConstant.ZERO;
            final int numTwo = i < partsTwo.length ? parseVersionPart(partsTwo[i]) : NumericalConstant.ZERO;

            if (numOne != numTwo) {
                return numOne - numTwo;
            }
        }
        return NumericalConstant.ZERO;
    }

    /**
     * <p>
     * Parses a version part string to an integer, returning 0 for non-numeric parts.
     * </p>
     *
     * @param part The version part string (e.g. "1", "0", "alpha").
     * @return the integer value of the version part, or 0 if it is not a valid number.
     */
    private int parseVersionPart(final String part) {

        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException exception) {
            return NumericalConstant.ZERO;
        }
    }

    /**
     * <p>
     * Handles the case where the requested version is not found.
     * </p>
     *
     * @param exchange         The WebExchange to send the response on.
     * @param requestedVersion The version string that was requested.
     */
    private void handleVersionNotFound(final WebExchange exchange, final String requestedVersion) {
        LOGGER.error("API version '{}' not found for path '{}'. Available versions: {}",
                requestedVersion, path, versionedActions.keySet());

        exchange.responseSender().send(Map.of("Content-Type", "application/json"),
                String.format("{\"error\":\"API version '%s' is not supported\",\"availableVersions\":%s}", requestedVersion,
                        versionedActions.keySet()).getBytes(), com.tlc.web.HttpStatus.BAD_REQUEST);
    }

    /**
     * <p>
     * Handles deprecation warnings for the used API version.
     * </p>
     *
     * <p>
     * This method is called when a deprecated version is used and no successor
     * version is available or routing to successor failed.
     * </p>
     *
     * @param exchange The WebExchange to add headers to.
     * @param entry    The VersionedActionEntry that is deprecated.
     */
    private void handleDeprecation(final WebExchange exchange, final VersionedActionEntry entry) {
        final ApiVersion apiVersion = entry.apiVersion();
        final String deprecationMessage = apiVersion.deprecationMessage().isEmpty()
                ? "This API version is deprecated. Consider upgrading to a newer version."
                : apiVersion.deprecationMessage();

        LOGGER.warn("Deprecated API version '{}' used for path '{}'. {}",
                entry.version(), path, deprecationMessage);
        exchange.header("Deprecation", "true");
        exchange.header("Warning", String.format("299 - \"API version %s is deprecated. %s\"",
                entry.version(), deprecationMessage));

        if (!apiVersion.successor().isEmpty()) {
            exchange.header("Link", String.format("<%s>; rel=\"successor-version\"", apiVersion.successor()));
        }
    }

    /**
     * <p>
     * Handles deprecation with automatic redirection to successor version.
     * </p>
     *
     * @param exchange        The WebExchange to add headers to.
     * @param deprecatedEntry The deprecated VersionedActionEntry that was requested.
     * @param successorEntry  The successor VersionedActionEntry that will be used.
     */
    private void handleDeprecationWithRedirect(final WebExchange exchange,
                                               final VersionedActionEntry deprecatedEntry,
                                               final VersionedActionEntry successorEntry) {
        final ApiVersion apiVersion = deprecatedEntry.apiVersion();
        final String deprecationMessage = apiVersion.deprecationMessage().isEmpty() ?
                String.format("Automatically redirected to version %s", successorEntry.version()) : apiVersion.deprecationMessage();

        LOGGER.info("Deprecated API version '{}' used for path '{}'. Automatically routing to successor version '{}'.",
                deprecatedEntry.version(), path, successorEntry.version());
        exchange.header(HeaderAttributes.DEPRECATION, HeaderAttributes.TRUE);
        exchange.header(HeaderAttributes.WARNING, String.format("299 - \"API version %s is deprecated. Automatically using version %s. %s\"",
                deprecatedEntry.version(), successorEntry.version(), deprecationMessage));
        exchange.header(HeaderAttributes.REDIRECTED_FROM, deprecatedEntry.version());
        exchange.header(HeaderAttributes.REDIRECTED_TO, successorEntry.version());
        exchange.header(HeaderAttributes.LINK, String.format("<%s>; rel=\"successor-version\"", successorEntry.version()));
        exchange.setDeprecationContext(new DeprecationContext(deprecatedEntry.version(), deprecationMessage,
                successorEntry.version(), successorEntry.version()));
    }

    /**
     * <p>
     * Builder for creating VersionedAction instances.
     * </p>
     *
     * @author Ajay.
     * @version 1.0.
     */
    public static class Builder {

        private static final String DEFAULT_VERSION = "1";

        private final String path;
        private final Map<String, VersionedActionEntry> versionedActions;

        private String defaultVersion;

        public Builder(final String path) {
            this.path = path;
            versionedActions = new TreeMap<>();
            defaultVersion = DEFAULT_VERSION;
        }

        /**
         * <p>
         * Adds a versioned action.
         * </p>
         *
         * @param version      The version string.
         * @param action       The action implementation.
         * @param apiVersion   The ApiVersion annotation.
         */
        public void addVersion(final String version, final Action action,
                               final ApiVersion apiVersion) {
            versionedActions.put(version, new VersionedActionEntry(version, action, apiVersion));
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
            return versionedActions.containsKey(version);
        }

        /**
         * <p>
         * Sets the default version.
         * </p>
         *
         * @param defaultVersion The default version.
         */
        public void setDefaultVersion(final String defaultVersion) {

            if (Objects.nonNull(defaultVersion)) {
                this.defaultVersion = defaultVersion;
            }
        }

        /**
         * <p>
         * Builds the VersionedAction.
         * </p>
         *
         * @return The built VersionedAction.
         */
        public VersionedAction build() {

            if (versionedActions.isEmpty()) {
                throw ErrorCode.get(WebErrorCodes.VERSIONED_ACTION_NOT_FOUND, "At least one versioned action must be registered: Path: " + path);
            }
            return new VersionedAction(path, versionedActions, defaultVersion);
        }
    }

    /**
     * <p>
     * Internal record to hold a versioned action entry.
     * </p>
     *
     * @param version      The version string.
     * @param action       The action implementation.
     * @param apiVersion   The ApiVersion annotation.
     */
    public record VersionedActionEntry(String version, Action action, ApiVersion apiVersion) {
    }
}