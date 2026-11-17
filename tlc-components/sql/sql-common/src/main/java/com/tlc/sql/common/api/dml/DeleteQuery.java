package com.tlc.sql.common.api.dml;

import com.tlc.sql.common.internal.dml.DeleteQueryImpl;


/**
 * @author Abishek
 * @version 1.0
 */
public interface DeleteQuery extends Query, ActionQuery
{
    static DeleteQuery get(Table table)
    {
        return new DeleteQueryImpl(table);
    }
}
