package com.tlc.email.server.internal.manager;

import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.request.SyncRequest;

import com.tlc.email.server.model.SyncSetting;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Optional;

/**
 * <p>
 * Provides the service for sync settings related db load actions.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface SyncSettingsManager {

    static SyncSettingsManager getInstance() {
        return SyncSettingsManagerImpl.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param id Refers the email id.
     * @return {@link SyncSetting}
     */
    Optional<SyncSetting> get(final long id);

    /**
     * <p>
     * Gets the sync table
     * </p>>
     *
     * @return the table
     */
    Table getTable();

    /**
     * <p>
     * Loads the sync settings into target {@link DataContainer}.
     * </p>
     *
     * @param syncRequest     Represents the {@link SyncRequest}
     * @param targetContainer Represents the container to load the updated email related actions
     */
    void loadSyncSettingsIntoContainer(final SyncRequest syncRequest,
                                       final DataContainer targetContainer);

    /**
     * <p>
     * Loads the sync settings into {@link DataContainer}.
     * </p>
     *
     * @param syncRequest Represents the {@link SyncRequest}
     */
    void loadSyncSettingsIntoContainer(final SyncRequest syncRequest);

    /**
     * Updates the date configuration for a specific folder and email interval
     * into the provided target container.
     *
     * @param syncId        The unique identifier for the sync process.
     * @param folderName    The name of the folder being updated.
     * @param emailInterval The email interval containing start and end times to be added.
     */
    void updateDateConfigIntoContainer(final Long syncId, final String folderName, final EmailInterval emailInterval);

    /**
     * <p>
     * Retrieves the date configuration for the given {@link EmailAddress}. This method looks up the
     * email address associated with the provided request and fetches the corresponding date configuration
     * from the date configuration table.
     * </p>
     *
     * @param emailAddress The {@link EmailAddress} containing the email address to search for.
     * @return An {@link Optional} containing the date configuration if found, otherwise an empty {@link Optional}.
     */
    Optional<String> getDateConfig(final String emailAddress);

    /**
     * <p>
     * Gets the sync email id for the given email address.
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return the sync email id
     */
    Optional<Long> getId(final String emailAddress);

    /**
     * <p>
     * Gets the sync settings based on provided limits.
     * </p>
     *
     * @param limit Represents the upper bound of activity to be fetched
     * @return {@link Collection} of {@link SyncSetting}
     */
    Collection<SyncSetting> get(final short limit);

    /**
     * <p>
     * Updates the sync status.
     * </p>
     *
     * @param syncInfo {@link SyncStatusInfo}
     */
    void updateSyncStatus(final SyncStatusInfo syncInfo);

    /**
     * <p>
     * Gets the {@link SyncStatusInfo} based on given {@link EmailAddress}.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link SyncStatusInfo}
     */
    SyncStatusInfo getSyncInfo(final String emailAddress);
}
