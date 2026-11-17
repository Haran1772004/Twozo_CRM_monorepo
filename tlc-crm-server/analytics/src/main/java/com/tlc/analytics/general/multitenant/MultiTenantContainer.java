package com.tlc.analytics.general.multitenant;

import lombok.NonNull;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Interface for a multi-tenant container that manages instances based on keys and IDs.
 *
 * @param <ID> The type of the ID used to identify instances.
 * @param <K>  The type of the key used to retrieve instances.
 * @param <R>  The type of the instances.
 *
 * @author Aswath
 * @version 1.0
 */
public interface MultiTenantContainer<ID, K, R>
{
    /**
     * Retrieves or creates an instance associated with the given key.
     *
     * @param key The key used to retrieve or create the instance.
     * @return The instance associated with the key.
     */
    R getOrCreateInstance( final @NonNull K key );

    /**
     * Retrieves all instances in the container.
     *
     * @return A collection containing all instances.
     */
    @NonNull
    Collection<R> getAll();

    /**
     * Performs an action for each entry in the container.
     *
     * @param consumer The action to perform for each entry.
     */
    void forEach( final @NonNull BiConsumer<ID, R> consumer );

    /**
     * Removes all instances from the container that match the given criteria.
     *
     * @param consumer The criteria for removing instances.
     */
    void removeAll( final @NonNull Consumer<R> consumer );

    /**
     * Removes the instance associated with the given ID from the container.
     *
     * @param id The ID of the instance to remove.
     * @return The removed instance, or null if no instance was found for the given ID.
     */
    R remove( final @NonNull ID id );

    /**
     * Creates an instance of {@link MultiTenantContainer} with the specified functions.
     *
     * @param idGetter        The function to retrieve the ID from the key.
     * @param defaultIdGetter The supplier to provide a default ID.
     * @param instanceCreator The function to create instances based on IDs.
     * @param <ID>            The type of the ID.
     * @param <K>             The type of the key.
     * @param <R>             The type of the instances.
     * @return A new instance of MultiTenantContainer.
     */
    static <ID, K, R> MultiTenantContainer<ID, K, R> createInstance( final @NonNull Function<K, ID> idGetter, final @NonNull Supplier<ID> defaultIdGetter,
                                                                     final @NonNull Function<ID, R> instanceCreator )
    {
        return MultiTenantContainerImpl.create( idGetter, defaultIdGetter, instanceCreator );
    }
}
