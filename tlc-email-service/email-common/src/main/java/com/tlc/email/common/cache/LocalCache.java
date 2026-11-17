package com.tlc.email.common.cache;

/**
 * <p>
 * Provides the services that loads and clear the data from the cache.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public interface LocalCache {

    /**
     * <p>
     * Loads data into the cache
     * </P>
     */
    void load();

    /**
     * <p>
     * Clears all data from the cache
     * </p>
     */
    void clear();
}
