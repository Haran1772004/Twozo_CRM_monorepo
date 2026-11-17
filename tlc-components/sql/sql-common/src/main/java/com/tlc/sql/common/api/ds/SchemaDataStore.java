package com.tlc.sql.common.api.ds;

import com.tlc.sql.common.api.handler.DDLHandler;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.ddl.AdvDDLAction;
import com.tlc.sql.common.ddl.DDLAction;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

/**
 * Provides schema-level operations for managing database tables,
 * including table existence checks, table creation, and DDL action execution.
 */
public interface SchemaDataStore<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> {

    /**
     * Checks whether a table with the given name exists in the database.
     *
     * @param tableName the name of the table to check
     * @return {@code true} if the table exists, {@code false} otherwise
     * @throws SQLException if a database access error occurs
     */
    boolean isTableExists(String tableName) throws SQLException;

    /**
     * Creates database tables for the given collection of table definitions.
     *
     * @param tableDefinitions the collection of table definitions to create
     */
    void createTables(Collection<T> tableDefinitions);

    /**
     * Executes the given list of DDL actions against the database.
     *
     * @param actions the list of DDL actions to execute
     */
    void executeDDLActions(List<? extends DDLAction<C, T, D>> actions);

    /**
     * Reverts the given list of advanced DDL actions, undoing their changes.
     *
     * @param actions the list of advanced DDL actions to revert
     */
    void revertDDLActions(List<? extends AdvDDLAction<C, T, D>> actions);
}