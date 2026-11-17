package com.tlc.commons.executor.context.registry;

import com.tlc.commons.executor.context.Context;

import java.util.Map;
import java.util.Objects;

/**
 * Registry for manage and holds services related to the context
 *
 * @param <T> the type of the context service
 * @author sundar
 * @version 1.0
 */
abstract class Registry<T> {

    /**
     * Registers the service with its associated {@link Context.Type}
     *
     * @param contextType Represents the context type
     * @param service     the service to register
     */
    public void register(final Context.Type contextType, final T service) {
        getServiceMap().putIfAbsent(contextType, service);
    }

    /**
     * Gets the service associated with the given context type
     *
     * @param contextType Represents the context type
     * @return the service associated with the given context type
     */
    public T get(final Context.Type contextType) {
        return getServiceMap().get(contextType);
    }

    /**
     * Gets the service associated with the given context type name
     *
     * @param contextTypeName Represents the context type name
     * @return the service associated with the given context type name
     */
    public T get(final String contextTypeName) {

        for (final Map.Entry<Context.Type, T> contextCodecEntry : getServiceMap().entrySet()) {

            if (Objects.equals(contextCodecEntry.getKey().getName(), contextTypeName)) {
                return contextCodecEntry.getValue();
            }
        }
        return null;
    }

    /**
     * Checks whether the service is available for the given context type name
     *
     * @param contextTypeName Represents the context type name
     * @return true if the service is available for the given context type name otherwise false
     */
    public boolean contains(final String contextTypeName) {

        for (final Context.Type contextType : getServiceMap().keySet()) {

            if (Objects.equals(contextType.getName(), contextTypeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the registry is empty or not
     *
     * @return true if the registry is empty; false otherwise
     */
    public boolean isEmpty() {
        return getServiceMap().isEmpty();
    }

    /**
     * Unregisters the service associated with the given context type
     *
     * @param contextType Represents the context type
     */
    public void unregister(final Context.Type contextType) {
        getServiceMap().remove(contextType);
    }

    /**
     * Gets the service map that contains the services associated with their respective context types
     *
     * @return {@link Map} containing the services associated with their respective context types
     */
    abstract Map<Context.Type, T> getServiceMap();
}