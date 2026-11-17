package com.tlc.sql.common.internal.dml;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * @author Abishek
 * @version 1.0
 */
public class UpdateQueryImpl extends QueryImpl implements UpdateQuery
{
	private final LinkedHashMap<Column, Object> tobeUpdated = new LinkedHashMap<>();
	
	public UpdateQueryImpl(Table table)
	{
		super(table);
	}

	@Override
	public void addUpdateColumn(Column column , Object value)
	{		
		final TableDefinition<?> tableDef =  column.getTable().getTableDefinition();
		final String columnName = column.getColumnName();
		if(tableDef.getPkDefinition().getColumnName().equals(columnName))
		{
			throw ErrorCode.get(SQLErrorCodes.DB_PK_WRITE_RESTRICTED);
		}
		tobeUpdated.put(column , value);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @param column Represents the column to be removed from the query
	 */
	@Override
	public void removeColumn(final Column column) {
		tobeUpdated.remove(column);
	}

	@Override
	public Map<Column , Object> getColumnValueMap()
	{
		return Collections.unmodifiableMap(tobeUpdated);
	}
	
}
