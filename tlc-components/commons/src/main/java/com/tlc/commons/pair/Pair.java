package com.tlc.commons.pair;

import java.io.Serializable;
import java.util.Map;

/**
 * <p>
 * Holds the pair of values.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class Pair<K, V> implements Map.Entry<K, V>, Serializable {

    private final K key;
    private V value;

    private Pair(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(final V value) {
        return this.value = value;
    }

    public static <K, V> Pair<K, V> of(final K key, final V value) {
        return new Pair<>(key, value);
    }
}
