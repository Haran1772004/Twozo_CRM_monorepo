package com.tlc.crm.core.features.internal.dataimport.async.registry.impl;

import com.tlc.cache.Cache;
import com.tlc.cache.CacheManager;
import com.tlc.crm.core.features.internal.dataimport.async.registry.ReadTaskRegistry;
import com.tlc.crm.core.features.internal.dataimport.async.task.ImportableFileReadTask;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A registry is responsible for storing the submitted tasks in the {@link Cache} specifically for reading the uploaded files for the import process
 *
 * @author sundar
 * @version 1.0
 * @see ReadTaskRegistry
 */
public class ReadTaskRegistryImpl implements ReadTaskRegistry {

    private final Cache<Long, ImportableFileReadTask> importableFileReadTaskCache;
    private final ReentrantReadWriteLock lock;

    private static class Instance {
        private static final ReadTaskRegistry INSTANCE = new ReadTaskRegistryImpl();
    }

    private ReadTaskRegistryImpl() {
        this.importableFileReadTaskCache = CacheManager.getInstance().createCache();
        this.lock = new ReentrantReadWriteLock();
    }

    /**
     * Gets the {@link ReadTaskRegistryImpl} instance
     *
     * @return {@link ReadTaskRegistryImpl}
     */
    public static ReadTaskRegistry getInstance() {
        return ReadTaskRegistryImpl.Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param importId Represents the import ID
     * @return {@link ImportableFileReadTask}
     */
    public ImportableFileReadTask get(final long importId) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();

        readLock.lock();

        try {
            return importableFileReadTaskCache.get(importId);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param importId               Represents the import ID
     * @param importableFileReadTask Which contains import information and record holder
     * @return true if the task is successfully added to the repository otherwise false
     */
    @Override
    public boolean add(final long importId, final ImportableFileReadTask importableFileReadTask) {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {

            if (importableFileReadTaskCache.containsKey(importId)) {
                return false;
            } else {
                importableFileReadTaskCache.put(importId, Objects.requireNonNull(importableFileReadTask));
                return true;
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param requestId Represents the async read request ID
     */
    @Override
    public void remove(final long requestId) {
        importableFileReadTaskCache.remove(requestId);
    }

    /**
     * Clears all ongoing or pending file read tasks within the {@link Cache} that are ready for execution or currently executing
     */
    public void clear() {
        importableFileReadTaskCache.clear();
    }
}