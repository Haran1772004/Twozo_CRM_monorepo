package com.tlc.crm.notifications.internal.async;

import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.crm.entity.async.SyncType;

import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.crm.notifications.internal.async.calendar.CalendarSyncExecutor;

/**
 * <p>
 * Provides the services for executing synchronization task.
 * </p>
 *
 * @param <T> Denotes the {@link SyncType} for which the instance is acquired.
 * @author KowsalyaSP
 * @version 1.0
 */
public interface SyncExecutorService<T> {

    /**
     * <p>
     * Gets the instance of {@link SyncExecutorService} based on {@link SyncType}.
     * </p>
     *
     * @return {@link SyncExecutorService}
     */
    static SyncExecutorService<?> getInstance(final SyncType syncType) {
        return CalendarSyncExecutor.getInstance();
    }

    /**
     * <p>
     * Executes the synchronization process with the provided details.
     * </p>
     *
     * @param syncSettings Represents the sync settings
     */
    void execute(final T syncSettings);

    /**
     * <p>
     * Gets the {@link SyncInfo} for the given sync id.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return {@link SyncInfo}
     */
    SyncInfo getSyncInfo(final long orgId, final long syncId);

    /**
     * <p>
     * Gets the {@link SyncStatus} for the given organization and sync id.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return {@link SyncStatus}
     */
    SyncStatus getSyncStatus(final long orgId, final long syncId);

    /**
     * <p>
     * Removes the sync status from the @link Cache}.
     * </p>
     *
     * @param syncId Represents the sync id
     */
    void remove(final long syncId);

    /**
     * <p>
     * Clears the sync status cache.
     * </p>
     */
    void clear();
}
