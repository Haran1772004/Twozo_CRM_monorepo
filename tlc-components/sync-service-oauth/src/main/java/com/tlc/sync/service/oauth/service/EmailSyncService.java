package com.tlc.sync.service.oauth.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sync.service.oauth.internal.status.SyncErrorCodes;
import com.tlc.sync.service.oauth.model.EmailAddress;
import com.tlc.sync.service.oauth.model.request.TokenWriteRequest;
import com.tlc.sync.service.oauth.service.sync.SyncService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Configures the properties for email oauth authentication.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface EmailSyncService extends SyncService {

    String WHITE_SPACE_REGEX = "\\s+";

    /**
     * <p>
     * Fetches the all accounts present with their unique id
     * </p>
     *
     * @return the user id and email address
     */
    default Map<String, EmailAddress> fetchAccounts() {
        return Collections.emptyMap();
    }

    /**
     * <p>
     * Fetches the access token got by using the client credential
     * </p>
     *
     * @return the access token got by using client credential
     */
    default Optional<String> fetchClientCredentialToken() {
        return Optional.empty();
    }

    /**
     * <p>
     * Sets the refresh token used for further access token processes
     * </p>
     *
     * @param tokenWriteRequest Represents the {@link TokenWriteRequest}
     */
    void fetchAndStoreRefreshToken(final TokenWriteRequest tokenWriteRequest);

    /**
     * <p>
     * Gets new refresh token by using existing refresh token
     * </p>
     *
     * @param oldRefreshToken  Represents the refresh token
     * @return the new refresh token
     */
    default String getRefreshToken(final String oldRefreshToken) {
        throw ErrorCode.get(SyncErrorCodes.UNSUPPORTED_ACTION);
    }

    /**
     * <p>
     * Validates the token by verifying the scope
     * </p>
     *
     * @param tokenScope     Represents the token scope
     * @param requiredScope  Represents the scopes required
     */
    default void validateTokenScopes(final String tokenScope,
                                     final Collection<String> requiredScope) {
        final String receivedScope = tokenScope.toLowerCase();

        requiredScope.forEach(scope -> {

            if (!receivedScope.contains(scope)) {
                throw ErrorCode.get(SyncErrorCodes.INSUFFICIENT_PERMISSIONS);
            }
        });
    }
}
