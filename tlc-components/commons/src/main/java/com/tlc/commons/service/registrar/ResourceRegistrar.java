package com.tlc.commons.service.registrar;

import com.tlc.commons.service.registrar.model.RegistrarData;

/**
 * <p>
 * The registrar that registers the given resources/files.
 * </p>
 *
 * @param <T> Holds the {@link RegistrarData} which is needed for registering the resources/files.
 * @author Sathish
 * @version 1.0
 */
public interface ResourceRegistrar<T extends RegistrarData> {

    /**
     * <p>
     * Registers the given resources/files.
     * </p>
     *
     * @param registrarData Holds the {@link T} which is needed for registering the resources/actions.
     */
    void register(final T registrarData);

    /**
     * <p>
     * Removes the registered resources/files.
     * </p>
     *
     * @param registrarData Holds the {@link T} which is needed for removing the resources/actions.
     */
    void unregister(final T registrarData);

    /**
     * <p>
     * Removes all previously registered resources/files.
     * Called during application shutdown to release all held references.
     * </p>
     */
    default void unregisterAll() {
    }
}
