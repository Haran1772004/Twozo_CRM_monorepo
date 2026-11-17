package com.tlc.web.versioning;

import com.tlc.web.Action;
import com.tlc.web.WebExchange;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * <p>
 * Context object stored in WebExchange for sparse/partial version routing.
 * </p>
 *
 * <p>
 * When a versioned action only handles specific operation types (via {@link ApiVersion#handles()}),
 * this context is attached to the exchange so the action implementation can delegate
 * unhandled types to the fallback action.
 * </p>
 *
 * @version 1.0
 * @author Ajay.
 */
public final class VersionFallbackContext {

    private final VersionedAction.VersionedActionEntry fallbackEntry;
    private final Set<String> handledTypes;

    public VersionFallbackContext(final VersionedAction.VersionedActionEntry fallbackEntry, final Set<String> handledTypes) {
        this.fallbackEntry = Objects.requireNonNull(fallbackEntry);
        this.handledTypes = Objects.requireNonNull(handledTypes);
    }

    /**
     * <p>
     * Checks if the given type is handled by the current resolved version.
     * </p>
     *
     * <ul>
     *     <li>If handledTypes is empty, it means unconditional fallback (fallbackVersion was explicitly set
     *         without handles), so all requests should fall back.</li>
     *     <li>If handledTypes is non-empty and type is null, request is considered handled (no fallback).</li>
     *     <li>If handledTypes is non-empty, only listed types are handled by the current version.</li>
     * </ul>
     *
     * @param type The request type.
     * @return true if the type is handled by the current version (no fallback needed).
     */
    public boolean isTypeHandled(final String type) {
        if (handledTypes.isEmpty()) {
            return false;
        }

        return handledTypes.contains(type);
    }

    /**
     * <p>
     * Gets the fallback action for unhandled types.
     * </p>
     *
     * @return The fallback Action.
     */
    public Action getFallbackAction() {
        return fallbackEntry.action();
    }

    /**
     * <p>
     * Extracts the VersionFallbackContext from a WebExchange.
     * </p>
     *
     * @param exchange The web exchange.
     * @return Optional containing the context if present.
     */
    public static Optional<VersionFallbackContext> from(final WebExchange exchange) {
        return exchange.getVersionFallbackContext();
    }
}

