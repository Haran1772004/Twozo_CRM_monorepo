package com.tlc.sql.common.internal.dml;

import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.Table;

/**
 * @author Abishek
 * @version 1.0
 */
public class DeleteQueryImpl extends QueryImpl implements DeleteQuery
{
    public DeleteQueryImpl(Table table)
    {
        super(table);
    }
}
