package com.tlc.sql.common.api.handler;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.ExistsQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.ConditionalColumn;
import com.tlc.sql.common.api.dml.With;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.TableDefinition;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * @author Abishek
 * @version 1.0
 */
public interface DMLHandler {

    String getSelectSQL(SelectQuery query);

    String getWithSQL(final With with);

    String getCountQuery(CountQuery query);

    /**
     * Generates SQL query string for EXISTS check query.
     *
     * @param query the EXISTS check query
     * @return the SQL string for EXISTS check query
     */
    String getExistsCheckSQL(final ExistsQuery query);

    String getDeleteSQL(DeleteQuery deleteQuery);

    String getInsertPreparedStatementSQL(TableDefinition<?> tableDef, Map<String, Object> columnValues);

    String getInsertPreparedStatementSQL(TableDefinition<?> tableDef);

    String getUpdatePreparedStatementSQL(UpdateQuery selectQuery);

    String getUpdatePreparedStatementSQL(TableDefinition<?> tableDef);

    void updatePreparedStatement(PreparedStatement statement, UpdateQuery updateQuery) throws SQLException;

    void updatePreparedStatement(PreparedStatement statement, TableDefinition<?> tableDef, Row row) throws SQLException;

    void insertPreparedStatement(PreparedStatement statement, TableDefinition<?> tableDef, Row row) throws SQLException;

    Object getValueFromResultSet(ResultSet resultSet, int position, DataType dataType) throws SQLException;

    String getColumnString(final Column column, final boolean needColumnAlias);

    String getConditionalColumnString(final ConditionalColumn<?, ?> column, final boolean needColumnAlias);

    TableResolver getTableResolver();
}
