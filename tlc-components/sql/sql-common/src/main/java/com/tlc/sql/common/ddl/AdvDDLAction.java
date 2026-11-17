package com.tlc.sql.common.ddl;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;
import com.tlc.sql.common.api.handler.DDLHandler;

/**
 * @author Abishek
 * @version 1.0
 */
public interface AdvDDLAction<C extends ColumnDefinition, T extends TableDefinition<C>, D extends DDLHandler<C, T>> extends DDLAction<C, T, D> {

    String[] getRevertSQL(D ddlHandler);
}