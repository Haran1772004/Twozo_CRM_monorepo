package com.tlc.sql.common.api.ds;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.ColumnReceiver;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.ExistsQuery;
import com.tlc.sql.common.api.dml.RowReceiver;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.dml.With;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

/**
 * @author Abishek
 * @version 1.0
 */

public interface ReadableDataStore
{
	long get(CountQuery query);

	Row get(Table table, Long id);

	/**
	 * <p>
	 * Fetches the {@link Row} for the given primary key value. The row includes only the specified columns.
	 * </p>
	 *
	 * @param table   Holds the metadata of the table.
	 * @param id      Represents the primary key value of the given table.
	 * @param columns Holds the columns to be selected.
	 * @return {@link Row}.
	 */
	Row get(final Table table, final Long id, final Collection<Column> columns);

	DataContainer get(Table table, Collection<Long> ids);

	/**
	 * <p>
	 * Fetches the {@link DataContainer} for the given primary key values. Those row includes only the specified columns.
	 * </p>
	 *
	 * @param table   Holds the metadata of the table.
	 * @param ids     Represents the primary key values of the given table.
	 * @param columns Holds the columns to be selected.
	 * @return {@link DataContainer}.
	 */
	DataContainer get(final Table table, final Collection<Long> ids, final Collection<Column> columns);

	DataContainer get(Table table, WhereClause whereClause);

	DataContainer get(Table table, WhereClause whereClause, Collection<Column> columns);

	DataContainer get(SelectQuery query);

	DataContainer get(With with);

	DataContainer get(Collection<String> tables, WhereClause whereClause);

	DataContainer get(Collection<String> tables, WhereClause whereClause, Collection<Column> columns);

	DataContainer get(SortedSet<Table> tables, WhereClause whereClause);

	DataContainer get(SortedSet<Table> tables, WhereClause whereClause, Collection<Column> columns);

	List<Map<String, Object>> getData(SelectQuery query);

	List<Map<String, Object>> getData(With with);

	void fetchData(Table table, WhereClause whereClause, RowReceiver rowReceiver);

	void fetchData(Column column, WhereClause whereClause, ColumnReceiver columnReceiver);

	/**
	 * <p>
	 * Retrieves the data corresponding to the provided {@link SelectQuery} and forwards this data to the {@link RowReceiver} for further processing.
	 * </p>
	 *
	 * @param selectQuery Represents the {@link SelectQuery}.
	 * @param rowReceiver Represents the {@link RowReceiver}.
	 */
	void fetchData(final SelectQuery selectQuery, final RowReceiver rowReceiver);

	/**
	 * <p>
	 * Retrieves the data corresponding to the provided {@link SelectQuery} and forwards this data to the {@link ColumnReceiver} for further processing.
	 * </p>
	 *
	 * @param selectQuery    Represents the {@link SelectQuery}.
	 * @param columnReceiver Represents the {@link ColumnReceiver}.
	 */
	void fetchData(final SelectQuery selectQuery, final ColumnReceiver columnReceiver);

	/**
	 * <p>
	 * Retrieves the data corresponding to the provided {@link SelectQuery} and forwards this data to the {@link RowReceiver} for further processing.
	 * </p>
	 *
	 * @param selectQuery  Represents the {@link SelectQuery}.
	 * @param rowReceivers Represents the map of {@link Table} and {@link RowReceiver}.
	 */
	void fetchDataWithRowReceivers(final SelectQuery selectQuery, final Map<Table, RowReceiver> rowReceivers);

	/**
	 * <p>
	 * Retrieves the data corresponding to the provided {@link SelectQuery} and forwards this data to the {@link ColumnReceiver} for further processing.
	 * </p>
	 *
	 * @param selectQuery     Represents the {@link SelectQuery}.
	 * @param columnReceivers Represents the map of {@link Column} and {@link ColumnReceiver}.
	 */
	void fetchDataWithColumnReceivers(final SelectQuery selectQuery, final Map<Column, ColumnReceiver> columnReceivers);

	/**
	 * <p>
	 * Retrieves the data corresponding to the provided {@link With} and forwards this data to the {@link RowReceiver} for further processing.
	 * </p>
	 *
	 * @param with Represents the {@link With}.
	 * @param rowReceiver Represents the {@link RowReceiver}.
	 */
	void fetchData(final With with, final RowReceiver rowReceiver);

	/**
	 * <p>
	 * Retrieves the data corresponding to the provided {@link With} and forwards this data to the {@link ColumnReceiver} for further processing.
	 * </p>
	 *
	 * @param with    Represents the {@link With}.
	 * @param columnReceiver Represents the {@link ColumnReceiver}.
	 */
	void fetchData(final With with, final ColumnReceiver columnReceiver);

	/**
	 * <p>
	 * Retrieves the data corresponding to the provided {@link With} and forwards this data to the {@link RowReceiver} for further processing.
	 * </p>
	 *
	 * @param with  Represents the {@link With}.
	 * @param rowReceivers Represents the map of {@link Table} and {@link RowReceiver}.
	 */
	void fetchDataWithRowReceivers(final With with, final Map<Table, RowReceiver> rowReceivers);

	/**
	 * <p>
	 * Retrieves the data corresponding to the provided {@link With} and forwards this data to the {@link ColumnReceiver} for further processing.
	 * </p>
	 *
	 * @param with     Represents the {@link With}.
	 * @param columnReceivers Represents the map of {@link Column} and {@link ColumnReceiver}.
	 */
	void fetchDataWithColumnReceivers(final With with, final Map<Column, ColumnReceiver> columnReceivers);

	/**
	 * Checks if the row with the given primary key exists in the given table.
	 *
	 * @param table Holds the metadata of the table.
	 * @param id    The primary key value of the given table.
	 * @return true if the row exists, false otherwise.
	 */
	boolean exists(final Table table, final Long id);

	/**
	 * Checks if the row with the given where clause exists in the given set of tables.
	 *
	 * @param tables     The set of tables to check.
	 * @param whereClause The where clause to apply.
	 * @return true if the row exists, false otherwise.
	 */
	boolean exists(final SortedSet<Table> tables, final WhereClause whereClause);

	/**
	 * Checks if the row with the given {@link ExistsQuery} exists in the database.
	 *
	 * @param existsQuery Represents the {@link ExistsQuery}.
	 * @return true if the row exists, false otherwise.
	 */
	boolean exists(final ExistsQuery existsQuery);
}
