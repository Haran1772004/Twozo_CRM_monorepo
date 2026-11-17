package com.tlc.cache;

import com.tlc.commons.listeners.Callback;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Asynchronous cache interface providing non-blocking cache operations.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 * @author Karthick V
 * @version 1.0
 */
public interface AsyncCache<K, V> {

    /**
     * Asynchronously retrieves the value associated with the specified key.
     *
     * @param key      the key whose associated value is to be returned
     * @param callback the callback to handle the result or failure
     */
    void getAsync(final K key, final Callback<V> callback);

    /**
     * Asynchronously retrieves the values associated with the specified keys.
     *
     * @param keys     the set of keys whose associated values are to be returned
     * @param callback the callback to handle the result or failure
     */
    void getAllAsync(final Set<K> keys, final Callback<Map<K, V>> callback);

    /**
     * Asynchronously associates the specified value with the specified key.
     *
     * @param key      the key with which the specified value is to be associated
     * @param value    the value to be associated with the specified key
     * @param callback the callback to handle the result or failure
     */
    void putAsync(final K key, final V value, final Callback<Map.Entry<K, V>> callback);

    /**
     * Asynchronously associates the specified values with the specified keys.
     *
     * @param map      the map of keys and values to be associated
     * @param callback the callback to handle the result or failure
     */
    void putAllAsync(final Map<K, V> map, final Callback<Map<K, V>> callback);

    /**
     * Asynchronously associates the specified value with the specified key if the key is not already associated with a value.
     *
     * @param key      the key with which the specified value is to be associated
     * @param value    the value to be associated with the specified key
     * @param callback the callback to handle the result or failure
     */
    void putIfAbsentAsync(final K key, final V value, final Callback<Map.Entry<K, V>> callback);

    /**
     * Asynchronously removes the mapping for the specified key.
     *
     * @param key      the key whose mapping is to be removed
     * @param callback the callback to handle the result or failure
     */
    void removeAsync(final K key, final Callback<V> callback);

    /**
     * Asynchronously removes the mappings for the specified keys.
     *
     * @param keys     the set of keys whose mappings are to be removed
     * @param callback the callback to handle the result or failure
     */
    void removeAllAsync(final Set<K> keys, final Callback<V> callback);

    /**
     * Asynchronously computes and associates a value with the specified key if the key is not already associated with a value.
     *
     * @param key             the key
     * @param mappingFunction the function to compute the value
     * @param callback        the callback to handle the result or failure
     */
    void computeIfAbsentAsync(final K key, final Function<? super K, ? extends V> mappingFunction, final Callback<Map.Entry<K, V>> callback);

    /**
     * Asynchronously replaces the entry for the specified key only if currently mapped to the specified value.
     *
     * @param key      the key with which the specified value is associated
     * @param oldValue the value expected to be associated with the specified key
     * @param newValue the value to be associated with the specified key
     * @param callback the callback to handle the result or failure
     * @return {@code true} if the value was replaced, {@code false} otherwise
     */
    boolean replaceAsync(final K key, final V oldValue, final V newValue, final Callback<Map.Entry<K, V>> callback);

    /**
     * Asynchronously iterates over all entries in the cache.
     *
     * @param callback the callback to handle the result or failure
     */
    void forEachAsync(final Callback<Map<K, V>> callback);
}

