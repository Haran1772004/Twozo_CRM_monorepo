package com.tlc.sql.common.api.handler;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.StringUtil;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.ConditionalColumn;
import com.tlc.sql.common.api.dml.Connector;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.ExistsQuery;
import com.tlc.sql.common.api.dml.GroupByClause;
import com.tlc.sql.common.api.dml.HavingClause;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.JsonColumn;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.Operator;
import com.tlc.sql.common.api.dml.OrderByClause;
import com.tlc.sql.common.api.dml.SelectClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.With;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.dml.secure.CTESelectQuery;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.util.DDLDMLUtil;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AbstractDMLHandler is an abstract class that provides common SQL query generation logic.
 * It serves as a base class for database-specific implementations.
 *
 * @author Abishek
 * @version 1.0
 */
public abstract class AbstractDMLHandler implements DMLHandler
{
	private static final String FORWARD_SLASH = "/";
    private static final String SINGLE_QUOTE = "'";
    private static final String ESCAPE = " ESCAPE ";
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]+");
    private static final String ESCAPE_REQUIRED_CHAR = "[%_\\$\\\\]";

    protected static final String PREPARED_STMT_CONSTANT = "?";

	/**
	 * Generates the SQL string for a SELECT query.
	 *
	 * @param selectQuery the SelectQuery object containing query details
	 * @return the generated SQL string
	 */
	@Override
	public String getSelectSQL(SelectQuery selectQuery)
	{
		final StringBuilder selectQueryBuilder = new StringBuilder(500);
		final SortedSet<SelectClause> selectClauses = selectQuery.getSelectClauses();

		selectQueryBuilder.append("SELECT ");

		if (Objects.isNull(selectClauses) || selectClauses.isEmpty()) {
			throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_SELECT_COLUMNS);
		} else {
			fillSelectClause(selectQueryBuilder, selectClauses);
		}
		final Table baseTable = selectQuery.getBaseTable();
		selectQueryBuilder.append(" FROM ");

		if (selectQuery instanceof CTESelectQuery cteSelectQuery) {
			final String cteName = cteSelectQuery.getCteName();

			selectQueryBuilder.append(cteName).append(" AS ").append("\"").append(cteName).append("\"");
			fillFromClause(selectQueryBuilder, selectQuery.getJoinClause());
		} else {
			selectQueryBuilder.append(getTableSource(baseTable));
			fillFromClause(selectQueryBuilder, selectQuery.getJoinClause());
		}

		final WhereClause whereClause = selectQuery.getWhereClause();
		if(whereClause != null)
		{
			selectQueryBuilder.append(" WHERE ");
			fillWhereClause(selectQueryBuilder, whereClause, true);
		}

		final List<GroupByClause> groupByClause = selectQuery.getGroupByClause();
		if(groupByClause != null && !groupByClause.isEmpty())
		{
			fillGroupByClause(selectQueryBuilder, groupByClause);
			final HavingClause havingClause = selectQuery.getHavingClause();

			if (havingClause != null) {
				selectQueryBuilder.append(" HAVING ");
				fillHavingClause(selectQueryBuilder, havingClause);
			}
		}

		final List<OrderByClause> orderByClause = selectQuery.getOrderByClause();
		if(orderByClause != null && !orderByClause.isEmpty())
		{
			selectQueryBuilder.append(" ORDER BY ");
			fillOrderByClause(selectQueryBuilder, orderByClause);
		}
		else
		{
			final String defaultOrderBy = getDefaultOrderByClause(baseTable);
			if(defaultOrderBy != null)
			{
				selectQueryBuilder.append(" ORDER BY ").append(defaultOrderBy);
			}
		}

		final LimitClause limitClause = selectQuery.getLimitClause();
		if(limitClause != null)
		{
			selectQueryBuilder.append(" ").append(getLimitClauseStr(limitClause));
		}
		return selectQueryBuilder.toString();
	}

	/**
	 * Generates the SQL string for a WITH clause.
	 *
	 * @param with the With object containing derived tables and the main query
	 * @return the generated SQL string for the WITH clause
	 */
	@Override
	public String getWithSQL(final With with) {
		final StringBuilder withClauseBuilder = new StringBuilder("WITH ");

		for (final DerivedTable derivedTable : with.getDerivedTables()) {
			final String alias = derivedTable.getAlias();

			withClauseBuilder.append(String.format("%s AS ( %s ) ,", alias, getSelectSQL(derivedTable.getSelectQuery())));
		}
		StringUtil.replaceSuffix(withClauseBuilder, NumericalConstant.ONE, StringUtil.EMPTY_STRING);

		return withClauseBuilder.append(String.format(" %s ", getSelectSQL(with.getSelectQuery()))).toString();
	}

	/**
	 * Generates the SQL string for a COUNT query.
	 *
	 * @param query the CountQuery object containing query details
	 * @return the generated SQL string for the COUNT query
	 */
	@Override
	public String getCountQuery(final CountQuery query) {
		final Table baseTable = query.getBaseTable();
		final StringBuilder selectQueryBuilder = new StringBuilder(500);

		selectQueryBuilder.append("SELECT COUNT(");
		query.getColumn().ifPresentOrElse(column -> {

			if (query.isDistinct()) {
				selectQueryBuilder.append("DISTINCT ");
			}
			selectQueryBuilder.append(getColumnIdentity(column, true, false));
		}, () -> selectQueryBuilder.append("*"));
		selectQueryBuilder.append(") FROM ");
		selectQueryBuilder.append(getTableSource(baseTable));
		fillFromClause(selectQueryBuilder, query.getJoinClause());

		final WhereClause whereClause = query.getWhereClause();

		if (whereClause != null) {
			selectQueryBuilder.append(" WHERE ");
			fillWhereClause(selectQueryBuilder, whereClause, true);
		}
		return selectQueryBuilder.toString();
	}

	/**
	 * Generates the SQL string for an EXISTS check query.
	 *
	 * @param query the EXISTS check query
	 * @return the generated SQL string for the EXISTS check query
	 */
	@Override
	public String getExistsCheckSQL(final ExistsQuery query) {
		final StringBuilder existsQueryBuilder = new StringBuilder(500);
		final SelectQuery selectQuery = SelectQuery.get(query.getBaseTable());

		selectQuery.addStaticSelectClause(NumericalConstant.ONE);
		selectQuery.addJoinClause(query.getJoinClause());
		selectQuery.setWhereClause(query.getWhereClause());

		existsQueryBuilder.append("SELECT EXISTS(");
		existsQueryBuilder.append(getSelectSQL(selectQuery));
		existsQueryBuilder.append(")");

		return existsQueryBuilder.toString();
	}

	/**
	 * Generates the SQL string for an INSERT prepared statement with specified column values.
	 *
	 * @param tableDef the table definition
	 * @param columnValues a map of column names to their values
	 * @return the generated SQL string for the INSERT prepared statement
	 */
	@Override
	public String getInsertPreparedStatementSQL(TableDefinition<?> tableDef, Map<String, Object> columnValues)
	{
		final StringBuilder builder =new StringBuilder(400);
		builder.append("INSERT INTO ");
		builder.append(getTableName(tableDef)).append(" (");

		final Iterator<Entry<String, Object>> itr = columnValues.entrySet().iterator();
		final StringBuilder valuesBuffer = new StringBuilder(200);
		final int maxColumns = columnValues.size();
		for( int index = 1; itr.hasNext(); index++)
		{
			final Entry<String, Object> entry = itr.next();
			final Object value = entry.getValue();
			final String column = entry.getKey();
			final DataType dataType = tableDef.getColumnDefinition(column).getDataType();

			builder.append(getColumnName(column));

			if(value == PREPARED_STMT_CONSTANT)
			{
				valuesBuffer.append(PREPARED_STMT_CONSTANT);
			}
			else
			{
				valuesBuffer.append(getColumnValue(dataType, value));
			}
			if(index < maxColumns)
			{
				builder.append(", ");
				valuesBuffer.append(", ");
			}
		}
		builder.append(") VALUES (");
		builder.append(valuesBuffer);
		builder.append(")");
		return builder.toString();
	}

	/**
	 * Generates the SQL string for an INSERT prepared statement with all columns.
	 *
	 * @param tableDef the table definition
	 * @return the generated SQL string for the INSERT prepared statement
	 */
	@Override
	public String getInsertPreparedStatementSQL(final TableDefinition<?> tableDef)
	{
		final Map<String, Object> valueMap = new LinkedHashMap<>();
		final Collection<String> columns = tableDef.getColumns();
		columns.forEach( column -> valueMap.put(column , PREPARED_STMT_CONSTANT));
		return getInsertPreparedStatementSQL(tableDef, valueMap);
	}

	/**
	 * Generates the SQL string for an UPDATE prepared statement using a table definition.
	 *
	 * @param tableDefinition the table definition
	 * @return the generated SQL string for the UPDATE prepared statement
	 */
	@Override
	public String getUpdatePreparedStatementSQL(final TableDefinition<?> tableDefinition)
	{
		final StringBuilder updateQueryBuilder = new StringBuilder(500);

		final String tableName = tableDefinition.getName();

		final Collection<String> columns = tableDefinition.getEditableColumns().keySet();
		final Table table = getTableResolver().resolve(tableName);
		final Column pkColumn = table.getPKColumn();

		final Map<Column, Object> columnValueMap = new LinkedHashMap<>();
		columns.forEach( column -> columnValueMap.put(table.getColumn(column) , PREPARED_STMT_CONSTANT));

		updateQueryBuilder.append("UPDATE ").append(getTableName(tableDefinition));
		updateQueryBuilder.append(" SET ");

		fillSetColumnValues(updateQueryBuilder, columnValueMap);

		final WhereClause criteriaBuilder = new WhereClause(Criteria.eq(pkColumn, PREPARED_STMT_CONSTANT));
		updateQueryBuilder.append(" WHERE ");
		fillWhereClause(updateQueryBuilder, criteriaBuilder, false);

		return updateQueryBuilder.toString();
	}

	/**
	 * Generates the SQL string for an UPDATE prepared statement using an UpdateQuery object.
	 *
	 * @param updateQuery the UpdateQuery object containing query details
	 * @return the generated SQL string for the UPDATE prepared statement
	 */
	@Override
	public String getUpdatePreparedStatementSQL(UpdateQuery updateQuery)
	{
		final StringBuilder updateQueryBuilder = new StringBuilder(500);
		final Table baseTable = updateQuery.getBaseTable();
		updateQueryBuilder.append("UPDATE ").append(getTableName(baseTable));

		final Map<Column, Object> updateColumnMap = updateQuery.getColumnValueMap();
		if(updateColumnMap == null || updateColumnMap.isEmpty())
		{
			throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_UPDATE_COLUMNS);
		}
		final Map<Column, Object> columnValueMap = new LinkedHashMap<>();
		updateColumnMap.keySet().forEach( column -> columnValueMap.put(column , PREPARED_STMT_CONSTANT));
		updateQueryBuilder.append(" SET ");

		fillSetColumnValues(updateQueryBuilder, columnValueMap);

		final WhereClause whereClause = updateQuery.getWhereClause();
		if(whereClause != null)
		{
			updateQueryBuilder.append(" WHERE ");
			fillWhereClause(updateQueryBuilder, whereClause, false);
		}
		return updateQueryBuilder.toString();
	}

	/**
	 * Generates the SQL string for a DELETE query.
	 *
	 * @param deleteQuery the DeleteQuery object containing query details
	 * @return the generated SQL string for the DELETE query
	 */
	@Override
	public String getDeleteSQL(DeleteQuery deleteQuery)
	{
		final StringBuilder deleteQueryBuilder = new StringBuilder(100);
		deleteQueryBuilder.append("DELETE FROM ");
		final Table baseTable = deleteQuery.getBaseTable();
		deleteQueryBuilder.append(getTableName(baseTable));
		final WhereClause criteriaBuilder = deleteQuery.getWhereClause();
		if(criteriaBuilder != null)
		{
			deleteQueryBuilder.append(" WHERE ");
			fillWhereClause(deleteQueryBuilder, criteriaBuilder, false);
		}
		return deleteQueryBuilder.toString();
	}

	/**
	 * Updates a prepared statement with values from a Row object for a given table definition.
	 *
	 * @param statement the PreparedStatement object to be updated
	 * @param tableDef the table definition
	 * @param row the Row object containing the values
	 * @throws SQLException if an SQL error occurs
	 */
	@Override
	public void updatePreparedStatement(PreparedStatement statement, TableDefinition<?> tableDef, Row row) throws SQLException
	{
		final Map<String, ? extends ColumnDefinition> columns = tableDef.getEditableColumns();
		int index = 1;
		for (final Entry<String, ? extends ColumnDefinition> entry : columns.entrySet())
		{
			final String columnName = entry.getKey();
			final Object value = row.get(columnName);
			final DataType dataType = entry.getValue().getDataType();
			setInsidePreparedStatement(statement, index++, dataType, value);
		}

		final Long pkValue = row.getPKValue();
		setInsidePreparedStatement(statement, index, CommonDataType.BIGINT, pkValue);
	}

	/**
	 * Updates a prepared statement with values from an UpdateQuery object.
	 *
	 * @param statement the PreparedStatement object to be updated
	 * @param updateQuery the UpdateQuery object containing the values
	 * @throws SQLException if an SQL error occurs
	 */
	@Override
	public void updatePreparedStatement(PreparedStatement statement, UpdateQuery updateQuery) throws SQLException
	{
		final Map<Column , Object> dataMap = updateQuery.getColumnValueMap();
		int index = 1;
		for (final Entry<Column , Object> entry : dataMap.entrySet())
		{
			final Column column = entry.getKey();
			final Object value = entry.getValue();
			final DataType dataType = column.getColumnDefinition().getDataType();
			setInsidePreparedStatement(statement, index++, dataType, value);
		}
	}

	/**
	 * Inserts values from a Row object into a prepared statement for a given table definition.
	 *
	 * @param statement the PreparedStatement object to be updated
	 * @param tableDef the table definition
	 * @param row the Row object containing the values
	 * @throws SQLException if an SQL error occurs
	 */
	@Override
	public void insertPreparedStatement(PreparedStatement statement, final TableDefinition<?> tableDef, Row row) throws SQLException
	{
		final Map<String, ? extends ColumnDefinition> columns = tableDef.getColumnDefinition();
		int index = 1;

		for(final Map.Entry<String, ? extends ColumnDefinition> entry : columns.entrySet())
		{
			final ColumnDefinition columnDef = entry.getValue();
			final DataType dataType = columnDef.getDataType();
			final Object value = row.get(entry.getKey());

			if(value == null)
			{
				setInsidePreparedStatement(statement, index, dataType, columnDef.getDefaultValue());
			}
			else
			{
				setInsidePreparedStatement(statement, index, dataType, value);
			}
			index++;
		}
	}

	/**
	 * Retrieves a value from a ResultSet for a given position and data type.
	 *
	 * @param resultSet the ResultSet object
	 * @param position the column position
	 * @param dataType the data type of the column
	 * @return the retrieved value
	 * @throws SQLException if an SQL error occurs
	 */
	@Override
	public Object getValueFromResultSet(ResultSet resultSet, int position, DataType dataType) throws SQLException {
		final Object object = resultSet.getObject(position);

		if (resultSet.wasNull()) {
			return null;
		} else {
			return dataType.getWrappedValue(object);
		}
	}

	/**
	 * Converts a Boolean value to its SQL string representation.
	 * Subclasses can override for dialect-specific boolean literals (e.g., {@code 1/0} for ClickHouse).
	 *
	 * @param value the Boolean value to convert
	 * @return the SQL string representation ({@code "true"} or {@code "false"})
	 */
	protected String getBooleanString(Boolean value) {
		if (value) {
			return "true";
		} else {
			return "false";
		}
	}

	/**
	 * Returns the SQL fragment for ordering NULL values in an ORDER BY clause.
	 * Subclasses can override for dialect-specific behavior (e.g., ClickHouse returns empty string).
	 *
	 * @param type      the order direction (ASC or DESC)
	 * @param nullFirst whether NULL values should appear first
	 * @return the SQL fragment for NULL ordering (e.g., {@code " NULLS FIRST"})
	 */
	protected String getOrderByClauseNullStr(OrderByClause.OrderType type, boolean nullFirst) {
		if (nullFirst) {
			return type == OrderByClause.OrderType.ASCENDING ? " NULLS FIRST" : " NULLS LAST";
		} else {
			return type == OrderByClause.OrderType.ASCENDING ? " NULLS LAST" : " NULLS FIRST";
		}
	}

	/**
	 * Returns the default ORDER BY clause for a given table, if any.
	 * Default implementation returns {@code null} (no default ordering).
	 *
	 * @param baseTable the base table of the query
	 * @return the default ORDER BY clause string, or {@code null} if none
	 */
	protected String getDefaultOrderByClause(Table baseTable)
	{
		return null;
	}

	/**
	 * Generates the SQL LIMIT clause string from the given {@link LimitClause}.
	 * Subclasses can override for dialect-specific syntax (e.g., ClickHouse max UINT64 instead of ALL).
	 *
	 * @param limitClause the limit clause containing start position and number of results
	 * @return the SQL LIMIT clause string
	 * @throws com.tlc.commons.code.impl.DefaultErrorCode if the start position is invalid
	 */
	protected String getLimitClauseStr(LimitClause limitClause)
	{
		final int start = limitClause.getStart();
		final int numberOfResults = limitClause.getNumberOfResults();

		if(start <= 0)
		{
			throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_LIMIT_CLAUSE);
		}
		final StringBuilder limitBuilder = new StringBuilder(25);
		limitBuilder.append("LIMIT ");
		if(numberOfResults <= 0)
		{
			limitBuilder.append("ALL");
		}
		else
		{
			limitBuilder.append(numberOfResults);
		}
		if( start > 1 )
		{
			limitBuilder.append(" OFFSET ").append(start - 1);
		}
		return limitBuilder.toString();
	}

	/**
	 * Formats a value as a quoted SQL string literal, escaping special characters.
	 *
	 * @param value    the value to format
	 * @param dataType the data type of the column (reserved for subclass overrides)
	 * @return the formatted SQL string literal (e.g., {@code 'escaped_value'})
	 */
	protected String getFormattedString(Object value, DataType dataType)
	{
		return "'"+escapeSpecialChars(value.toString())+ "'";
	}

	/**
	 * Escapes special characters in a value string for safe inclusion in SQL.
	 * Subclasses can override for additional escaping (e.g., backslashes in ClickHouse).
	 *
	 * @param value the input string
	 * @return the escaped string with single quotes doubled and whitespace trimmed
	 */
	protected String escapeSpecialChars(String value)
	{
		return value.replace("'","''").trim();
	}

	/**
	 * Returns the quoted table name for use in SQL statements.
	 * Subclasses can override for dialect-specific quoting (e.g., backticks for ClickHouse).
	 *
	 * @param table the table name
	 * @return the quoted table name
	 */
	protected String getTableName(String table)
	{
		return DDLDMLUtil.getTableName(table);
	}

	/**
	 * Returns the quoted table name for the given {@link TableDefinition}.
	 *
	 * @param tableDefinition the table definition
	 * @return the quoted table name
	 */
	protected String getTableName(TableDefinition<?> tableDefinition)
	{
		return DDLDMLUtil.getTableName(tableDefinition);
	}

	/**
	 * Returns the quoted column name for use in SQL statements.
	 * Subclasses can override for dialect-specific quoting.
	 *
	 * @param column the column name
	 * @return the quoted column name
	 */
	protected String getColumnName(String column)
	{
		return DDLDMLUtil.getColumnName(column);
	}

	/**
	 * Returns the quoted column name from a {@link Column} object.
	 *
	 * @param column the column object
	 * @return the quoted column name
	 */
	protected String getColumnName(final Column column)
	{
		return getColumnName(column.getColumnName());
	}

	/**
	 * Generates a SQL bitwise comparison expression.
	 * Produces: {@code (column & value) = response} or {@code (column | value) = response}.
	 *
	 * @param column         the column to apply the bitwise operation on
	 * @param updateTableDef whether to include table aliases
	 * @param operator       the bitwise operator ({@code BIT_AND} or {@code BIT_OR})
	 * @param value          the bitwise operand value
	 * @param response       the expected result of the bitwise operation
	 * @return the SQL bitwise comparison string
	 */
	protected String getBitwiseFunction(Column column, boolean updateTableDef, Operator operator, Number value, Number response)
	{
		return "(" +getColumnIdentity(column, updateTableDef, false)+ (operator == Operator.BIT_AND ? " & " : " | ") + value+") = "+response;
	}

	/**
	 * Builds the SQL identity string for a column, optionally including table alias and column type.
	 * When {@code includeColumnType} is true and the column has a non-NORMAL type, delegates to
	 * {@link #getColumnString} to wrap the column with its aggregate/function syntax.
	 *
	 * @param column            the column to resolve
	 * @param includeTableAlias whether to prefix with the table alias
	 * @param includeColumnType whether to include the column type wrapper (e.g., COUNT, SUM)
	 * @return the SQL column identity string
	 */
	protected String getColumnIdentity(final Column column, final boolean includeTableAlias, final boolean includeColumnType)
	{
		if (includeColumnType && !column.getColumnType().isNormal()) {
			return getColumnString(column, includeTableAlias, false);
		}

		if(includeTableAlias)
		{
			final Table table = column.getTable();
			if (table.hasAlias())
			{
				return getTableName(table.getAlias()) + (".") + getColumnName(column);
			}
			else
			{
				return getTableName(table) + (".") + getColumnName(column);
			}
		}
		else
		{
			return getColumnName(column);
		}
	}

	private String getTableName(final Table table) {
		final TableDefinition<ColumnDefinition> tableDef = table.getTableDefinition();
		final String tableName = Objects.nonNull(tableDef) ? getTableName(tableDef) : getTableName(table.getName());

		return table.hasAlias() ? String.format("%s AS %s", tableName, getTableName(table.getAlias())) : tableName;
	}

	/**
	 * Resolves the SQL source expression for a table.
	 * For derived tables, generates a subquery or CTE reference.
	 * For regular tables, returns the quoted table name with optional alias.
	 * Subclasses can override to add dialect-specific modifiers (e.g., ClickHouse FINAL).
	 *
	 * @param table the table to resolve
	 * @return the SQL source expression for the table
	 */
	protected String getTableSource(final Table table) {
		final String tableName;

		if (table instanceof final DerivedTable derivedTable) {
			final String alias = derivedTable.getAlias();

			if (derivedTable.isWithClauseCTE()) {
				tableName = alias;
			} else {
				tableName = "( " + getSelectSQL(derivedTable.getSelectQuery()) + " ) AS " + getTableName(alias);
			}
		} else {
			tableName = getTableName(table);
		}

		return tableName;
	}

	/**
	 * Sets a value into a {@link PreparedStatement} at the given index,
	 * handling NULL values, BLOB types, and general object types.
	 *
	 * @param statement the prepared statement
	 * @param index     the parameter index (1-based)
	 * @param dataType  the data type of the column
	 * @param value     the value to set (may be {@code null})
	 * @throws SQLException if a database access error occurs
	 */
	protected void setInsidePreparedStatement(PreparedStatement statement, int index, DataType dataType, Object value) throws SQLException
	{
		final int sqlType = dataType.getSqlType();
		if(value == null)
		{
			statement.setNull(index, sqlType);
		}
		else if(sqlType == Types.BLOB)
		{
			if(value instanceof final byte[] bValue)
			{
				statement.setBytes(index, bValue);
			}
			else
			{
				statement.setBytes(index, value.toString().getBytes());
			}
		} else if (Types.ARRAY == sqlType) {
            handleArrayType(statement, index, dataType, value);
        }
		else
		{
			statement.setObject(index, dataType.getUnWrappedValue(value));
		}

	}

    /**
     * Handles setting array types in a PreparedStatement. Subclasses should override this method
     * to provide database-specific handling for array types, as the default implementation does nothing.
     *
     * @param statement the PreparedStatement to set the value on
     * @param index     the parameter index (1-based)
     * @param dataType  the DataType of the column, which should be an array type
     * @param value     the value to set, which may be a Java array or collection
     * @throws SQLException if an error occurs while setting the array value
     */
    protected void handleArrayType(PreparedStatement statement, int index, DataType dataType, Object value) throws SQLException {
    }

	/**
	 * Fills the SET clause of an UPDATE statement by appending {@code column=value} pairs.
	 *
	 * @param builder        the SQL string builder
	 * @param columnValueMap the map of columns to their values
	 */
	private void fillSetColumnValues(StringBuilder builder, Map<Column , Object> columnValueMap)
	{
		final Iterator<Entry<Column , Object>> entryItr =  columnValueMap.entrySet().iterator();
		int index = 1;
		final int size = columnValueMap.size();

		while(entryItr.hasNext())
		{
			final Entry<Column , Object> entry = entryItr.next();
			final Column column = entry.getKey();
			final Object value = entry.getValue();
			builder.append(getColumnName(column.getColumnName())).append("=");

			final DataType dataType = column.getColumnDefinition().getDataType();

			if(value == PREPARED_STMT_CONSTANT)
			{
				builder.append(PREPARED_STMT_CONSTANT);
			}
			else
			{
				builder.append(getColumnValue(dataType, value));
			}
			if(index++ < size)
			{
				builder.append(", ");
			}
		}
	}


	/**
	 * Converts a value to its SQL representation based on the column's data type.
	 * Returns {@code "NULL"} for null values, raw value for numerics, boolean string
	 * for booleans, and a formatted quoted string for all other types.
	 *
	 * @param dataType the data type of the column
	 * @param value    the value to convert (may be {@code null})
	 * @return the SQL representation of the value
	 */
	private Object getColumnValue(DataType dataType, Object value)
	{
		if(value == null)
		{
			return "NULL";
		}
		else
		{
			if(dataType.isNumeric())
			{
				return value;
			}
			else if(dataType.isBoolean())
			{
				return getBooleanString((Boolean) value);
			}
			else
			{
				return getFormattedString(value, dataType);
			}
		}
	}

	/**
	 * Fills the FROM clause of a SQL query, including the base table and any JOIN clauses.
	 * Supports INNER, LEFT, and RIGHT joins with multiple join relations.
	 *
	 * @param builder    the SQL string builder
	 * @param joinClause the list of join clauses, or {@code null} if no joins
	 */
	protected void fillFromClause(StringBuilder builder, List<JoinClause> joinClause)
	{
		if(joinClause != null)
		{
			for(JoinClause jClause : joinClause)
			{
				final JoinClause.JoinType joinType = jClause.getJoinType();
				if(joinType == JoinClause.JoinType.INNER)
				{
					builder.append(" INNER JOIN ");
				}
				else if(joinType == JoinClause.JoinType.LEFT)
				{
					builder.append(" LEFT JOIN ");
				}
				else
				{
					builder.append(" RIGHT JOIN ");
				}
				final Table remoteTable = jClause.getRemoteTable();
				builder.append(getTableSource(remoteTable)).append(" ON ");

				final StringJoiner stringJoiner = new StringJoiner(" AND ");
				final List<JoinClause.Relation> columns =  jClause.getRelations();
				for (JoinClause.Relation relation : columns)
				{
					final Column localColumn = relation.getLocalColumn();
					final Column remoteColumn = relation.getRemoteColumn();
					stringJoiner.add(getColumnIdentity(localColumn, true, true) + " = "+ getColumnIdentity(remoteColumn, true, false));
				}
				builder.append(stringJoiner);
				final WhereClause conditions = jClause.getConditions();

				if (Objects.nonNull(conditions)) {
					builder.append(getConnectorString(jClause.getInitialConnectorOfCondition()));
					fillWhereClause(builder, conditions, true);
				}
			}
		}
	}

	/**
	 * Fills the SELECT clause by appending comma-separated column SQL strings.
	 * Each column is rendered via {@link Column#toString(DMLHandler, boolean)}.
	 *
	 * @param builder       the SQL string builder
	 * @param selectClauses the sorted set of columns to include in the SELECT clause
	 */
	private void fillSelectClause(StringBuilder builder, SortedSet<SelectClause> selectClauses)
	{
		int index = 1;
		final int Size = selectClauses.size();

		for (final SelectClause selectClause : selectClauses) {

			if (selectClause.hasColumn()) {
				builder.append(selectClause.getColumn().toString(this, true));
			} else {
				fillStaticSelectClause(builder, selectClause.isEmpty() ? NumericalConstant.ONE : selectClause.getStaticValue());
			}

			if (index++ < Size) {
				builder.append(", ");
			}
		}
	}

	/**
	 * Fills the SELECT clause with static values when no regular columns are provided.
	 * Appends comma-separated static values (Strings and Numbers) to the SQL string builder.
	 * Supports both String and numeric values for static SELECT clauses.
	 *
	 * @param builder             the SQL string builder to append static values to
	 * @param staticSelectValue   a list of static values (String or Number objects) to include in the SELECT clause
	 */
	private void fillStaticSelectClause(final StringBuilder builder, final Object staticSelectValue) {

		if (staticSelectValue instanceof final String stringValue) {
			builder.append(stringValue);
		} else if (staticSelectValue instanceof final Number numberValue) {
			builder.append(numberValue.longValue());
		} else {
			throw ErrorCode.get(ErrorCodes.INVALID_PARAMETER,
					String.format("Unsupported static select clause value type: %s", Objects.isNull(staticSelectValue) ? "null" : staticSelectValue.getClass().getName()));
		}
	}

	/**
	 * Returns the SQL string representation of a column for use in a SELECT clause,
	 * including table alias and column alias.
	 *
	 * @param column the column to render
	 * @return the SQL string for the column in a SELECT context
	 */
	@Override
	public String getColumnString(final Column column, final boolean needColumnAlias) {
		return getColumnString(column, true, needColumnAlias);
	}

	/**
	 * Core method for converting a {@link Column} object into its corresponding SQL string representation.
	 *
	 * @param column the column object to process.
	 * @param includeTableAlias {@code true} to prefix table aliases (e.g., "123"); {@code false} to omit them.
	 * @return the generated SQL fragment.
	 */
	private String getColumnString(final Column column, final boolean includeTableAlias, final boolean needColumnAlias) {
		final StringBuilder columnBuilder = new StringBuilder(60);
		final Column.ColumnType type = column.getColumnType();

		switch (type) {
			case FUNCTION -> {
				final String functionString = Objects.requireNonNull(column.getFunction())
						.toString(functionColumn -> getColumnIdentity(functionColumn, includeTableAlias, false));

				columnBuilder.append(functionString);
			}
			case EXPRESSION -> {
				final String expressionString = Objects.requireNonNull(column.getExpression())
						.resolve(expressionColumn -> getColumnIdentity(expressionColumn, includeTableAlias, false));

				columnBuilder.append(expressionString);
			}
			case NORMAL -> columnBuilder.append(getColumnIdentity(column, includeTableAlias, false));
			default -> {
				final String columnIdentity = getColumnIdentity(column, includeTableAlias, false);

				columnBuilder.append(getColumnTypeString(type)).append("(");
				columnBuilder.append(columnIdentity).append(")");
			}
		}

		if (needColumnAlias && (column.hasAlias() || isFromDerivedTable(column)
				|| type == Column.ColumnType.FUNCTION || type == Column.ColumnType.EXPRESSION)) {
			columnBuilder.append(" AS ").append(getColumnName(column.getColumnAlias()));
		}

		return columnBuilder.toString();
	}


	/**
	 * Checks if the column originates from a DerivedTable (CTE or subquery).
	 * Columns from derived tables need explicit AS aliases so outer queries can reference them.
	 *
	 * @param column the column to check
	 * @return true if the column's table is a DerivedTable
	 */
	private boolean isFromDerivedTable(final Column column) {
		final Table table = column.getTable();
		return table instanceof DerivedTable;
	}

	/**
	 * Builds the SQL string representation of a {@link ConditionalColumn} for use in a SELECT clause.
	 *
	 * @param column the {@link ConditionalColumn} to convert into a SQL string
	 * @return the SQL string representation of the conditional column, ready to be placed in a SELECT clause
	 */
	public String getConditionalColumnString(final ConditionalColumn<?, ?> column, final boolean needColumnAlias)
	{
		final StringBuilder columnBuilder = new StringBuilder(60);
		final Column.ColumnType type =  column.getColumnType();

		if (type.isNormal()) {
			columnBuilder.append(getConditionalColumnIdentity(column, true));
		} else {
			columnBuilder.append(getColumnTypeString(type)).append("(")
					.append(getConditionalColumnIdentity(column, true)).append(")");
		}

		if(needColumnAlias && column.hasAlias())
		{
			columnBuilder.append(" AS ").append(getColumnName(column.getColumnAlias()));
		}
		return columnBuilder.toString();
	}

	/**
	 * Builds the SQL string representation of a {@link ConditionalColumn} (CASE expression).
	 * <p>
	 * Example output:
	 * <pre>
	 *   CASE
	 *      WHEN condition1 THEN 'result1'
	 *      WHEN condition2 THEN column_ref
	 *      ELSE 0
	 *   END
	 * </pre>
	 *
	 * @param conditionalColumn the {@link ConditionalColumn} containing WHEN...THEN and optional ELSE clauses
	 * @param updateTableDef    flag indicating whether table definitions/aliases should be updated
	 *                          when resolving column identities
	 * @return the SQL string representation of the CASE expression
	 */
	private String getConditionalColumnIdentity(final ConditionalColumn<?, ?> conditionalColumn, final boolean updateTableDef) {
		final StringBuilder builder = new StringBuilder("CASE ");
		final Object elseResult = conditionalColumn.getElseResult();

		for (final ConditionalColumn.WhenClause whenClause : conditionalColumn.getWhenClauses()) {
			builder.append("WHEN ");

			final StringBuilder conditionBuilder = new StringBuilder();

			fillWhereClause(conditionBuilder, whenClause.getWhereClause(), updateTableDef);
			builder.append(conditionBuilder).append(" THEN ").append(formatResult(whenClause.getResult(), updateTableDef))
					.append(" ");
		}

		if (Objects.nonNull(elseResult)) {
			builder.append("ELSE ").append(formatResult(elseResult, updateTableDef)).append(" ");
		}

		builder.append("END");

		return builder.toString();
	}

	/**
	 * Formats the given result object into a SQL-compatible string.
	 *
	 * @param result          the result object from a CASE expression (THEN/ELSE)
	 * @param updateTableDef  whether to update table definitions when resolving columns
	 * @return a SQL-ready string representation of the result
	 */
	private String formatResult(final Object result, final boolean updateTableDef) {
		if (result instanceof Column column) {
			return getColumnIdentity(column, updateTableDef, true);
		} else if (result instanceof String string) {
			return "'" + string.replace("'", "''") + "'";
		} else {
			return String.valueOf(result);
		}
	}

	/**
	 * Converts a given {@link Column.ColumnType} into its corresponding SQL function name.
	 * <p>
	 * This method is only used by {@link ConditionalColumn} rendering in
	 * {@link #getConditionalColumnString}, which wraps CASE expressions
	 * with aggregate functions (e.g., {@code SUM(CASE ... END)}).
	 * </p>
	 *
	 * @param columnType The column type to be converted (e.g., COUNT, MAX, etc.).
	 * @return The SQL function name as a string.
	 *
	 * <p>For unsupported types, an empty string is returned.</p>
	 */
	private String getColumnTypeString(final Column.ColumnType columnType) {
		return switch (columnType) {
			case COUNT -> "COUNT";
			case MAX -> "MAX";
			case MIN -> "MIN";
			case AVG -> "AVG";
			case SUM -> "SUM";
			default -> "";
		};
	}


	/**
	 * Fills the ORDER BY clause by appending comma-separated column references
	 * with sort direction and NULL ordering.
	 *
	 * @param builder     the SQL string builder
	 * @param sortColumns the collection of ORDER BY clauses
	 */
	private void fillOrderByClause(StringBuilder builder, Collection<OrderByClause> sortColumns)
	{
		final int size = sortColumns.size();
		int index = 1;
		for(OrderByClause orderBy : sortColumns )
		{
			final Column column = orderBy.getColumn();
			if(column.hasAlias())
			{
				final String columnAlias = column.getColumnAlias();
				if (!isColumnSelected(builder, columnAlias))
				{
					builder.append(column.toString(this, false));
				}
				else
				{
					builder.append(getColumnName(columnAlias));
				}
			}
			else
			{
				builder.append(column.toString(this, false));
			}
			if(orderBy.getType() == OrderByClause.OrderType.DESCENDING)
			{
				builder.append(" DESC");
			}
			final String nullSortOrder = getOrderByClauseNullStr(orderBy.getType(), orderBy.isNullFirst());
			if(nullSortOrder != null)
			{
				builder.append(nullSortOrder);
			}
			if(index < size)
			{
				builder.append(",");
			}
			index++;
		}
	}

	/**
	 * <p>
	 * Checks whether the column alias name presents in the select clause.
	 * </p>
	 *
	 * @param selectQueryBuilder  The SQL string builder
	 * @param columnAlias		  The column alias name
	 * @return true if the column is selected
	 */
	private boolean isColumnSelected(final StringBuilder selectQueryBuilder, final String columnAlias) {
		final int columnAliasIndex = selectQueryBuilder.indexOf(columnAlias);

        return columnAliasIndex > NumericalConstant.ZERO && columnAliasIndex < selectQueryBuilder.indexOf("FROM");
    }

	/**
	 * Fills the GROUP BY clause by appending comma-separated column references.
	 * Uses column alias if available, otherwise uses the full column identity.
	 *
	 * @param selectQueryBuilder the SQL string builder
	 * @param groupByClause      the list of GROUP BY clauses
	 */
	private void fillGroupByClause(StringBuilder selectQueryBuilder, List<GroupByClause> groupByClause)
	{
		selectQueryBuilder.append(" GROUP BY ");
		final int size = groupByClause.size();
		int index = 1;
		for (GroupByClause clause : groupByClause)
		{
			final Column column = clause.getColumn();
			if(column.hasAlias())
			{
				selectQueryBuilder.append(getColumnName(column.getColumnAlias()));
			}
			else
			{
				selectQueryBuilder.append(getColumnIdentity(column, true, false));
			}
			if(index++ < size)
			{
				selectQueryBuilder.append(", ");
			}
		}
	}

	/**
	 * Fills the {@link HavingClause} of a SQL query based on the provided criteria.
	 *
	 * @param builder         the {@link StringBuilder} object used to construct the SQL query
	 * @param criteriaBuilder the {@link HavingClause} object containing the criteria for the HAVING clause
	 */
	private void fillHavingClause(final StringBuilder builder, final HavingClause criteriaBuilder) {
		if (criteriaBuilder.isInverted()) {
			throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
		}

		if (criteriaBuilder.isConnector()) {
			final HavingClause left = criteriaBuilder.getLeft();
			final HavingClause right = criteriaBuilder.getRight();

			builder.append(" ( ");
			fillHavingClause(builder, left);
			builder.append(getConnectorString(criteriaBuilder.getConnector()));
			fillHavingClause(builder, right);
			builder.append(" ) ");
		} else {
			final Criteria sqlCriteria = criteriaBuilder.getCriteria();
			final Column column = sqlCriteria.getColumn();
			final Object value = sqlCriteria.getValue();
			final Operator operator = sqlCriteria.getOperator();

			if (column instanceof final ConditionalColumn<?, ?> conditionalColumn) {
				final Column.ColumnType type = column.getColumnType();

				if (type.isNormal()) {
					builder.append(getConditionalColumnIdentity(conditionalColumn, true));
				} else {
					builder.append(getColumnTypeString(type)).append("(")
							.append(getConditionalColumnIdentity(conditionalColumn, true)).append(")");
				}
			} else {
				builder.append(getColumnString(column, true, false));
			}

			builder.append(getComparator(operator));

			if (value instanceof final SelectQuery selectQuery) {
				builder.append("(").append(getSelectSQL(selectQuery)).append(")");
			} else if (value instanceof Number) {
				builder.append(value);
			} else {
				throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
			}
		}
	}

	/**
	 * Fills the WHERE clause recursively, handling nested AND/OR connectors,
	 * NULL checks, prepared statement placeholders, subqueries, column-to-column
	 * comparisons, and data-type-specific comparator loading.
	 *
	 * @param builder        the SQL string builder
	 * @param criteriaBuilder the WHERE clause tree to process
	 * @param updateTableDef whether to include table aliases in column references
	 */
	protected void fillWhereClause(StringBuilder builder, WhereClause criteriaBuilder, boolean updateTableDef)
	{
		if(criteriaBuilder.isInverted())
		{
			builder.append(" NOT");
		}

		if(criteriaBuilder.isConnector())
		{
			final WhereClause left = criteriaBuilder.getLeft();
			final WhereClause right = criteriaBuilder.getRight();
			builder.append(" (");
			fillWhereClause(builder, left, updateTableDef);
			builder.append(getConnectorString(criteriaBuilder.getConnector()));
			fillWhereClause(builder, right, updateTableDef);
			builder.append(" )");

		}
		else
		{
			final Criteria sqlCriteria = criteriaBuilder.getCriteria();
			final Object value = sqlCriteria.getValue();
			final Operator operator = sqlCriteria.getOperator();

			if (operator == Operator.EXISTS) {
				if (!(value instanceof SelectQuery selectQuery)) {
					throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
				}
				builder.append(" (");
				builder.append("EXISTS (").append(getSelectSQL(selectQuery)).append(")");
				builder.append(" )");
				return;
			}
			final Column column = sqlCriteria.getColumn();

			builder.append(" (");

			if (column instanceof JsonColumn) {
				loadJsonComparatorValue(column, value, builder, operator, updateTableDef);
			}
			else if(value == null)
			{
				if (column.getColumnDefinition().getDataType().isArray()) {
					loadComparatorByDataType(column, value, builder, operator, updateTableDef);
				} else {
					builder.append(getColumnIdentity(column, updateTableDef, true)).append(getNullComparator(operator));
				}
			}
			else if(value == PREPARED_STMT_CONSTANT && !Operator.isPatternMatchable(operator))
			{
				builder.append(getColumnIdentity(column, updateTableDef, true)).append(getComparator(operator)).append(PREPARED_STMT_CONSTANT);
			}
			else if(value instanceof final SelectQuery selectQuery)
			{
				builder.append(getColumnIdentity(column, updateTableDef, true)).append(getComparator(operator)).append("(").append(getSelectSQL(selectQuery)).append(")");
			}
			else if (value instanceof final Column criteriaColumn)
			{
				builder.append(getColumnIdentity(column, updateTableDef, true)).append(getComparator(operator)).append("(").append(getColumnIdentity(criteriaColumn, updateTableDef, false)).append(")");
			}
			else
			{
				loadComparatorByDataType(column, value, builder, operator, updateTableDef);
			}
			builder.append(" )");
        }
	}

	/**
	 * Dispatches comparator value loading based on the column's data type.
	 * Default implementation handles standard OLTP types: filterable-string, numeric, boolean, and string.
	 * Subclasses can override to add dialect-specific type handling (e.g., JSON, Array, BuiltInFunction types).
	 *
	 * @param column         the column being compared
	 * @param value          the comparator value
	 * @param builder        the SQL string builder
	 * @param operator       the comparison operator
	 * @param updateTableDef whether to include table aliases
	 */
	protected void loadComparatorByDataType(final Column column, final Object value, final StringBuilder builder, final Operator operator, final boolean updateTableDef) {
		final DataType dataType = column.getColumnDefinition().getDataType();

		if (dataType.isFilterableString() || dataType.isString()) {
			loadStringComparatorValue(column, value, builder, operator, updateTableDef);
		} else if (dataType.isNumeric()) {
			loadNumberComparatorValue(column, value, builder, operator, updateTableDef);
		} else if (dataType.isBoolean()) {
			loadBooleanComparator(column, value, builder, operator, updateTableDef);
		} else {
			throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
		}
	}

	/**
	 * Loads a comparator value for a {@link JsonColumn} whose comparator semantics are
	 * driven by its extracted return type rather than the column's nominal data type.
	 *
	 * <p>Default implementation throws unsupported. Subclasses that support JSON columns
	 * (e.g., {@code CHDMLHandler}) must override this to provide the actual SQL generation.</p>
	 *
	 * @param column         the JSON column being compared
	 * @param value          the comparator value
	 * @param builder        the SQL string builder
	 * @param operator       the comparison operator
	 * @param updateTableDef whether to include table aliases
	 */
	protected void loadJsonComparatorValue(final Column column, final Object value, final StringBuilder builder,
	                                       final Operator operator, final boolean updateTableDef) {
		throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
	}


	/**
	 * Loads a boolean comparator value into the SQL builder.
	 * Only supports EQUAL and NOT_EQUAL operators.
	 *
	 * @param column         the column being compared
	 * @param value          the comparator value (must be a Boolean)
	 * @param builder        the SQL string builder
	 * @param operator       the comparison operator
	 * @param updateTableDef whether to include table aliases
	 */
	private void loadBooleanComparator(final Column column, final Object value, final StringBuilder builder, Operator operator, boolean updateTableDef)
	{
		if(value instanceof final Boolean bool)
		{
			if(operator == Operator.EQUAL || operator == Operator.NOT_EQUAL)
			{
				builder.append(getColumnIdentity(column, updateTableDef, true)).append(getComparator(operator)).append(getBooleanString(bool));
			}
			else
			{
				throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
			}
		}
		else
		{
			throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
		}
	}

	/**
	 * Loads a string-based comparator value into the SQL builder.
	 * Handles single values, collections (IN/NOT IN), and pattern-matching operators
	 * (CONTAINS, STARTS_WITH, ENDS_WITH) with proper escaping.
	 *
	 * @param column         the column being compared
	 * @param value          the comparator value (String or Iterable)
	 * @param builder        the SQL string builder
	 * @param operator       the comparison operator
	 * @param updateTableDef whether to include table aliases
	 */
	protected void loadStringComparatorValue(final Column column, final Object value, final StringBuilder builder, final Operator operator, boolean updateTableDef)
	{
		builder.append(getColumnIdentity(column, updateTableDef, true));
		final DataType dataType = column.getColumnDefinition().getDataType();
		if(value instanceof final Iterable<?> collection)
		{
			final Iterator<?> itr = collection.iterator();
			if(!itr.hasNext())
			{
				final String arrayType = getSqlArrayType(dataType);

				if (operator == Operator.IN) {
					builder.append(" = ANY(ARRAY[]::").append(arrayType).append("[])");
					return;
				} else if (operator == Operator.NOT_IN) {
					builder.append(" <> ALL(ARRAY[]::").append(arrayType).append("[])");
					return;
				} else {
					throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
				}
			}
			if(operator == Operator.IN || operator == Operator.NOT_IN)
			{
				builder.append(getComparator(operator));

				if(Objects.equals(CommonDataType.ARRAY, dataType))
				{
					builder.append(" [ ");
				} else {
					builder.append(" ( ");
				}

				do
				{
					final Object cValue = itr.next();
					final Object converted = dataType.getUnWrappedValue(cValue);
					if(!(converted instanceof String))
					{
						throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
					}
					builder.append(getFormattedString(converted, dataType));
					if(itr.hasNext())
					{
						builder.append(",");
					}
					else
					{
						break;
					}
				} while (true);

				if(Objects.equals(CommonDataType.ARRAY, dataType)) {
					builder.append(" ] ");
				} else {
					builder.append(" ) ");
				}
			}
			else
			{
				throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
			}
		}
		else
		{
			final Object unWrappedValue = dataType.getUnWrappedValue(value);

			if(!(unWrappedValue instanceof String))
			{
				throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
			}
			final String valueAsString = unWrappedValue.toString();
			final boolean isPatternMatchable = Operator.isPatternMatchable(operator);
			final boolean isEscapeRequired = Pattern.compile(ESCAPE_REQUIRED_CHAR).matcher(valueAsString).find();
			final String converted = isPatternMatchable ? wrapSpecialCharValue(valueAsString) : valueAsString;

			if(converted.equals(PREPARED_STMT_CONSTANT) && !isPatternMatchable)
			{
				builder.append(getComparator(operator)).append(SINGLE_QUOTE).append(converted).append(SINGLE_QUOTE);
			}
			else if(operator == Operator.CONTAINS || operator == Operator.NOT_CONTAINS)
			{
				appendWithLowerFunction(builder, operator, "%" + converted + "%", column.getColumnDefinition().getDataType());
			}
			else if(operator == Operator.STARTS_WITH || operator == Operator.NOT_STARTS_WITH)
			{
				appendWithLowerFunction(builder, operator, converted + "%", column.getColumnDefinition().getDataType());
			}
			else if(operator == Operator.EQUAL || operator == Operator.NOT_EQUAL)
			{
				builder.append(getComparator(operator)).append(getFormattedString(converted, column.getColumnDefinition().getDataType()));
			}
			else if(operator == Operator.ENDS_WITH || operator == Operator.NOT_ENDS_WITH)
			{
				appendWithLowerFunction(builder, operator, "%" + converted, column.getColumnDefinition().getDataType());
			}
			else
			{
				throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
			}

			if (isPatternMatchable && SPECIAL_CHAR_PATTERN.matcher(converted).find() && isEscapeRequired) {
				builder.append(ESCAPE).append(SINGLE_QUOTE).append(FORWARD_SLASH).append(SINGLE_QUOTE);
			}
		}
	}

	/**
	 * <p>
	 * Wrap the value if the special character is found.
	 * </p>
	 *
	 * @param value Represents the value.
	 * @return Wrapped value.
	 */
	private String wrapSpecialCharValue(final String value) {
		final Matcher specialCharMatcher = SPECIAL_CHAR_PATTERN.matcher(value);
		final boolean isEscapeRequired = Pattern.compile(ESCAPE_REQUIRED_CHAR).matcher(value).find();
		final StringBuilder valueBuilder = new StringBuilder();

		while (specialCharMatcher.find()) {
			specialCharMatcher.appendReplacement(valueBuilder, isEscapeRequired ?
					Matcher.quoteReplacement(String.join("", FORWARD_SLASH, specialCharMatcher.group())) : Matcher.quoteReplacement(specialCharMatcher.group()));
		}
		specialCharMatcher.appendTail(valueBuilder);
		return valueBuilder.toString();
	}

	/**
	 * Loads a numeric comparator value into the SQL builder.
	 * Handles single numbers, collections (IN/NOT IN), ranges (BETWEEN),
	 * bitwise operations, and {@link SqlFunction}-based comparisons.
	 *
	 * @param column         the column being compared
	 * @param value          the comparator value (Number, Iterable, Number[], or SqlFunction)
	 * @param builder        the SQL string builder
	 * @param operator       the comparison operator
	 * @param updateTableDef whether to include table aliases
	 */
	protected void loadNumberComparatorValue(final Column column, final Object value, final StringBuilder builder, final Operator operator, final boolean updateTableDef)
	{
		if (value instanceof final Iterable<?> collection)
		{
			final Iterator<?> itr = collection.iterator();
			if(!itr.hasNext())
			{
				final DataType dataType = column.getColumnDefinition().getDataType();
				final String arrayType = getSqlArrayType(dataType);

				if (operator == Operator.IN) {
					builder.append(getColumnIdentity(column, updateTableDef, true)).append(" = ANY(ARRAY[]::").append(arrayType).append("[])");
					return;
				} else if (operator == Operator.NOT_IN) {
					builder.append(getColumnIdentity(column, updateTableDef, true)).append(" <> ALL(ARRAY[]::").append(arrayType).append("[])");
					return;
				} else {
					throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
				}
			}
			if(operator == Operator.IN || operator == Operator.NOT_IN)
			{
				builder.append(getColumnIdentity(column, updateTableDef, true)).append(getComparator(operator));
				builder.append(" ( ");
				do
				{
					final Object cValue = itr.next();
					if(!(cValue instanceof Number))
					{
						throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
					}
					builder.append(cValue);
					if(itr.hasNext())
					{
						builder.append(",");
					}
					else
					{
						break;
					}
				}while (true);
				builder.append(" ) ");
			}
			else
			{
				throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
			}
		}
		else if(value instanceof final Number[] pair)
		{
			if(operator == Operator.BETWEEN || operator == Operator.NOT_BETWEEN)
			{
				builder.append(getColumnIdentity(column, updateTableDef, true)).append(getComparator(operator)).append(getBetweenValueNumber(pair[0], pair[1]));
			}
			else if(operator == Operator.BIT_AND || operator == Operator.BIT_OR)
			{
				builder.append(getBitwiseFunction(column, updateTableDef, operator, pair[0], pair[1]));
			}
			else
			{
				throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
			}
		}
		else if(value instanceof Number)
		{
			if((operator == Operator.EQUAL) || (operator == Operator.NOT_EQUAL) || (operator == Operator.GREATER_EQUAL) ||
					(operator == Operator.GREATER_THAN) || (operator == Operator.LESS_THAN) || (operator == Operator.LESS_EQUAL))
			{
				builder.append(getColumnIdentity(column, updateTableDef, true)).append(getComparator(operator)).append(value);
			}
			else
			{
				throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
			}
		}
		else if (value instanceof final SqlFunction sqlFunction)
		{
			if ((operator == Operator.GREATER_THAN) || (operator == Operator.GREATER_EQUAL) ||
					(operator == Operator.LESS_THAN) || (operator == Operator.LESS_EQUAL))
			{
				builder.append(getColumnIdentity(column, updateTableDef, true)).append(getComparator(operator)).append(sqlFunction.toString(Column::getColumnName));
			}
			else
			{
				throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
			}
		}
		else if (value instanceof final SqlFunction[] sqlFunctions)
		{
			if (operator == Operator.BETWEEN)
			{
				builder.append(getColumnIdentity(column, updateTableDef, true)).append(getComparator(operator)).append(" ")
						.append(sqlFunctions[0].toString(Column::getColumnName)).append(" AND ").append(sqlFunctions[1].toString(Column::getColumnName));
			}
			else
			{
				throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
			}
		}
		else
		{
			throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
		}
	}

	/**
	 * Fetches the SQL connector string for the provided connector type.
	 *
	 * @param connector the {@link Connector} type to retrieve the SQL connector string
	 * @return the SQL connector string for the provided connector type
	 */
	private String getConnectorString(final Connector connector) {
		return connector == Connector.AND ? " AND " : " OR ";
	}

	/**
	 * Returns the SQL comparator string for the given {@link Operator}.
	 *
	 * @param operator the comparison operator
	 * @return the SQL comparator string (e.g., {@code " = "}, {@code " LIKE "})
	 * @throws com.tlc.commons.code.impl.DefaultErrorCode if the operator is unsupported
	 */
	protected String getComparator(final Operator operator)
	{
		return switch (operator) {
			case EQUAL -> " = ";
			case NOT_EQUAL -> " != ";
			case CONTAINS, STARTS_WITH, ENDS_WITH -> " LIKE ";
			case NOT_CONTAINS, NOT_STARTS_WITH, NOT_ENDS_WITH -> " NOT LIKE ";
			case GREATER_EQUAL -> " >= ";
			case GREATER_THAN -> " > ";
			case LESS_EQUAL -> " <= ";
			case LESS_THAN -> " < ";
			case IN -> " IN ";
			case NOT_IN -> " NOT IN ";
			case BETWEEN -> " BETWEEN ";
			case NOT_BETWEEN -> " NOT BETWEEN ";
			default -> throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
		};
	}

	/**
	 * Formats the BETWEEN range as a SQL string ({@code "start AND end"}).
	 *
	 * @param start the lower bound of the range
	 * @param end   the upper bound of the range
	 * @return the formatted BETWEEN value string
	 */
	protected String getBetweenValueNumber(Number start, Number end)
	{
		return start + " AND " + end;
	}

	/**
	 * Returns the SQL null comparator string for the given {@link Operator}.
	 * Maps {@code EQUAL} to {@code " IS NULL"} and {@code NOT_EQUAL} to {@code " IS NOT NULL"}.
	 *
	 * @param operator the comparison operator (must be EQUAL or NOT_EQUAL)
	 * @return the SQL null comparator string
	 * @throws com.tlc.commons.code.impl.DefaultErrorCode if the operator is not EQUAL or NOT_EQUAL
	 */
	private String getNullComparator(Operator operator)
	{
		if(operator == Operator.EQUAL)
		{
			return " IS NULL";
		}
		else if(operator == Operator.NOT_EQUAL)
		{
			return " IS NOT NULL";
		}
		else
		{
			throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
		}
	}

	/**
	 * Resolves the dialect-level SQL array element type name for the given {@link DataType}.
	 *
	 * @param dataType the column's data type
	 * @return the SQL element-type name to use inside an {@code ARRAY[]::TYPE[]} cast
	 */
	protected String getSqlArrayType(final DataType dataType) {
		if (dataType instanceof final CommonDataType commonDataType) {
			return switch (commonDataType) {
				case SMALLINT -> "SMALLINT";
				case INTEGER -> "INTEGER";
				case BIGINT -> "BIGINT";
				case DECIMAL -> "DECIMAL";
				case TEXT -> "TEXT";
				case BOOLEAN -> "BOOLEAN";
				default -> throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
			};
		}
		throw ErrorCode.get(SQLErrorCodes.DB_QUERY_INVALID_COMPARATOR);
	}

	/**
	 * Wraps the given formatted value string with a case-insensitive function if required by the dialect.
	 *
	 * @param value    the formatted value string to wrap
	 * @param dataType the data type of the column
	 * @return the value wrapped with a case-insensitive function, or the value as-is
	 */
	protected abstract String wrapWithLower(final String value, final DataType dataType);

	/**
	 * Appends a comparator operator and a case-insensitive wrapped value to the SQL builder.
	 * Delegates to {@link #wrapWithLower(String, DataType)} for dialect-specific lowercasing.
	 *
	 * @param builder  the SQL string builder to append to
	 * @param operator the comparison operator (e.g., LIKE, NOT LIKE)
	 * @param value    the raw pattern value (e.g., {@code "%search%"})
	 * @param dataType the data type of the column being compared
	 */
	private void appendWithLowerFunction(final StringBuilder builder, final Operator operator, final String value, final DataType dataType) {
		builder.append(getComparator(operator));
		builder.append(wrapWithLower(getFormattedString(value, dataType), dataType));
	}
}