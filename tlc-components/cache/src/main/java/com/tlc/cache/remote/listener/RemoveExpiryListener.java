package com.tlc.cache.remote.listener;

/**
 * Provides an abstract way to deal with {@link RemoveListener}, {@link ExpiryListener} actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public interface RemoveExpiryListener<K, V> extends RemoveListener<K, V>, ExpiryListener<K, V> {

}
