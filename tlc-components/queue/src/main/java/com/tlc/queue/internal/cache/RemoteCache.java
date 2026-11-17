package com.tlc.queue.internal.cache;

/**
 * A remote cache that stores frequently accessed data on the remote cache server
 *
 * @param <K> Represents the key
 * @param <V> Represents the value
 */
public interface RemoteCache<K, V> {

    /**
     * Gets the value associated with the given key
     *
     * @param key Represents the key
     * @return the value associated with the given key
     */
    V get(K key);
}