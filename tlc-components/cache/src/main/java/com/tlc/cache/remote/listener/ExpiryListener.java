package com.tlc.cache.remote.listener;

/**
 * Provides an abstract way to deal with {@link EntryListener} actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public interface ExpiryListener<K, V> extends EntryListener<K, V> {

    /**
     * Provides the services of listener when expiry event occurs
     *
     * @param key   Represents the reference of key
     * @param value Represents the reference of value
     */
    void onExpiry(final K key, final V value);
}
