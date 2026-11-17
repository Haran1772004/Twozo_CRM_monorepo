package com.tlc.sql.common.api;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.ImmutableColumns;
import com.tlc.sql.common.api.meta.PKDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * @author Abishek
 * @version 1.0
 */
public class Row
{
	private final Table table;

	private final Map<String, Object> columnValues;
	private final Map<String, DataType> columnDataTypeMap;
	private final Set<String> modifiedColumns;

	private final Set<String> editableColumns;
	private final Set<String> allColumns;

	private final String pkColumn;
	private final boolean isNewRow;

	public Row(Table table) // New Row
	{
		this(table, true);
		table.getTableDefinition().getEditableColumns().forEach( (column, def) ->
		{
			final Object defaultValue = def.getDefaultValue();
			if(defaultValue != null)
			{
				this.setWithoutCheck(column, defaultValue);
			}
		});
	}

	public Row(Table table, Long pk) // Existing Row
	{
		this(table, false);
		this.setWithoutCheck(pkColumn, pk);
	}

	private Row(Table table, boolean isNewRow)
	{
		this.table = Objects.requireNonNull(table);
		this.columnValues = new HashMap<>();
		this.columnDataTypeMap = new HashMap<>();

		final TableDefinition<?> tableDef = table.getTableDefinition();
		final PKDefinition pkDef = tableDef.getPkDefinition();
		this.pkColumn = pkDef.getColumnName();

		this.allColumns = tableDef.getColumnDefinition().keySet();
		this.editableColumns = tableDef.getEditableColumns().keySet();
		this.loadColumnDataTypeMap(tableDef);

		if(isNewRow)
		{
			this.isNewRow = true;
			this.modifiedColumns = new HashSet<>();
			if(pkDef.hasSequenceGenerator())
			{
				final long sequence = pkDef.getNextSequence();

				this.setWithoutCheck(pkColumn, sequence);
			}
		}
		else
		{
			this.modifiedColumns = new HashSet<>();
			this.isNewRow = false;
		}
	}

	public Row(Row row)
	{
		this.table = row.table;

		this.editableColumns = row.editableColumns;
		this.allColumns = row.allColumns;

		this.pkColumn = row.pkColumn;
		this.isNewRow = row.isNewRow;

		this.columnValues = new HashMap<>(row.columnValues);
		this.columnDataTypeMap = new HashMap<>(row.columnDataTypeMap);
		this.modifiedColumns = new HashSet<>(row.modifiedColumns);
	}

	public boolean isNewRow()
	{
		return isNewRow;
	}

	public <T> T get(Column column)
	{
		return get(column.getColumnAlias());
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String column)
	{
		return (T) columnValues.get(column);
	}

	public void setOrgId(Long orgId)
	{
		if(table.getTableDefinition().getType().isOrgDependent() && isNewRow)
		{
			setWithoutCheck(ImmutableColumns.ORG_ID.getName(), orgId);
		}
		else
		{
			throw ErrorCode.get(SQLErrorCodes.DB_ROW_INVALID_COLUMN, ImmutableColumns.ORG_ID.getName());
		}
	}

	public Long getOrgId()
	{
		if(table.getTableDefinition().getType().isOrgDependent())
		{
			return get(ImmutableColumns.ORG_ID.getName());
		}
		else
		{
			throw ErrorCode.get(SQLErrorCodes.DB_ROW_INVALID_COLUMN, ImmutableColumns.ORG_ID.getName());
		}
	}

	public void set(Column column, Object value)
	{
		if(column.hasAlias())
		{
			throw ErrorCode.get(SQLErrorCodes.DB_ROW_INVALID_COLUMN, "Alias cannot be used");
		}
		final String columnName = column.getColumnName();
		set(columnName, value);
	}

	public void set(String column, Object value)
	{
		if(!editableColumns.contains(column))
		{
			throw ErrorCode.get(SQLErrorCodes.DB_ROW_INVALID_COLUMN, column);
		}

		if(!isNewRow && !Objects.equals(columnValues.get(column), value))
		{
			this.modifiedColumns.add(column);
		}
		this.columnValues.put(column, columnDataTypeMap.get(column).getWrappedValue(value));
	}

    /**
     * <p>
     * Sets the value if it's modified
     * </p>
     *
     * @param column Represents the column name
     * @param value  Represents the column value
     */
    public void setIfModified(final String column, final Object value) {

        if (!Objects.equals(value, columnValues.get(column))) {
            set(column, value);
        }
    }

	public void setWithoutCheck(Column column, Object value)
	{
		final String columnAlias = column.getColumnAlias();

		columnDataTypeMap.put(columnAlias, column.getColumnDefinition().getDataType());
		setWithoutCheck(columnAlias, value);
	}

	public void setWithoutCheck(String column, Object value)
	{
		final DataType dataType = columnDataTypeMap.get(column);

		if (Objects.isNull(dataType)) {
			throw ErrorCode.get(SQLErrorCodes.DB_ROW_INVALID_COLUMN);
		}
		this.columnValues.put(column, dataType.getWrappedValue(value));
	}

	public Long getPKValue()
	{
		return get(pkColumn);
	}

	public Table getTable()
	{
		return table;
	}

	public boolean isCompleteRow()
	{
		return columnValues.size() == allColumns.size();
	}

	public void forEach(BiConsumer<String, Object> biConsumer)
	{
		columnValues.forEach(biConsumer);
	}

	public void merge(Row row)
	{
		if(row.getTable().equals(this.getTable()))
		{
			columnDataTypeMap.putAll(row.columnDataTypeMap);
			columnValues.putAll(row.columnValues);
		}
		else
		{
			throw ErrorCode.get(SQLErrorCodes.DB_ROW_UNKNOWN_TABLE, row.table.getName());
		}
	}

	public Set<String> getModifiedColumns()
	{
		return Collections.unmodifiableSet(modifiedColumns);
	}

	@Override
	public String toString()
	{
		return columnValues.toString();
	}

	/**
	 * Gets the column value map.
	 *
	 * @return The column value map.
	 */
	public Map<String, Object> getColumnValues() {
		return Collections.unmodifiableMap(columnValues);
	}

	/**
	 * Removes the given column's value
	 *
	 * @param columnName Represents the column name the value to be removed
	 */
	public void remove(final String columnName) {
		columnValues.remove(columnName);
	}

	/**
	 * Removes the given column's value
	 *
	 * @param column Represents the column the value to be removed
	 */
	public void remove(final Column column) {
		remove(column.getColumnName());
	}

	/**
	 * <p>
	 * Populates the column data type map.
	 * </p>
	 *
	 * @param tableDefinition {@link TableDefinition}.
	 */
	private void loadColumnDataTypeMap(final TableDefinition<?> tableDefinition) {

		if (columnDataTypeMap.isEmpty()) {
			tableDefinition.getColumns().forEach(column -> columnDataTypeMap.put(column, tableDefinition.getColumnDefinition(column).getDataType()));
		}
	}

    /**
     * Returns {@code true} if this row contains no column values.
     *
     * @return {@code true} if no column values are present, {@code false} otherwise.
     */
    public boolean hasNoData() {
        return columnValues.isEmpty();
    }
}
