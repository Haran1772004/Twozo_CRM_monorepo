package com.tlc.crm.entity.async.cache;

import com.tlc.crm.entity.async.SyncInfo;

/**
 * <p>
 * The {@code SyncInfoHolderResolver} is responsible for resolving and providing {@link SyncInfo} details.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface SyncInfoHolderResolver {

    /**
     * <p>
     * Resolves the {@link SyncInfo}.
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return {@link SyncInfo}
     */
    SyncInfo resolve(final long orgId, final Long syncId);

    /**
     * <p>
     * Sets the sync status to the cache with the provided synchronization id.
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param userId   Represents the user id
     * @param syncInfo Represents the {@link SyncInfo}
     */
    void setSyncStatus(final long orgId, final long userId, final SyncInfo syncInfo);

    /**
     * <p>
     * Checks whether the sync status is in progress or not
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the sync id
     * @return true, if the sync is in progress
     */
    boolean isSyncInProgress(final long orgId, final Long syncId);
}
