package com.tlc.crm.entity.async.cache;

import com.tlc.crm.entity.async.SyncInfo;

import com.tlc.notification.model.sync.SyncStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * <p>
 * Provides the services for {@link SyncInfo} related actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface SyncInfoHolderService {

    /**
     * <p>
     * Gets the {@link SyncInfo}.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return {@link SyncInfo}
     */
    SyncInfo get(final long orgId, final Long syncId);

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
     * Sets the {@link SyncStatus} to the cache with the provided synchronization id.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    void setSyncStatus(final long orgId, final long userId, final SyncInfo syncInfo);

    /**
     * <p>
     * Checks whether the {@link SyncStatus} is in progress or not
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return true, if the sync is in progress
     */
    boolean isSyncInProgress(final long orgId, final Long syncId);
}
