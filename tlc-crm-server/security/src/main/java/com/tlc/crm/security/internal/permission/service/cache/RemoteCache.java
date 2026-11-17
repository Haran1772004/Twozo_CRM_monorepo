package com.tlc.crm.security.internal.permission.service.cache;

/**
 * <p>
 * A service that provides loading the data into a cache
 * </p>
 *
 * @author sundar
 * @version 1.0
 */
public interface RemoteCache {

    /**
     * <p>
     * Loads the data into the cache
     * </P>
     */
    void load();
}
