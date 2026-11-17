package com.tlc.sql.common.api.meta;

import lombok.Getter;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Abstract base class representing the metadata definition of a database table.
 * Holds column definitions, primary key, foreign keys, child table references,
 * and engine configuration.
 *
 * @param <C> the type of {@link ColumnDefinition} used by this table
 * @author Abishek
 * @version 1.0
 */
public abstract class TableDefinition<C extends ColumnDefinition> {

    @Getter
    private final String name;
    private final String nameLowercase;

    private final PKDefinition pkDefinition;
    private final Map<String, FKDefinition> fkDefinitions;
    private final Map<String, C> columnDefinitions;

    private final Set<String> childTables;
    @Getter
    private final Map<String, C> editableColumns;

    @Getter
    private final int seqId;
    @Getter
    private final TableType type;
    @Getter
    protected final TableEngine engine;

    protected <M extends TableMetaInput<?, C>> TableDefinition(String name, TableType type, int seqId, M metaInput) {
        this.type = Objects.requireNonNull(type);
        this.seqId = seqId;
        this.pkDefinition = Objects.requireNonNull(metaInput.getPkDefinition());
        this.name = Objects.requireNonNull(name);
        this.nameLowercase = name.toLowerCase();
        this.engine = metaInput.buildTableEngine();
        this.columnDefinitions = Map.copyOf(metaInput.getColumnDefinitions());
        this.childTables = Set.copyOf(metaInput.getChildTables());
        this.fkDefinitions = metaInput.getFkDefinitions();

        editableColumns = new HashMap<>(columnDefinitions);
        editableColumns.remove(ImmutableColumns.ORG_ID.getName());

        if (pkDefinition.hasSequenceGenerator()) {
            editableColumns.remove(pkDefinition.getColumnName());
        }
    }

    /**
     * Returns the foreign key definitions for this table.
     *
     * @return an unmodifiable map of foreign key name to {@link FKDefinition}
     */
    public Map<String, FKDefinition> getFKDefinitions() {
        return Collections.unmodifiableMap(fkDefinitions);
    }

    /**
     * Returns the table name in lowercase.
     *
     * @return the lowercase table name
     */
    public String getLowercaseName() {
        return nameLowercase;
    }

    /**
     * Returns the names of child tables that reference this table.
     *
     * @return a collection of child table names
     */
    public Collection<String> getChildTables() {
        return childTables;
    }

    /**
     * Returns all column definitions for this table.
     *
     * @return a map of column name to {@link ColumnDefinition}
     */
    public Map<String, C> getColumnDefinition() {
        return columnDefinitions;
    }

    /**
     * Returns the column definition for the specified column name.
     *
     * @param columnName the name of the column
     * @return the column definition, or {@code null} if not found
     */
    public C getColumnDefinition(String columnName) {
        return columnDefinitions.get(columnName);
    }

    /**
     * Returns the set of all column names in this table.
     *
     * @return a set of column names
     */
    public Set<String> getColumns() {
        return columnDefinitions.keySet();
    }

    /**
     * Returns a mutable copy of all column names excluding the primary key column.
     *
     * @return a set of non-primary-key column names
     */
    public Set<String> copyNonPkColumns() {
        final Set<String> columns = new HashSet<>(columnDefinitions.keySet());
        columns.remove(pkDefinition.getColumnName());
        return columns;
    }

    /**
     * Returns the primary key definition for this table.
     *
     * @param <P> the specific {@link PKDefinition} subtype
     * @return the primary key definition
     */
    public <P extends PKDefinition> P getPkDefinition() {
        return (P) pkDefinition;
    }

    /**
     * Returns the database type (OLTP or OLAP) that this table definition belongs to.
     *
     * @return the {@link DatabaseType}
     */
    public abstract DatabaseType getDatabaseType();
}