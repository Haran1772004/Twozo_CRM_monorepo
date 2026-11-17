package com.tlc.crm.core.internal.async;

import com.tlc.crm.core.api.contact.model.ContactSyncSettings;
import com.tlc.crm.entity.async.SyncInfo;
import com.tlc.notification.model.sync.SyncStatus;

/**
 * <p>
 * Provides the services for executing contact synchronization task
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface ContactSyncHandler {

    /**
     * <p>
     * Gets the instance of {@link ContactSyncHandler}.
     * </p>
     *
     * @return {@link ContactSyncHandler}
     */
    static ContactSyncHandler getInstance() {
        return ContactSyncHandlerImpl.getInstance();
    }

    /**
     * <p>
     * Executes the synchronization process with the provided details.
     * </p>
     *
     * @param syncSettings Represents the sync settings
     */
    void processSynchronization(final ContactSyncSettings syncSettings);

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
