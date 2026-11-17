package com.tlc.email.server.internal.sync.cache;

import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.common.model.address.EmailAddress;

import com.tlc.notification.model.sync.SyncStatus;

/**
 * <p>
 * Provides read services for {@link SyncStatusInfo} related actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface SyncInfoReadService {

    static SyncInfoReadService get() {
        return SyncInfoServiceImpl.getInstance();
    }

    /**
     * <p>
     * Gets the {@link SyncStatusInfo}.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link SyncStatusInfo}
     */
    SyncStatusInfo get(final String emailAddress);

    /**
     * <p>
     * Gets the {@link SyncStatus} for the given {@link EmailAddress}.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link SyncStatus}
     */
    SyncStatus getSyncStatus(final String emailAddress);

    /**
     * <p>
     * Checks whether the {@link SyncStatus} is in progress or not.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return true, if the sync is in progress
     */
    boolean isInProgress(final String emailAddress);

    /**
     * <p>
     * Checks whether the {@link SyncStatus} is in progress or not.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return true, if the sync is in progress
     */
    boolean isInQueue(final String emailAddress);

    /**
     * <p>
     * Checks whether the {@link SyncStatus} is in progress or not.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return true, if the sync is in progress
     */
    boolean isSynced(final String emailAddress);
}
