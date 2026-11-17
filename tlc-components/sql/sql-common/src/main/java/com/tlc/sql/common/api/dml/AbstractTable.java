package com.tlc.sql.common.api.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractTable implements Table {

    private final String tableName;
    @Getter
    private final String alias;
    private final Map<String, Column> columnCache;
    private final int uniqueSeqId;
    private final boolean hasAlias;

    private final Column pkColumn;

    @Getter
    protected final TableDefinition<?> tableDefinition;

    private static final String FORMAT = "%s_%s";


    protected AbstractTable(String name, String alias) {
        this.tableName = Objects.requireNonNull(name).trim();
        this.tableDefinition = getTableDefinition(tableName);
        if (tableDefinition == null) {
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_UNKNOWN_TABLE, name);
        }
        this.uniqueSeqId = tableDefinition.getSeqId();
        if (alias == null) {
            this.hasAlias = true;
            this.alias = Integer.toString(uniqueSeqId);
        } else {
            this.hasAlias = !name.equals(alias);
            this.alias = Objects.requireNonNull(alias);
        }
        this.columnCache = new ConcurrentHashMap<>();
        final String pkColumnName = tableDefinition.getPkDefinition().getColumnName();
        columnCache.put(pkColumnName, buildColumn(pkColumnName, pkColumnName, true));
        this.pkColumn = getColumn(pkColumnName);
    }

    protected AbstractTable(TableDefinition<?> tableDefinition, String alias) {
        this.tableName = tableDefinition.getName();
        this.tableDefinition = tableDefinition;
        this.uniqueSeqId = tableDefinition.getSeqId();
        if (alias == null) {
            this.hasAlias = true;
            this.alias = Integer.toString(uniqueSeqId);
        } else {
            this.hasAlias = !tableName.equals(alias);
            this.alias = Objects.requireNonNull(alias);
        }
        this.columnCache = new HashMap<>();
        final String pkColumnName = tableDefinition.getPkDefinition().getColumnName();
        columnCache.put(pkColumnName, buildColumn(pkColumnName, pkColumnName, true));
        this.pkColumn = getColumn(pkColumnName);
    }

    public Column getPKColumn() {
        return pkColumn;
    }

    public Column getColumn(String columnName) {
        return columnCache.computeIfAbsent(columnName, k -> buildColumn(columnName, columnName, false));
    }

    public Column getColumn(String columnName, String columnAlias) {
        final String key = String.format(FORMAT, columnName, columnAlias);
        return columnCache.computeIfAbsent(key, k -> buildColumn(columnName, columnAlias, false));
    }

    /**
     * Checks if the specified column, identified by its alias, exists in the table
     *
     * @param columnAlias The alias name of the column
     * @return true if the column is present in the table, false otherwise
     */
    public boolean containsColumn(final String columnAlias) {
        return columnCache.containsKey(columnAlias);
    }

    public String getName() {
        return tableName;
    }

    public boolean hasAlias() {
        return hasAlias;
    }

    @Override
    public String toString() {
        if (tableName.equals(alias)) {
            return tableName;
        } else {
            return tableName + "_" + alias;
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Table) {
            return this.hashCode() == object.hashCode();
        }
        return false;
    }

    public boolean equals(Table table) {
        return this.uniqueSeqId == table.getSeqId() && Objects.equals(alias, table.getAlias());
    }

    public int getSeqId() {
        return uniqueSeqId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(alias, uniqueSeqId);
    }

    @Override
    public int compareTo(@NotNull final Table table) {
        final int result = Integer.compare(this.uniqueSeqId, table.getSeqId());

        if (result == 0) {
            return this.alias.compareTo(table.getAlias());
        }
        return result;
    }

    protected abstract Column buildColumn(final String columnName, final String columnAlias, final boolean isPkColumn);

    protected abstract TableDefinition getTableDefinition(final String tableName);
}
