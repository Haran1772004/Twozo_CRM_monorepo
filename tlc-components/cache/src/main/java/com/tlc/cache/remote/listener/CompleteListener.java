package com.tlc.cache.remote.listener;

/**
 * Provides an abstract way to deal with {@link CreateListener},
 * {@link UpdateListener}, {@link RemoveListener} or {@link ExpiryListener} actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public interface CompleteListener<K, V> extends CreateListener<K, V>, UpdateListener<K, V>, RemoveListener<K, V>, ExpiryListener<K, V> {

}
