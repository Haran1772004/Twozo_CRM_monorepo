package com.tlc.cache.remote.listener;

/**
 * Provides an abstract way to deal with {@link CreateListener}, {@link UpdateListener} actions
 *
 * @author kowsalya SP
 * @version 1.0
 */
public interface CreateUpdateListener<K, V> extends CreateListener<K, V>, UpdateListener<K, V> {

}
