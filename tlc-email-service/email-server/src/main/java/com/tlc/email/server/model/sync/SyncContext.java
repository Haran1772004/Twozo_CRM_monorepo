package com.tlc.email.server.model.sync;

import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.FolderInterval;

import java.util.Collection;

/**
 * <p>
 * Represents the {@link SyncContext}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public record SyncContext(Long syncId, SyncAddressInfo syncAddressInfo,
                          Collection<FolderInterval> folderIntervals, StateData stateData) {

    /**
     * Creates a {@link SyncContext} with mandatory sync and address information.
     * Other fields are initialized as {@code null} and can be populated later.
     *
     * @param syncId the unique sync identifier
     * @param syncAddressInfo the address information associated with the sync
     */
    public SyncContext(Long syncId, SyncAddressInfo syncAddressInfo) {
        this(syncId, syncAddressInfo, null, null);
    }
}
