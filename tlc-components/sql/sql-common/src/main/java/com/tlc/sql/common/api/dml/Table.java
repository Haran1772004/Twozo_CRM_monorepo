package com.tlc.sql.common.api.dml;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.registrar.TableResolverRegistry;

/**
 * @author Abishek
 * @version 1.0
 */
public interface Table extends Comparable<Table> {

    static Table get(final String tableName) {
        return TableResolverRegistry.getInstance().get(DatabaseType.OLTP).resolve(tableName);
    }

    static Table get(final String tableName, final String tableAlias) {
        return TableResolverRegistry.getInstance().get(DatabaseType.OLTP).resolve(tableName, tableAlias);
    }

    static Table get(final DatabaseType databaseType, final String tableName) {
        return TableResolverRegistry.getInstance().get(databaseType).resolve(tableName);
    }

    static Table get(final DatabaseType databaseType, final String tableName, final String tableAlias) {
        return TableResolverRegistry.getInstance().get(databaseType).resolve(tableName, tableAlias);
    }

    <C extends ColumnDefinition,T extends TableDefinition<C>> T getTableDefinition();

    String getName();

    Column getPKColumn();

    Column getColumn(String columnName);

    Column getColumn(String columnName, String columnAlias);

    boolean containsColumn(final String columnAlias);

    boolean hasAlias();

    String getAlias();

    int getSeqId();
}