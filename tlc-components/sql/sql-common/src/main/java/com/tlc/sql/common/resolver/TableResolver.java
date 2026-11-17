package com.tlc.sql.common.resolver;

import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;

public interface TableResolver {

    Table resolve(final String tableName);

    Table resolve(final String name, final String alias);

    DerivedTable get(final String name, final String alias, final SelectQuery selectQuery);
}