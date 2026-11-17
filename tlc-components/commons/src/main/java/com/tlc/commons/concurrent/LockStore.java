package com.tlc.commons.concurrent;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A storage facility that oversees locks to implement segment-level locking. <br>
 * <b>Note: <span style="color:red;">Expanding the size of locks might lead to performance concerns.</span></b>
 *
 * @param <T> Denotes the type of object for which the lock is acquired.
 * @author sundar
 * @version 1.0
 */
public final class LockStore<T> {

    private static final byte DEFAULT_LOCK_SIZE = 2;

    private final int size;
    private final Map<Integer, ReentrantReadWriteLock> locks;

    public LockStore(final int size) {
        this.size = size;
        this.locks = new ConcurrentHashMap<>(size);
    }

    public LockStore() {
        this(DEFAULT_LOCK_SIZE);
    }

    /**
     * Gets the corresponding lock for the provided object
     *
     * @param object The object for which the lock is retrieved
     * @return The {@link ReentrantReadWriteLock} associated with the object
     */
    public ReentrantReadWriteLock getLock(final T object) {
        return locks.computeIfAbsent(Objects.requireNonNull(object).hashCode() % size, position -> new ReentrantReadWriteLock());
    }
}
