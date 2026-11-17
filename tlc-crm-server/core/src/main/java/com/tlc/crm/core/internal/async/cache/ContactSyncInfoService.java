package com.tlc.crm.core.internal.async.cache;

import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.crm.entity.async.cache.SyncInfoHolderService;

import com.tlc.notification.model.sync.SyncStatus;

/**
 * <p>
 * Provides the services for {@link SyncInfo} holder related functionalities.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 * @see SyncInfoHolderService
 */
public final class ContactSyncInfoService implements SyncInfoHolderService {

    private final ContactSyncInfoResolver syncInfoHolderResolver;

    private ContactSyncInfoService() {
        this.syncInfoHolderResolver = ContactSyncInfoResolver.getInstance();
    }

    private static class Instance {
        private static final ContactSyncInfoService INSTANCE = new ContactSyncInfoService();
    }

    /**
     * <p>
     * Gets the instance of {@link ContactSyncInfoService}.
     * </p>
     *
     * @return {@link ContactSyncInfoService}
     */
    public static ContactSyncInfoService getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param syncId Represents the contact sync id
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
     * @param syncId Represents the contact sync id
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
     * @param syncId Represents the contact sync id
     * @return true, if the contact sync is in progress
     */
    @Override
    public boolean isSyncInProgress(final long orgId, final Long syncId) {
        return syncInfoHolderResolver.isSyncInProgress(orgId, syncId);
    }
}