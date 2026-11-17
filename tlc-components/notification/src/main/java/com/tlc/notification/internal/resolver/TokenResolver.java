package com.tlc.notification.internal.resolver;

/**
 * <p>
 * Provides the services to resolve the tokens used for synchronization
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface TokenResolver {

    /**
     * <p>
     * Gets the sync token for respective key
     * </p>
     *
     * @param key Represents the key
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