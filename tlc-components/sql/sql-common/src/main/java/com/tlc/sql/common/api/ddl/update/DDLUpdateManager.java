package com.tlc.sql.common.api.ddl.update;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;

import java.util.Map;

/**
 * @author Abishek
 * @version 1.0
 */
public interface DDLUpdateManager<C extends ColumnDefinition, T extends TableDefinition<C>> {

    void doPreSchemaUpdate(Map<String, T> currentTableDef, Map<String, T> newTableDef);

    void doPostSchemaUpdate(Map<String, T> currentTableDef, Map<String, T> newTableDef);
}