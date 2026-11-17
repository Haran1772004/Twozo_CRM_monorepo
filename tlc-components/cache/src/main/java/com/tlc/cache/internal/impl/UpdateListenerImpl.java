package com.tlc.cache.internal.impl;

import com.tlc.cache.internal.RedissonEntryListener;
import org.redisson.api.map.event.EntryEvent;
import org.redisson.api.map.event.EntryUpdatedListener;

/**
 * Notifies when an event occurs that listens the {@link EntryUpdatedListener} actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public abstract class UpdateListenerImpl<K, V> implements EntryUpdatedListener<K, V>, RedissonEntryListener {

    /**
     * Invoked upon update of an entry
     *
     * @param event Holds the attribute of entry event
     */
    @Override
    public final void onUpdated(final EntryEvent<K, V> event) {
        final EntryEventListener<K, V> eventListener = new EntryEventListener<>(event);

        onUpdate(eventListener.getKey(), eventListener.getNewValue(), eventListener.getOldValue());
    }

    /**
     * Provides the services of listener when update event occurs
     *
     * @param key      Represents the reference of key
     * @param newValue Represents the reference of new value
     * @param oldValue Represents the reference of old value
     */
    public abstract void onUpdate(final K key, final V newValue, final V oldValue);

}
