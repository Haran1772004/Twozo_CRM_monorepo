package com.tlc.sql.common.api.dml;

import com.tlc.sql.common.internal.dml.UpdateQueryImpl;

import java.util.Map;


/**
 * @author Abishek
 * @version 1.0
 */
public interface UpdateQuery extends Query, ActionQuery
{
	static UpdateQuery get(Table table)
	{
		return new UpdateQueryImpl(table);
	}

	void addUpdateColumn(Column column, Object value);

	/**
	 * Removes the specified column from the set of updatable columns in the query
	 *
	 * @param column Represents the column to be removed from the query
	 */
	void removeColumn(final Column column);

	Map<Column, Object> getColumnValueMap();
}
