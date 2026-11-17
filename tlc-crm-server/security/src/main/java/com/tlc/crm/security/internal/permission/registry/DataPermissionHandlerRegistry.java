package com.tlc.crm.security.internal.permission.registry;

import com.tlc.crm.security.internal.permission.handler.DataPermissionHandler;

/**
 * Registry stores and retries the {@link DataPermissionHandler} instances for each session
 *
 * @author sundar
 * @version 1.0
 */
public interface DataPermissionHandlerRegistry {

    /**
     * Gets the instance of the {@link DataPermissionHandlerRegistry}
     *
     * @return {@link DataPermissionHandlerRegistry}
     */
    static DataPermissionHandlerRegistry getInstance() {
        return DataPermissionHandlerRegistryImpl.getInstance();
    }

    /**
     * Checks whether the {@link DataPermissionHandler} is existing for the current session
     *
     * @return true if the {@link DataPermissionHandler} is already present for the current session
     */
    boolean contains();

    /**
     * Gets the {@link DataPermissionHandler}
     *
     * @return the {@link DataPermissionHandler}
     */
    DataPermissionHandler get();

    /**
     * Clears the data permission handler registry
     */
    void clear();

    /**
     * Removes the {@link DataPermissionHandler} associated with the current session from the registry
     */
    void remove();
}