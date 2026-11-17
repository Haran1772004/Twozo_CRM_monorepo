package com.tlc.sql.common.api.dml.secure;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.meta.FKDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Serves as a wrapper for {@link SelectQuery}, enabling the storage of the refactored history of the {@link SelectQuery}
 *
 * @author sundar
 * @version 1.0
 */
public class SelectQueryWrapper {

    private static final String COLUMN_ALIAS_FORMAT = "%s_%s";

    private final boolean isDirectSelect;
    private final SelectQuery selectQuery;
    private final Collection<Column> groupByColumns;
    private final Collection<Table> removableTables;
    private final Map<String, Map<String, Collection<String>>> columnAliasMap;
    private final Collection<Column> additionallySelectedColumns;

    private boolean selectedColumnsLoaded;
    private Collection<Column> selectedColumns;

    /**
     * Which initialize the wrapper with the given {@link SelectQuery}
     *
     * @param selectQuery    Represents the select query to be wrapped
     * @param isDirectSelect A flag indicating whether the select operation is direct which returns the results containing within the Collection. If the isDirectSelect flag is false then the results would be returned through the DataContainer
     */
    public SelectQueryWrapper(final SelectQuery selectQuery, final boolean isDirectSelect) {
        this.isDirectSelect = isDirectSelect;
        this.selectQuery = selectQuery;
        this.additionallySelectedColumns = new TreeSet<>();
        this.selectedColumnsLoaded = false;
        this.groupByColumns = selectQuery.getGroupByClause().stream().map(GroupByClause::getColumn).toList();
        this.columnAliasMap = isDirectSelect ? buildColumnAliasMap() : Collections.emptyMap();
        this.removableTables = new HashSet<>();
    }

    /**
     * Which initialize the wrapper with the given {@link SelectQuery}
     *
     * @param selectQuery Represents the select query to be wrapped
     */
    public SelectQueryWrapper(final SelectQuery selectQuery) {
        this(selectQuery, false);
    }

    /**
     * Gets the {@link Collection} of additionally selected columns.
     *
     * @return A {@link Collection} of additionally selected columns.
     */
    public Collection<Column> additionallySelectedColumns() {
        return Collections.unmodifiableCollection(additionallySelectedColumns);
    }

    /**
     * Appends the given column to the list of selected columns
     *
     * @param column The column to be added
     */
    public void addSelectClause(final Column column) {

        if (!selectedColumnsLoaded) {
            final Collection<Column> selectClauses = getSelectClauses();

            if (!selectClauses.isEmpty()) {
                this.selectedColumns = new TreeSet<>(selectClauses);
            }
            selectedColumnsLoaded = true;
        }

        if (!selectedColumns.contains(column)) {
            final Table table = column.getTable();
            final String columnName = column.getColumnName();
            final Column additionalColumn;

            if (isDirectSelect) {
                additionalColumn = table.getColumn(columnName, String.format(COLUMN_ALIAS_FORMAT, table.getAlias(), columnName));

                columnAliasMap.computeIfAbsent(table.getAlias(), tableName -> new HashMap<>())
                        .computeIfAbsent(columnName, newColumnName -> new HashSet<>()).add(additionalColumn.getColumnAlias());
            } else {
                additionalColumn = column;
            }
            additionallySelectedColumns.add(additionalColumn);
            selectQuery.addSelectClause(additionalColumn);
        }
    }

    /**
     * Appends the given column to the list of selected columns
     *
     * @param columns {@link Collection} of columns to be added
     */
    public void addSelectClause(final Collection<Column> columns) {

        for (final Column column : columns) {
            addSelectClause(column);
        }
    }

    /**
     * Removes the specified column from the {@link SelectQuery}
     *
     * @param column the column to be removed
     */
    public void removeSelectClause(final Column column) {

        if (!groupByColumns.contains(column)) {
            selectQuery.removeSelectClause(column);
            additionallySelectedColumns.remove(column);
            final String tableAlias = column.getTable().getAlias();

            if (isDirectSelect && columnAliasMap.containsKey(tableAlias)) {
                columnAliasMap.get(tableAlias).remove(column.getColumnAlias());
            }
        }
    }

    /**
     * Removes the specified columns from the {@link SelectQuery}
     *
     * @param columns Which contains the columns to be removed from the {@link SelectQuery}
     */
    public void removeSelectClause(final Collection<Column> columns) {

        for (final Column column : columns) {
            removeSelectClause(column);
        }
    }

    /**
     * Gets the columns to be selected
     *
     * @return {@link Collection} of columns to be selected
     */
    public Collection<Column> getSelectClauses() {
        return selectQuery.getSelectClause();
    }

    /**
     * Gets the base {@link Table} of the {@link SelectQuery}
     *
     * @return the base {@link Table}
     */
    public Table getBaseTable() {
        return selectQuery.getBaseTable();
    }

    /**
     * Sets the {@link WhereClause} to the {@link SelectQuery}
     *
     * @param whereClause Represents the {@link WhereClause} to be set to the {@link SelectQuery} to filter the results
     */
    public void setWhereClause(final WhereClause whereClause) {
        selectQuery.setWhereClause(whereClause);
    }

    /**
     * Gets the {@link WhereClause} of the {@link SelectQuery}
     *
     * @return {@link WhereClause}
     */
    public WhereClause getWhereClause() {
        return selectQuery.getWhereClause();
    }

    /**
     * Gets the tables associated with the {@link SelectQuery}
     *
     * @return {@link Collection} of tables associated with the {@link SelectQuery}
     */
    public Collection<Table> getTables() {
        return selectQuery.getTables();
    }

    /**
     * Gets the aliases of the columns in the select clause, grouped by their respective tables
     *
     * @return A {@link Map} where the keys represent table names and the values represent column aliases
     */
    public Map<String, Map<String, Collection<String>>> getColumnAliasMap() {
        return columnAliasMap;
    }

    /**
     * Adds the table to be removed from the results
     *
     * @param table the table to be removed
     */
    public void addRemovableTable(final Table table) {
        removableTables.add(table);
    }

    /**
     * Gets the table to be removed from the result set
     *
     * @return the table to be removed
     */
    public Collection<Table> getRemovableTables() {
        return Collections.unmodifiableCollection(removableTables);
    }

    /**
     * Gets the selected tables from the {@link SelectQuery}
     *
     * @return the selected tables from the {@link SelectQuery}
     */
    public Collection<Table> getSelectedTables() {
        return selectQuery.getSelectClause().stream().map(Column::getTable).collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Build a {@link Map} of column alias names, grouped by their respective selected table and original column names
     *
     * @return A {@link Map} grouped by the selected table and original column names
     */
    private Map<String, Map<String, Collection<String>>> buildColumnAliasMap() {
        final Map<String, Map<String, Collection<String>>> selectedColumnAliasMap = new HashMap<>();
        final Map<Table, TableDefinition<?>> tableDefinitionMap = new HashMap<>();
        final Map<Table, Collection<FKDefinition>> fkDefinitionMap = new HashMap<>();
        final Map<String, List<Table>> tableMap = selectQuery.getTables().stream().collect(Collectors.groupingBy(Table::getName));

        for (final Column column : selectQuery.getSelectClause()) {
            final String columnName = column.getColumnName();
            final Table table = column.getTable();

            final String columnAlias = column.getColumnAlias();

            selectedColumnAliasMap.computeIfAbsent(table.getAlias(), tableName -> new HashMap<>())
                    .computeIfAbsent(columnName, newColumnName -> new HashSet<>()).add(columnAlias);
            final TableDefinition<?> tableDefinition = tableDefinitionMap.computeIfAbsent(table, Table::getTableDefinition);

            if (DatabaseType.OLTP == tableDefinition.getDatabaseType()) {
                final Collection<FKDefinition> fkDefinitions = fkDefinitionMap.computeIfAbsent(table,
                        tableToFetchFKDefinitions -> tableDefinition.getFKDefinitions().values());

                for (final FKDefinition fkDefinition : fkDefinitions) {

                    if (Objects.equals(columnName, fkDefinition.getLocal().getColumnName())) {
                        final String referenceTableName = fkDefinition.getReferenceTable();
                        final Consumer<String> columnAliasLoader = tableName -> selectedColumnAliasMap.computeIfAbsent(tableName, tableAlias -> new HashMap<>())
                                .computeIfAbsent(fkDefinition.getRemote().getColumnName(), remoteColumnName -> new HashSet<>())
                                .add(columnAlias);

                        if (tableMap.containsKey(referenceTableName)) {
                            final List<Table> tables = tableMap.get(referenceTableName);

                            tables.forEach(referenceTable -> columnAliasLoader.accept(referenceTable.getAlias()));
                        } else {
                            columnAliasLoader.accept(referenceTableName);
                        }
                    }
                }
            }
        }
        return selectedColumnAliasMap;
    }

    /**
     * Gets the {@link GroupByClause} of the {@link SelectQuery}
     *
     * @return {@link GroupByClause}
     */
    public Collection<GroupByClause> getGroupByClauses() {
        return selectQuery.getGroupByClause();
    }

    /**
     * Sets the {@link GroupByClause} to the {@link SelectQuery}
     *
     * @param groupByClauses Represents the {@link GroupByClause} to be set to the {@link SelectQuery}
     */
    public void addGroupByClauses(final List<GroupByClause> groupByClauses) {
        selectQuery.addGroupByClause(groupByClauses);
    }

    /**
     * Sets the {@link JoinClause} to the {@link SelectQuery}
     *
     * @param joinClause Represents the {@link JoinClause} to be set to the {@link SelectQuery}
     */
    public void addJoinClause(final JoinClause joinClause) {
        selectQuery.addJoinClause(joinClause);
    }

    /**
     * Sets the {@link JoinClause} to the {@link SelectQuery}
     *
     * @param joinClauses Represents the {@link JoinClause} to be set to the {@link SelectQuery}
     */
    public void addJoinClause(final List<JoinClause> joinClauses) {
        selectQuery.addJoinClause(joinClauses);
    }

    /**
     * <p>
     * Gets the {@link JoinClause} from the {@link SelectQuery}
     * </p>
     *
     * @return the collection of {@link JoinClause}
     */
    public List<JoinClause> getJoinClause() {
        return selectQuery.getJoinClause();
    }
}