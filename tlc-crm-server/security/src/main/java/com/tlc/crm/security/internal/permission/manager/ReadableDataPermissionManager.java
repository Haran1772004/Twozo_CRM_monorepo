package com.tlc.crm.security.internal.permission.manager;

import com.tlc.crm.security.internal.permission.model.ColumnPermission;
import com.tlc.crm.security.internal.permission.model.TablePermission;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Optional;

/**
 * Offers services to fetch the data access permission information of tables and columns to facilitate authorization
 *
 * @author Sundar
 * @version 1.0
 */
public interface ReadableDataPermissionManager {

    /**
     * Gets the {@link ReadableDataPermissionManager}
     *
     * @return the {@link ReadableDataPermissionManager} instance
     */
    static ReadableDataPermissionManager get() {
        return DataPermissionManager.get();
    }

    /**
     * Gets the {@link ColumnPermission} for the specified column
     *
     * @param column Represents the column associated with the {@link ColumnPermission}
     * @return The {@link ColumnPermission}
     */
    Optional<ColumnPermission> getColumnPermission(final Column column);

    /**
     * Gets the {@link TablePermission} for the specified table
     *
     * @param table Represents the table associated with the {@link TablePermission}
     * @return The {@link TablePermission}
     */
    Optional<TablePermission> getTablePermission(final Table table);

    /**
     * Gets the related tables of the given {@link Table}
     *
     * @param table Represents the table
     * @return {@link Collection} of related table names
     */
    Collection<String> getRelatedTables(final Table table);
}