package com.tlc.notification.service;

/**
 * <p>
 * Service interface responsible for managing the caching of tokens.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface TokenService {

    /**
     * <p>
     * Gets the sync token for respective key
     * </p>
     *
     * @param key Represents the email address
     * @return Sync token stored for every sync
     */
    String getSyncToken(final String key);

    /**
     * <p>
     * Sets the sync token associated with the specified key.
     * </p>
     *
     * @param key   Represents the key
     * @param token Represents the sync token
     */
    void putSyncToken(final String key, final String token);

    /**
     * <p>
     * Clears the sync token cache stored
     * </p>
     */
    void clearAllSyncTokens();
}