package com.tlc.analytics.general.multitenant;

import com.tlc.commons.util.executor.NullSafeExecutor;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of the {@link MultiTenantContainer} interface.
 *
 * @param <ID> The type of the ID used to identify instances.
 * @param <K>  The type of the key used to retrieve instances.
 * @param <R>  The type of the instances.
 *
 * @author Aswath
 * @version 1.0
 */
@RequiredArgsConstructor( staticName = "create", access = AccessLevel.PACKAGE )
class MultiTenantContainerImpl<ID, K, R> implements MultiTenantContainer<ID, K, R>
{
    private final Function<K, ID> idGetter;
    private final Supplier<ID> defaultIdGetter;
    private final Function<ID, R> instanceCreator;
    private final Map<ID, R> idInstanceMap = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * @param key The key used to retrieve or create the instance.
     * @return The instance associated with the key.
     */
    @Override
    public R getOrCreateInstance( final @NonNull K key )
    {
        return idInstanceMap.computeIfAbsent( getId( key ), instanceCreator );
    }

    /**
     * Retrieves the ID associated with the given key.
     *
     * @param key The key for which to retrieve the ID.
     * @return The ID associated with the key.
     */
    private ID getId( final K key )
    {
        ID id = idGetter.apply( key );
        return NullSafeExecutor.orElseGet( id, defaultIdGetter );
    }

    /**
     * {@inheritDoc}
     *
     * @return A collection containing all instances.
     */
    @Override
    public @NonNull Collection<R> getAll()
    {
        return Collections.unmodifiableCollection( idInstanceMap.values() );
    }

    /**
     * {@inheritDoc}
     *
     * @param consumer The action to perform for each entry.
     */
    @Override
    public void forEach( final @NonNull BiConsumer<ID, R> consumer )
    {
        idInstanceMap.forEach( consumer );
    }

    /**
     * {@inheritDoc}
     *
     * @param consumer The criteria for removing instances.
     */
    @Override
    public void removeAll( final @NonNull Consumer<R> consumer )
    {
        idInstanceMap.values()
                .removeIf( instance -> {
                    consumer.accept( instance );
                    return true;
                } );
    }

    /**
     * {@inheritDoc}
     *
     * @param id The ID of the instance to remove.
     * @return The removed instance, or null if no instance was found for the given ID.
     */
    @Override
    public R remove( final @NonNull ID id )
    {
        return idInstanceMap.remove( id );
    }
}
