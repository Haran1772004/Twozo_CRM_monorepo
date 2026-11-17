package com.tlc.sql.common.api.meta;

import java.util.Map;
import java.util.Set;

/**
 * @author Abishek
 * @version 1.0
 */
public interface MetaCache<T extends TableDefinition<?>> {

    void put(Map<String, T> tableDefinitions);

    void put(T tableDefinition);

    void remove(String tableName);

    void remove(Set<String> tableNames);

    T get(String tableName);
}
