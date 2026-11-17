package com.tlc.queue.internal.cache;

import java.util.Collection;

/**
 * Stores the required data in heap memory for caching.
 *
 * @param <K> Represents the key
 * @param <V> Represents the value
 * @author sundar
 * @version 1.0
 */
public interface LocalCache<K, V> {

    /**
     * Loads the value into the cache
     *
     * @param value the value to be loaded
     */
    void load(final V value);

    /**
     * Stores the values into the cache
     *
     * @param values Which contains the values to be stored
     */
    void load(final Collection<V> values);

    /**
     * Gets the value mapped with the given key
     *
     * @param key Represents the key
     * @return the value mapped with the given key
     */
    V get(final K key);

    /**
     * Clears the cache
     */
    void clear();
}