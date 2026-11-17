package com.tlc.sql.common.api.parser;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.TableDefinition;

/**
 * @author Abishek
 * @version 1.0
 */
public interface TableDefinitionLoader<C extends ColumnDefinition, T extends TableDefinition<C>> {

    boolean isDuplicate(String tableName);

    void add(T tableDefinition);

    T resolve(String tableName);

    default boolean loadKeys() {
        return true;
    }
}