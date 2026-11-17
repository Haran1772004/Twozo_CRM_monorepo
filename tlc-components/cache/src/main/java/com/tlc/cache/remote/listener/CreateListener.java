package com.tlc.cache.remote.listener;

/**
 * Provides an abstract way to deal with {@link EntryListener} actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public interface CreateListener<K, V> extends EntryListener<K, V> {

    /**
     * Provides the services of listener when create event occurs
     *
     * @param key   Represents the reference of key
     * @param value Represents the reference of value
     */
    void onCreate(final K key, final V value);
}
