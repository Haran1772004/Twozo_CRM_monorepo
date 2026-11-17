package com.tlc.sql.common.ddl;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.handler.DDLHandler;

/**
 * @author Abishek
 * @version 1.0
 */
public interface DDLAction<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> {

    String[] getSQL(D ddlHandler);
}