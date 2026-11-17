package com.tlc.crm.fields.cache;

/**
 * <p>
 * A service that provides the loading data into a cache and clear data from it
 * </p>
 *
 * @author PandiarajkumarG
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
