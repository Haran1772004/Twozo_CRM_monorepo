package com.tlc.crm.notifications.internal.async.calendar.cache;

import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.crm.entity.async.cache.SyncInfoHolderResolver;
import com.tlc.crm.notifications.calendar.internal.CalendarSyncService;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * <p>
 * Resolves the services of {@link SyncInfo} that hold the calendar-related synchronization information.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see SyncInfoHolderResolver
 */
public final class CalendarSyncInfoHolderResolver implements SyncInfoHolderResolver {

    private final CalendarSyncInfoHolder calendarSyncInfoHolder;
    private final ReentrantReadWriteLock lock;
    private final CalendarSyncService calendarSyncService;

    private CalendarSyncInfoHolderResolver() {
        this.calendarSyncInfoHolder = CalendarSyncInfoHolder.getInstance();
        this.lock = new ReentrantReadWriteLock();
        this.calendarSyncService = CalendarSyncService.getInstance();
    }

    private static class Instance {
        private static final CalendarSyncInfoHolderResolver INSTANCE = new CalendarSyncInfoHolderResolver();
    }

    /**
     * <p>
     * Gets the instance of {@link CalendarSyncInfoHolderResolver}.
     * </p>
     *
     * @return {@link CalendarSyncInfoHolderResolver}
     */
    public static CalendarSyncInfoHolderResolver getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the synchronization id
     * @return {@link SyncInfo}
     */
    @Override
    public SyncInfo resolve(final long orgId, final Long syncId) {
        final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        final SyncInfo calendarSyncInfo;

        readLock.lock();

        try {
            calendarSyncInfo = calendarSyncInfoHolder.get(syncId);
        } finally {
            readLock.unlock();
        }

        if (Objects.isNull(calendarSyncInfo)) {
            final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

            writeLock.lock();

            try {
                final SyncInfo syncInfo = calendarSyncService.getCalendarSyncInfo(orgId, syncId);

                if (Objects.nonNull(syncInfo)) {
                    calendarSyncInfoHolder.put(syncId, syncInfo);
                }

                return syncInfo;
            } finally {
                writeLock.unlock();
            }
        }

        return calendarSyncInfo;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    @Override
    public void setSyncStatus(final long orgId, final long userId, final SyncInfo syncInfo) {
        final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

        writeLock.lock();

        try {
            calendarSyncService.updateCalendarSyncStatus(orgId, userId, syncInfo);
            calendarSyncInfoHolder.put(syncInfo.syncId(), Objects.requireNonNull(syncInfo));
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return true, if the calendar sync is in progress
     */
    @Override
    public boolean isSyncInProgress(final long orgId, final Long syncId) {
        final SyncInfo syncInfo = resolve(orgId, syncId);

        return syncInfo.syncStatus() == SyncStatus.IN_PROGRESS;
    }
}
