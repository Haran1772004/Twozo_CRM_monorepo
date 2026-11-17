package com.tlc.cache.internal.impl;

import com.tlc.cache.internal.RedissonEntryListener;
import org.redisson.api.map.event.EntryEvent;
import org.redisson.api.map.event.EntryRemovedListener;

/**
 * Notifies when an event occurs that listens the {@link EntryRemovedListener} actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public abstract class RemoveListenerImpl<K, V> implements EntryRemovedListener<K, V>, RedissonEntryListener {

    /**
     * Invoked upon removal of an entry
     *
     * @param event Holds the attribute of entry event
     */
    @Override
    public final void onRemoved(final EntryEvent<K, V> event) {
        final EntryEventListener<K, V> eventListener = new EntryEventListener<>(event);

        onRemove(eventListener.getKey(), eventListener.getNewValue());
    }

    /**
     * Provides the services of listener when remove event occurs
     *
     * @param key   Represents the reference of key
     * @param value Represents the reference of value
     */
    public abstract void onRemove(final K key, final V value);
}