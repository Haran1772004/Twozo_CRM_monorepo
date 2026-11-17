package com.tlc.notification.internal.calendar.resolver;

import com.tlc.commons.concurrent.LockStore;
import com.tlc.notification.internal.calendar.token.CalendarTokenCache;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Provides the services to resolve the tokens used for calendar synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
class CalendarTokenResolverImpl implements CalendarTokenResolver {

    private static final byte STRIPES_COUNT = 10;

    private final CalendarTokenCache calendarTokenCache;
    private final LockStore<String> lockStore;

    private CalendarTokenResolverImpl() {
        this.calendarTokenCache = CalendarTokenCache.getInstance();
        this.lockStore = new LockStore<>(STRIPES_COUNT);
    }

    private static class Instance {
        private static final CalendarTokenResolverImpl INSTANCE = new CalendarTokenResolverImpl();
    }

    /**
     * <p>
     * Gets the {@link CalendarTokenResolverImpl} instance
     * </p>
     *
     * @return {@link CalendarTokenResolverImpl} instance
     */
    public static CalendarTokenResolverImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearAllSyncTokens() {
        calendarTokenCache.clearSyncToken();
    }

    /**
     * {@inheritDoc}
     *
     * @param key Represents the key
     * @return Sync token stored for every sync
     */
    @Override
    public String getSyncToken(final String key) {
        final ReentrantReadWriteLock.ReadLock readLock = lockStore.getLock(key).readLock();
        final String syncToken;

        readLock.lock();

        try {
            syncToken = calendarTokenCache.getSyncToken(key);
        } finally {
            readLock.unlock();
        }

        return syncToken;
    }

    /**
     * {@inheritDoc}
     *
     * @param key   Represents the key
     * @param token Represents the sync token
     */
    @Override
    public void putSyncToken(final String key, final String token) {
        final ReentrantReadWriteLock.WriteLock writeLock = lockStore.getLock(key).writeLock();

        writeLock.lock();

        try {
            calendarTokenCache.putSyncToken(key, token);
        } finally {
            writeLock.unlock();
        }
    }
}
