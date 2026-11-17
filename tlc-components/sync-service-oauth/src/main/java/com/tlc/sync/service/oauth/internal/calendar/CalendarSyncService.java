package com.tlc.sync.service.oauth.internal.calendar;

import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.service.sync.SyncService;

/**
 * <p>
 * Configures the properties for calendar oauth authentication.
 * </p>
 *
 * @author Ajith venkadesh
 * @version 1.0
 */
public interface CalendarSyncService extends SyncService {

    /**
     * <p>
     * Fetches the refresh token from the provider and stores it in database
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    void fetchAndStoreRefreshToken(final TokenWriteRequest tokenWriteRequest);
}
