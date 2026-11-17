package com.tlc.sql.common.util;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.common.api.meta.FKDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Utility class for handling operations related to database tables.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public final class TableUtils {

    private static final String COLUMN_FORMAT = "%s_%s";

    private TableUtils() {

    }

    /**
     * <p>
     * Fetches the foreign key columns of the given table
     * </p>
     *
     * @param table Represents the {@link Table}
     * @return {@link Map} - fk column as value and reference table name as key
     */
    public static Map<Table, Column> groupFkColumnsByTable(final Table table, final TableResolver tableResolver) {
        final Map<Table, Column> fkColumns = new HashMap<>();
        final Map<String, FKDefinition> fkDefinitions = table.getTableDefinition().getFKDefinitions();

        for (final FKDefinition fkDefinition : fkDefinitions.values()) {
            fkColumns.put(tableResolver.resolve(fkDefinition.getReferenceTable()), table.getColumn(fkDefinition.getLocal().getColumnName()));
        }

        return fkColumns;
    }

    /**
     * <p>
     * Fetches the foreign key columns of the given table
     * </p>
     *
     * @param table Represents the {@link Table}
     * @return {@link Map} - fk column as value and reference table name as key
     */
    public static Map<String, Column> groupFkColumnsByTableName(final Table table) {
        final Map<String, Column> fkColumns = new HashMap<>();
        final Map<String, FKDefinition> fkDefinitions = table.getTableDefinition().getFKDefinitions();

        for (final FKDefinition fkDefinition : fkDefinitions.values()) {
            fkColumns.put(fkDefinition.getReferenceTable(), table.getColumn(fkDefinition.getLocal().getColumnName()));
        }
        return fkColumns;
    }

    /**
     * <p>
     * Fetches the foreign key columns of the given table
     * </p>
     *
     * @param table Represents the {@link Table}
     * @return {@link Map} - fk column as value and reference table name as key
     */
    public static Map<String, String> groupFkRemoteColumnsByTableName(final Table table) {
        final Map<String, String> fkColumns = new HashMap<>();
        final Map<String, FKDefinition> fkDefinitions = table.getTableDefinition().getFKDefinitions();

        for (final FKDefinition fkDefinition : fkDefinitions.values()) {
            fkColumns.put(fkDefinition.getReferenceTable(), fkDefinition.getRemote().getColumnName());

        }
        return fkColumns;
    }

    /**
     * <p>
     * Fetches all the columns of the given table from the {@link TableDefinition}.
     * </p>
     *
     * @param table Sql table.
     * @return Columns of the table.
     */
    public static Collection<Column> getAllColumns(final Table table) {
        final Collection<String> columnNames = table.getTableDefinition().getColumns();

        return columnNames.stream().map(table::getColumn).toList();
    }

    /**
     * <p>
     * Fetches the alias table of the given {@link Table}.
     * </p>
     *
     * @param table It provides the name of the table.
     * @param alias Helps to create unique table instance for fields.
     * @return Alias table of the given {@link Table}.
     */
    public static Table getAliasTable(final Table table, final String alias, final TableResolver tableResolver) {
        return tableResolver.resolve(table.getName(), String.format(COLUMN_FORMAT, alias, table.getSeqId()));
    }
}
