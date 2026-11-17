package com.tlc.web.versioning;

import java.util.Objects;

/**
 * <p>
 * Immutable value object carrying all deprecation metadata for a versioned request.
 * </p>
 *
 * @param deprecatedVersion   The version that was requested but is deprecated.
 * @param message             Human-readable deprecation message.
 * @param successorVersion    The successor version.
 * @param redirectedToVersion The version the request was automatically routed to
 *                            (non-null only when an automatic redirect occurred).
 *
 * @author Ajay.
 * @version 1.0.
 */
public record DeprecationContext(String deprecatedVersion, String message, String successorVersion, String redirectedToVersion) {

    /**
     * Returns {@code true} when the request was automatically redirected to a
     * successor version rather than being served by the deprecated version itself.
     *
     * @return whether an automatic redirect took place.
     */
    public boolean isRedirected() {
        return Objects.nonNull(redirectedToVersion);
    }
}