package com.tlc.cache.internal.impl;

import com.tlc.cache.internal.RedissonEntryListener;
import org.redisson.api.map.event.EntryCreatedListener;
import org.redisson.api.map.event.EntryEvent;

/**
 * Notifies when a map event occurs that listens the {@link EntryCreatedListener} actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public abstract class CreateListenerImpl<K, V> implements EntryCreatedListener<K, V>, RedissonEntryListener {

    /**
     * Invoked upon creation of an entry
     *
     * @param event Holds the attribute of entry event
     */
    @Override
    public final void onCreated(final EntryEvent<K, V> event) {
        final EntryEventListener<K, V> eventListener = new EntryEventListener<>(event);

        onCreate(eventListener.getKey(), eventListener.getNewValue());
    }

    /**
     * Provides the services of cache event listener when create occur
     *
     * @param key   Represents the reference of key
     * @param value Represents the reference of value
     */
    public abstract void onCreate(final K key, final V value);
}
