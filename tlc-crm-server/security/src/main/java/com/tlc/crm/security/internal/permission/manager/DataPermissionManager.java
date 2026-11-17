package com.tlc.crm.security.internal.permission.manager;

/**
 * Offers services for utilizing and loading data access permission information for tables and columns to facilitate authorization
 *
 * @author Sundar
 * @version 1.0
 * @see ReadableDataPermissionManager
 */
public interface DataPermissionManager extends ReadableDataPermissionManager {

    /**
     * Gets the {@link DataPermissionManager}
     *
     * @return the {@link DataPermissionManager} instance
     */
    static DataPermissionManager get() {
        return DataPermissionManagerImpl.getInstance();
    }

    /**
     * Loads the table and column permissions
     */
    void loadDataPermissions();
}