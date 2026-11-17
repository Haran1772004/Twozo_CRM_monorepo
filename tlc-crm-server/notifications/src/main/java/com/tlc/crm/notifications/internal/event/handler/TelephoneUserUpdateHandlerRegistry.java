package com.tlc.crm.notifications.internal.event.handler;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Registry that holds all registered {@link TelephoneUserUpdateHandler} implementations.
 * Handlers are loaded during application startup and accessed dynamically at runtime.
 *
 * @author Sathish
 * @version 1.0
 */
public class TelephoneUserUpdateHandlerRegistry {

    private final Collection<TelephoneUserUpdateHandler> handlers;

    private TelephoneUserUpdateHandlerRegistry() {
        handlers = new CopyOnWriteArraySet<>();
    }

    private static class Instance {
        private static final TelephoneUserUpdateHandlerRegistry REGISTRY = new TelephoneUserUpdateHandlerRegistry();
    }

    /**
     * Returns the singleton instance of {@link TelephoneUserUpdateHandlerRegistry}.
     *
     * @return the singleton instance
     */
    public static TelephoneUserUpdateHandlerRegistry getInstance() {
        return Instance.REGISTRY;
    }

    /**
     * Initializes the registry during application startup.
     * Should be called once during bootstrap/initialization phase.
     */
    public void add(final TelephoneUserUpdateHandler handler) {
        handlers.add(handler);
    }

    /**
     * Returns the list of registered telephony user update handlers.
     *
     * @return an unmodifiable list of {@link TelephoneUserUpdateHandler} implementations
     */
    public Collection<TelephoneUserUpdateHandler> getHandlers() {
        return handlers;
    }
}

