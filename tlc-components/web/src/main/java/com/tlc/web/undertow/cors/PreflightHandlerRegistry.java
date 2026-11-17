package com.tlc.web.undertow.cors;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry for managing and resolving {@link PreflightHandler} instances.
 * <p>
 * Maintains a collection of handlers responsible for processing
 * CORS preflight requests.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class PreflightHandlerRegistry {

    private final Map<String, PreflightHandler> preflightVerifier;

    private PreflightHandlerRegistry() {
        this.preflightVerifier = new HashMap<>();
    }

    private static final class Instance {
        private static final PreflightHandlerRegistry PREFLIGHT_HANDLER_REGISTRY = new PreflightHandlerRegistry();
    }

    /**
     * Gets the instance of {@code PreflightHandlerRegistry}.
     *
     * @return The instance of {@code PreflightHandlerRegistry}.
     */
    public static PreflightHandlerRegistry getInstance() {
        return Instance.PREFLIGHT_HANDLER_REGISTRY;
    }

    /**
     * Registers a {@link PreflightHandler} for the specified application.
     *
     * @param applicationName  The name of the application.
     * @param preflightHandler The handler responsible for managing preflight checks for the given application.
     */
    public void register(final String applicationName, final PreflightHandler preflightHandler) {
        this.preflightVerifier.put(applicationName.toLowerCase(), preflightHandler);
    }

    /**
     * Retrieves the Optional {@link PreflightHandler} registered for the specified application.
     *
     * @param applicationName The name of the application.
     * @return The corresponding {@code PreflightHandler}, or {@code Optional.empty()} if none is registered.
     */
    public Optional<PreflightHandler> get(final String applicationName) {
        return Optional.ofNullable(preflightVerifier.get(applicationName.toLowerCase()));
    }
}
