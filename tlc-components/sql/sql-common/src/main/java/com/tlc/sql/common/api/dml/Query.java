package com.tlc.sql.common.api.dml;


/**
 * @author Abishek
 * @version 1.0
 */
public interface Query
{
    void setWhereClause(WhereClause criteria);

    WhereClause getWhereClause();
    
    Table getBaseTable();
}
