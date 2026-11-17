package com.tlc.cache.remote;

import com.tlc.cache.internal.RedissonEntryListener;
import com.tlc.cache.internal.impl.CreateListenerImpl;
import com.tlc.cache.internal.impl.ExpiryListenerImpl;
import com.tlc.cache.internal.impl.RemoveListenerImpl;
import com.tlc.cache.internal.impl.UpdateListenerImpl;
import com.tlc.cache.remote.listener.CreateListener;
import com.tlc.cache.remote.listener.ExpiryListener;
import com.tlc.cache.remote.listener.RemoveListener;
import com.tlc.cache.remote.listener.UpdateListener;
import com.tlc.cache.remote.listener.EntryListener;
import com.tlc.commons.executor.Executor;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * @author Abishek
 * @version 1.0
 */
public class CacheConfig<K, V>
{
	private final String name;
	private int ttl = -1;
	private int size = -1;
    private Executor asyncExecutor;
    private Collection<RedissonEntryListener> listeners;

    public CacheConfig(final String name)
	{
		this.name = Objects.requireNonNull(name);
	}

	public String getName()
	{
		return name;
	}

	public int getTtl()
	{
		return ttl;
	}

	public void setTtl(int ttl)
	{
		this.ttl = ttl;
	}

	public int getSize()
	{
		return size;
	}

	public void setSize(int size)
	{
		this.size = size;
	}

    public Executor getTaskExecutor() {
        return asyncExecutor;
    }

    public void setTaskExecutor(final Executor taskExecutor) {
        this.asyncExecutor = taskExecutor;
    }

    public Collection<RedissonEntryListener> getListeners() {
        return listeners;
    }

    public void addListener(final EntryListener<K, V> entryListener) {
        this.listeners = Objects.isNull(this.listeners) ? new LinkedHashSet<>() : listeners;

        registerEntryListeners(entryListener);
    }

    /**
     * Registers the collection of entry listener
     *
     * @param listener Represents the entry listener
     */
    private void registerEntryListeners(final EntryListener<K, V> listener) {

        if (listener instanceof CreateListener) {
            listeners.add(new CreateListenerImpl<K, V>() {
                @Override
                public void onCreate(final K key, final V value) {
                    ((CreateListener<K, V>) listener).onCreate(key, value);
                }
            });
        }

        if (listener instanceof UpdateListener) {
            listeners.add(new UpdateListenerImpl<K, V>() {

                @Override
                public void onUpdate(final K key, final V newValue, final V oldValue) {
                    ((UpdateListener<K, V>) listener).onUpdate(key, newValue, oldValue);
                }
            });
        }

        if (listener instanceof RemoveListener) {
            listeners.add(new RemoveListenerImpl<K, V>() {

                @Override
                public void onRemove(final K key, final V value) {
                    ((RemoveListener<K, V>) listener).onRemove(key, value);
                }
            });
        }

        if (listener instanceof ExpiryListener) {
            listeners.add(new ExpiryListenerImpl<K, V>() {

                @Override
                public void onExpiry(final K key, final V value) {
                    ((ExpiryListener<K, V>) listener).onExpiry(key, value);
                }
            });
        }
    }
}
