package com.tlc.cache.internal.impl;

import com.tlc.cache.internal.RedissonEntryListener;
import org.redisson.api.map.event.EntryEvent;
import org.redisson.api.map.event.EntryExpiredListener;

/**
 * Notifies when an event occurs that listens the {@link EntryExpiredListener} actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public abstract class ExpiryListenerImpl<K, V> implements EntryExpiredListener<K, V>, RedissonEntryListener {

    /**
     * Invoked upon expiry of an entry
     *
     * @param event Holds the attribute of entry event
     */
    @Override
    public final void onExpired(final EntryEvent<K, V> event) {
        final EntryEventListener<K, V> eventListener = new EntryEventListener<>(event);

        onExpiry(eventListener.getKey(), eventListener.getNewValue());
    }

    /**
     * Provides the services of listener when expiry event occurs
     *
     * @param key   Represents the reference of key
     * @param value Represents the reference of value
     */
    public abstract void onExpiry(final K key, final V value);
}
