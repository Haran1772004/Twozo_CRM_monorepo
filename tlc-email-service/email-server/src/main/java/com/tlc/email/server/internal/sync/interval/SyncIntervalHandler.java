package com.tlc.email.server.internal.sync.interval;

import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.config.FolderType.FilterMode;
import com.tlc.email.common.request.SyncRequest;
import com.tlc.email.common.model.config.FolderInterval;

import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * <p>
 * Handles email synchronization requests.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface SyncIntervalHandler {

    /**
     * <p>
     * Gets the instance of {@link SyncIntervalHandler}
     * </p>
     *
     * @return {@link SyncIntervalHandler}
     */
    static SyncIntervalHandler getInstance() {
        return SyncIntervalHandlerImpl.getInstance();
    }

    /**
     * <p>
     * Fetches the auto sync folder intervals.
     * </p>
     *
     * @param emailAddress Represents the {@link EmailAddress}
     * @return {@link FolderInterval}
     */
    PriorityQueue<FolderInterval> fetchAutoSyncIntervals(final EmailAddress emailAddress);

    /**
     * <p>
     * Retrieves email intervals for each {@link FolderType} using the provided date configuration.
     * Each queue contains {@link EmailInterval}, sorted by start time in descending order.
     * </p>
     *
     * @param dateConfig Represents the data config
     * @return A map where each {@link FolderType} is associated with a {@link Queue} of {@link EmailInterval},
     * containing intervals sorted by start time.
     */
    Map<FolderType, List<EmailInterval>> retrieveEmailIntervalsByFolderType(final String dateConfig);

    /**
     * <p>
     * Gets the `dateConfig` JSON, adds the new interval, merge overlapping intervals,
     * and returns the updated JSON as string.
     * </p>
     *
     * @param dateConfig    Represents the date config
     * @param folderName    Represents the folder name
     * @param emailInterval Represents the {@link EmailInterval}
     * @return the data config json as string
     */
    String getUpdatedDateConfig(final String dateConfig, final String folderName, final EmailInterval emailInterval);

    /**
     * <p>
     * Loads synchronized and un synchronized intervals for email folders.
     * </p>
     *
     * @param syncRequest            The sync request containing email sync configurations.
     * @param syncedFolderInterval   The list to store synced folder intervals.
     * @param unSyncedFolderInterval The list to store un synced folder intervals.
     */
    void loadSyncAndUnsyncedIntervals(final SyncRequest syncRequest, final List<FolderInterval> syncedFolderInterval,
                                      final List<FolderInterval> unSyncedFolderInterval);
}
