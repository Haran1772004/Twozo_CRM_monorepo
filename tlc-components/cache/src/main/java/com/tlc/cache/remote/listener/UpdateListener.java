package com.tlc.cache.remote.listener;

/**
 * Provides an abstract way to deal with {@link EntryListener} actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public interface UpdateListener<K, V> extends EntryListener<K, V> {

    /**
     * Provides the services of listener when update event occurs
     *
     * @param key      Represents the reference of key
     * @param newValue Represents the reference of new value
     * @param oldValue Represents the reference of old value
     */
    void onUpdate(final K key, final V newValue, final V oldValue);
}
