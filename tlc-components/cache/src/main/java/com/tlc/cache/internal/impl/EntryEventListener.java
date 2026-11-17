package com.tlc.cache.internal.impl;

import org.redisson.api.map.event.EntryEvent;

/**
 * Manages the {@link EntryEvent} related actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public record EntryEventListener<K, V>(EntryEvent<K, V> event) {

    /**
     * Returns the key of the entry event
     *
     * @return Represents the type of key
     */
    public K getKey() {
        return event.getKey();
    }

    /**
     * Returns the old value of the entry event
     *
     * @return Represents the type of old value
     */
    public V getOldValue() {
        return event.getOldValue();
    }

    /**
     * Returns the new value of the entry event
     *
     * @return Represents the type of new value
     */
    public V getNewValue() {
        return event.getValue();
    }
}
