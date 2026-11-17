package com.tlc.crm.notifications.internal.async.calendar.cache;

import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.crm.entity.async.cache.SyncInfoHolderService;

import com.tlc.notification.model.sync.SyncStatus;

/**
 * <p>
 * Provides the services for {@link SyncInfo} holder related functionalities.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see SyncInfoHolderService
 */
public final class CalendarSyncInfoHolderService implements SyncInfoHolderService {

    private final CalendarSyncInfoHolderResolver syncInfoHolderResolver;

    private CalendarSyncInfoHolderService() {
        this.syncInfoHolderResolver = CalendarSyncInfoHolderResolver.getInstance();
    }

    private static class Instance {
        private static final CalendarSyncInfoHolderService INSTANCE = new CalendarSyncInfoHolderService();
    }

    /**
     * <p>
     * Gets the instance of {@link CalendarSyncInfoHolderService}.
     * </p>
     *
     * @return {@link CalendarSyncInfoHolderService}
     */
    public static CalendarSyncInfoHolderService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the calendar sync id
     * @return {@link SyncInfo}
     */
    @Override
    public SyncInfo get(final long orgId, final Long syncId) {
        return syncInfoHolderResolver.resolve(orgId, syncId);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the calendar sync id
     * @return {@link SyncStatus}
     */
    @Override
    public SyncStatus getSyncStatus(final long orgId, final long syncId) {
        return get(orgId, syncId).syncStatus();
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
        syncInfoHolderResolver.setSyncStatus(orgId, userId, syncInfo);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the calendar sync id
     * @return true, if the calendar sync is in progress
     */
    @Override
    public boolean isSyncInProgress(final long orgId, final Long syncId) {
        return syncInfoHolderResolver.isSyncInProgress(orgId, syncId);
    }
}
