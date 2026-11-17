package com.tlc.email.server.internal.sync;

import com.tlc.email.common.request.SyncRequest;

/**
 * <p>
 * Handles the email sync action related functionalities.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface SyncActionHandler {

    /**
     * Acquires the {@link SyncRequest} and delegates the actual processing to the {@link AbstractSyncActionHandler},
     * Describes the role of the emailProperties parameter, which contains the data needed for processing.
     *
     * <p>
     * After processing by the {@link AbstractSyncActionHandler}, the following steps occur:
     * <li>Fetches the emails from the specified provider (e.g., Gmail, Outlook).</li>
     * <li>Evaluates the folders and their associated intervals, ensuring only the relevant emails are fetched for each folder.</li>
     * <li>The processor then performs the necessary email processing (e.g., categorization, sorting, filtering).</li>
     * <li>Once the emails are fetched and processed, the system performs database actions to load the emails
     * into the database (e.g., updating tables with email data, syncing with the core CRM system, etc.).</li>
     * </p>
     *
     * @param syncRequest Represents the {@link SyncRequest}
     */
    void sync(final SyncRequest syncRequest);
}
