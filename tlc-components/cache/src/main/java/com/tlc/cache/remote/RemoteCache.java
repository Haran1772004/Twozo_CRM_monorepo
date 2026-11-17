package com.tlc.cache.remote;

import com.tlc.cache.AsyncCache;
import com.tlc.cache.Cache;
import com.tlc.commons.listeners.PresenceHandler;

import java.util.concurrent.locks.Lock;

/**
 * Remote caching interface that extends {@link Cache} and {@link AsyncCache} to provide additional remote-specific operations,
 * including conditional updates, locking mechanisms, and presence checks.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 * @author Abishek
 * @version 1.0
 */
public interface RemoteCache<K, V> extends Cache<K, V>, AsyncCache<K, V> {
    /**
     * Associates the specified value with the specified key if the key is not already associated with a value.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return {@code true} if the key was not present and the value was set, {@code false} otherwise
     */
    boolean putIfAbsent(K key, V value);

    /**
     * Replaces the entry for the specified key only if currently mapped to the specified value.
     *
     * @param key      the key with which the specified value is associated
     * @param oldValue the value expected to be associated with the specified key
     * @param newValue the value to be associated with the specified key
     * @return {@code true} if the value was replaced, {@code false} otherwise
     */
    boolean replace(K key, V oldValue, V newValue);

    /**
     * Returns a lock associated with the specified key for synchronization purposes.
     *
     * @param key the key for which to obtain the lock
     * @return the lock for the specified key
     */
    Lock lock(K key);

    /**
     * Checks the presence of the specified key and invokes the appropriate handler.
     *
     * @param key             the key to check for presence
     * @param presenceHandler the handler to invoke based on presence
     */
    void ifPresent(K key, final PresenceHandler<K> presenceHandler);
}
