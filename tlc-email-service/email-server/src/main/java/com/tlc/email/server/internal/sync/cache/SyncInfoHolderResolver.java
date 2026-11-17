package com.tlc.email.server.internal.sync.cache;

import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.common.model.address.EmailAddress;

/**
 * <p>
 * The {@code SyncInfoHolderResolver} is responsible for resolving and providing {@link SyncStatusInfo} details.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface SyncInfoHolderResolver {

    static SyncInfoHolderResolver get() {
        return SyncInfoHolderResolverImpl.getInstance();
    }

    /**
     * <p>
     * Resolves the {@link SyncStatusInfo}.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link SyncStatusInfo}
     */
    SyncStatusInfo resolve(final String emailAddress);

    /**
     * <p>
     * Sets the sync status to the cache.
     * </p>
     *
     * @param syncInfo Represents the {@link SyncStatusInfo}
     */
    void setSyncStatus(final SyncStatusInfo syncInfo);
}
