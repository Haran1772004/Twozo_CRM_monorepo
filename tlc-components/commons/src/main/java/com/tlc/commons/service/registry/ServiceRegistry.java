package com.tlc.commons.service.registry;

import com.tlc.commons.service.Service;

/**
 * <p>
 * Interface representing a registry for services.
 * </p>
 *
 * @param <T> The type of service to be registered.
 * @author Sathish
 * @version 1.0
 */
public interface ServiceRegistry<T extends Service> {

    /**
     * <p>
     * Registers a service with the registry.
     * </p>
     *
     * @param service The service to register.
     */
    void register(final T service);

    /**
     * <p>
     * Retrieves the registered service instance.
     * </p>
     *
     * @return The service instance.
     */
    T get();

    /**
     * <p>
     * Unregisters a service from the registry.
     * </p>
     *
     * @return The unregistered service.
     */
    T unregister();
}
