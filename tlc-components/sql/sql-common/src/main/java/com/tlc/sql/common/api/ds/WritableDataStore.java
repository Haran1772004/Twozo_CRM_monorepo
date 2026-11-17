package com.tlc.sql.common.api.ds;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.ActionQuery;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.listeners.CompleteRowListener;
import com.tlc.sql.common.api.listeners.RowAddListener;
import com.tlc.sql.common.api.listeners.RowDeleteListener;
import com.tlc.sql.common.api.listeners.RowIdDeleteListener;
import com.tlc.sql.common.api.listeners.RowListener;
import com.tlc.sql.common.api.listeners.RowUpdateListener;

import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;

/**
 * @author Abishek
 * @version 1.0
 */
public interface WritableDataStore extends ReadableDataStore
{
	void update(UpdateQuery query);

	void update(List<UpdateQuery> queries);

	void delete(Table table, WhereClause whereClause);

	void delete(Table table, Long id);

	void delete(Table table, Collection<Long> ids);

	void delete(DeleteQuery deleteQuery);

	void delete(List<DeleteQuery> deleteQueries);

	void delete(NavigableMap<Table, NavigableSet<Long>> tableAndPk);

	void addRow(Row row);

	void addRows(List<Row> rows);

	void registerRowListener(Table table, RowListener rowListener);

	void registerRowListener(Table table, CompleteRowListener rowListener);

	void registerNewEntryListener(Table table, RowAddListener rowAddListener);

	void registerUpdatedEntryListener(Table table, RowUpdateListener rowUpdateListener);

	void registerDeletedEntryListener(Table table, RowDeleteListener rowDeleteListener);

	void registerDeletedEntryListener(Table table, RowIdDeleteListener rowIdDeleteListener);

	void executeActions(List<ActionQuery> queries);

	void commitChanges(DataContainer dataContainer);
}
