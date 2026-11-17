package com.tlc.sql.common.api;

import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.internal.data.DataContainerImpl;

import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.stream.Stream;


/**
 * @author Abishek
 * @version 1.0
 */
public interface DataContainer
{

	static DataContainer create()
	{
		return new DataContainerImpl();
	}

	boolean isEmpty();

	int size(Table table);

	void append(DataContainer dataContainer);

	void appendIntensively(final DataContainer dataContainer);

	Stream<Row> getRows(Table table);

	Set<Table> getTables();

	Map<Long, Row> getRowsMap(Table table);

	Row getRow(Table table);

	void updateRow(Row row);

	void storeRow(Row row);

	void addNewRow(Row row);

	void removeNewRow(Row row);

	void indexRows(Column column);

	Row getIndexedRow(Column column, Object value);

	Stream<Row> getIndexedRows(Column column, Object value);

	Stream<Row> getRows(Table table, WhereClause whereClause);

	Row getRow(Table table, Long id);

	Row getRow(Table table, WhereClause whereClause);

	void deleteRow(Row row);
	
	NavigableMap<Table, NavigableSet<Long>> getModifiedRows(Operation operation);

	Set<Long> clearActionRows(Operation operation, Table table); // handle with care

	void clearActionRows(Operation operation);  // handle with care

	/**
	 * <p>
	 * Gets the existing row if present, otherwise returns a new row
	 * </p>
	 * <p>
	 * <strong>Use with caution:</strong> Calling this method inside a loop may result in creation of multiple unintended new rows
	 * </p>
	 *
	 * @param table Represents the {@link Table}
	 * @param id    Represents the pk value
	 * @return the existing or new row
	 */
	Row getOrInitRow(final Table table, final Long id);

	/**
	 * <p>
	 * Gets the existing row if present, otherwise returns a new row
	 * </p>
	 * <p>
	 * <strong>Use with caution:</strong> Calling this method inside a loop may result in creation of multiple unintended new rows
	 * </p>
	 *
	 * @param table       Represents the {@link Table}
	 * @param whereClause Represents the where clause to filter the data
	 * @return the existing or new row
	 */
	Row getOrInitRow(final Table table, final WhereClause whereClause);

	/**
	 * <p>
	 * Removes the rows of the given table from the data container
	 * </p>
	 *
	 * @param table Represents the {@link Table} whose rows needs to be removed from the data container
	 */
	void removeTableRows(final Table table);

	/**
	 * Removes the specified columns from all rows of the given table in the data container.
	 *
	 * @param table   The {@link Table} whose column values should be removed.
	 * @param columns The {@link Collection} of {@link Column} instances to remove from each row.
	 */
	void removeTableColumns(final Table table, final Collection<Column> columns);
}