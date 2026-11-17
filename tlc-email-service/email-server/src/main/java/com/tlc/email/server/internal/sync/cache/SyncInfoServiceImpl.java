package com.tlc.email.server.internal.sync.cache;

import com.tlc.email.common.model.sync.SyncStatusInfo;

import com.tlc.notification.model.sync.SyncStatus;

/**
 * <p>
 * Provides the services for {@link SyncStatusInfo} holder related functionalities.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see SyncInfoReadService
 */
final class SyncInfoServiceImpl implements SyncInfoService {

    private final SyncInfoHolderResolverImpl emailSyncInfoResolver;

    private SyncInfoServiceImpl() {
        this.emailSyncInfoResolver = SyncInfoHolderResolverImpl.getInstance();
    }

    private static class Instance {
        private static final SyncInfoServiceImpl INSTANCE = new SyncInfoServiceImpl();
    }

    /**
     * <p>
     * Gets the instance of {@link SyncInfoServiceImpl}.
     * </p>
     *
     * @return {@link SyncInfoServiceImpl}
     */
    public static SyncInfoServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link SyncStatusInfo}
     */
    @Override
    public SyncStatusInfo get(final String emailAddress) {
        return emailSyncInfoResolver.resolve(emailAddress);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return {@link SyncStatus}
     */
    @Override
    public SyncStatus getSyncStatus(final String emailAddress) {
        return get(emailAddress).getSyncStatus();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return true, if the sync status is in progress; otherwise false
     */
    @Override
    public boolean isInProgress(final String emailAddress) {
        final SyncStatusInfo syncInfo = get(emailAddress);

        return syncInfo.getSyncStatus() == SyncStatus.IN_PROGRESS;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return true, if the sync status is in queue; otherwise false
     */
    @Override
    public boolean isInQueue(final String emailAddress) {
        final SyncStatusInfo syncInfo = get(emailAddress);

        return syncInfo.getSyncStatus() == SyncStatus.IN_QUEUE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return true, if the sync status is synced; otherwise false
     */
    @Override
    public boolean isSynced(final String emailAddress) {
        final SyncStatusInfo syncInfo = get(emailAddress);

        return syncInfo.getSyncStatus() == SyncStatus.SYNCED;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param syncInfo Represents the {@link SyncStatusInfo}
     */
    @Override
    public void setSyncStatus(final SyncStatusInfo syncInfo) {
        emailSyncInfoResolver.setSyncStatus(syncInfo);
    }
}
