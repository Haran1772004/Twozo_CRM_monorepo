package com.tlc.analytics.config.entity.cache;

import lombok.NonNull;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * <p>
 * It provides methods to store, retrieve, and remove cached items, as well as batch operations and utilities
 * for handling cache contents
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
public interface CacheAdapter<K, V>
{
    /**
     * Puts a single key-value pair into the cache
     *
     * @param key   The key with which the specified value is to be associated
     * @param value The value to be cached for the specified key
     */
    void put( final @NonNull K key,
              final V value );

    /**
     * Inserts all key-value pairs from the provided map into the cache
     *
     * @param pairs A map containing the key-value pairs to be inserted into the cache
     */
    void putAll( final @NonNull Map<K, V> pairs );

    /**
     * Removes the cache entry associated with the specified key
     *
     * @param key The key whose mapping is to be removed from the cache
     */
    void remove( final @NonNull K key );

    /**
     * Removes all cache entries associated with the given set of keys
     *
     * @param keys The set of keys whose mappings are to be removed from the cache
     */
    void removeAll( final @NonNull Set<K> keys );

    /**
     * Checks if the cache contains a mapping for the specified key
     *
     * @param key The key whose presence in the cache is to be tested
     * @return True if the cache contains a mapping for the key, false otherwise
     */
    boolean containsKey( final @NonNull K key );

    /**
     * Retrieves the value associated with the specified key from the cache
     *
     * @param key The key whose associated value is to be returned
     * @return The value associated with the key, or null if no mapping exists
     */
    @Nullable
    V get( final @NonNull K key );

    /**
     * Retrieves a map of values associated with the specified set of keys from the cache
     *
     * @param keys The set of keys whose associated values are to be retrieved
     * @return A map containing the keys and their associated values from the cache
     */
    @NonNull
    Map<Long, V> getAll( final @NonNull Set<K> keys );

    /**
     * Performs the given action for each entry in the cache
     *
     * @param consumer A BiConsumer that accepts each key-value pair in the cache
     */
    void forEach( final @NonNull BiConsumer<K, V> consumer );
}
