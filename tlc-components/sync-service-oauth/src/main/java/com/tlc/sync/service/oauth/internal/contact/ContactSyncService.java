package com.tlc.sync.service.oauth.internal.contact;

import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.service.sync.SyncService;

/**
 * <p>
 * Configures the properties for contact oauth authentication
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface ContactSyncService extends SyncService {

    /**
     * <p>
     * Fetches the refresh token from provider and stores it in database
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    void fetchAndStoreRefreshToken(final TokenWriteRequest tokenWriteRequest);
}
