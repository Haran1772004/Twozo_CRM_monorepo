package com.tlc.crm.notifications.internal.async.calendar.cache;

import com.tlc.cache.Cache;
import com.tlc.cache.service.provider.CacheServiceProvider;
import com.tlc.cache.remote.CacheConfig;

import com.tlc.crm.entity.async.SyncInfo;

import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.crm.notifications.calendar.fields.CalendarFields;

import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Manages the storage and retrieval of calendar synchronization {@link SyncStatus} task.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public final class CalendarSyncInfoHolder {

    private final Cache<Long, SyncInfo> calendarsyncInfoCache;

    private static class Instance {
        private static final CalendarSyncInfoHolder INSTANCE = new CalendarSyncInfoHolder();
    }

    private CalendarSyncInfoHolder() {
        final CacheConfig<Long, SyncInfo> calendarSyncInfo = new CacheConfig<>(CalendarFields.CALENDAR_SYNC_INFO);

        calendarSyncInfo.setTtl((int) TimeUnit.MINUTES.toMillis(5));
        this.calendarsyncInfoCache = CacheServiceProvider.get().createOrOpenCache(calendarSyncInfo);
    }

    /**
     * <p>
     * Gets the instance of {@link CalendarSyncInfoHolder}
     * </p>
     *
     * @return {@link CalendarSyncInfoHolder}
     */
    public static CalendarSyncInfoHolder getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets the {@link SyncInfo} from the cache based on the provided synchronization id.
     * </p>
     *
     * @param syncId Represents the synchronization id
     * @return {@link SyncInfo}
     */
    public SyncInfo get(final long syncId) {
        return calendarsyncInfoCache.get(syncId);
    }

    /**
     * <p>
     * Puts the {@link SyncInfo} into the cache.
     * </p>
     *
     * @param syncId   Represents the sync id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    public void put(final long syncId, final SyncInfo syncInfo) {
        calendarsyncInfoCache.put(syncId, syncInfo);
    }

    /**
     * <p>
     * Removes the {@link SyncStatus} associated with the provided synchronization id from the cache.
     * </p>
     *
     * @param syncId Represents the synchronization id
     */
    public void remove(final long syncId) {
        calendarsyncInfoCache.remove(syncId);
    }

    /**
     * <p>
     * Clears the {@link SyncStatus} from the cache.
     * </p>
     */
    public void clear() {
        calendarsyncInfoCache.clear();
    }
}