package com.tlc.web.client.event;

import com.tlc.web.client.internal.event.EventManagerImpl;

import java.util.Map;

/**
 * A registrar that registers the event consumers for the given events
 *
 * @author Selvakumar
 * @version 1.0
 */
public interface EventConsumerRegistrar {

    /**
     * Fetches the event consumer registrar
     *
     * @return {@link EventConsumerRegistrar}
     */
    static EventConsumerRegistrar get() {
        return EventManagerImpl.getInstance();
    }

    /**
     * Registers the given event consumers under the given group
     *
     * @param group     Consumer group
     * @param consumers Consumers
     */
    void register(String group, Map<EventListener, EventConsumer> consumers);

    /**
     * Unregisters the event consumers that are registered under the given group
     *
     * @param group Consumer group
     * @param <T>
     */
    <T> void unregister(String group);

    /**
     * Unregisters all event consumers across all groups.
     */
    default void unregisterAll() {
    }

    /**
     * Drops local consumer bookkeeping without waiting on the Event Bus.
     * Used when Vert.x is already leaving/closed and unregister propagation is unreliable.
     */
    default void discardAllConsumers() {
    }
}
