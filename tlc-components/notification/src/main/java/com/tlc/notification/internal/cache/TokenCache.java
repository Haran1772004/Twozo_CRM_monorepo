package com.tlc.notification.internal.cache;

/**
 * <p>
 * Provides the cache services for tokens
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public interface TokenCache {

    /**
     * <p>
     * Gets the sync token for respective email address
     * </p>
     *
     * @param emailAddress Represents the email address
     * @return Sync token stored for every sync
     */
    String getSyncToken(final String emailAddress);

    /**
     * <p>
     * Stores the page token in cache
     * </p>
     *
     * @param emailAddress Represents the email address
     * @param token        Represents the sync token
     */
    void putSyncToken(final String emailAddress, final String token);

    /**
     * <p>
     * Clears the sync token stored in cache
     * </p>
     */
    void clearSyncToken();
}