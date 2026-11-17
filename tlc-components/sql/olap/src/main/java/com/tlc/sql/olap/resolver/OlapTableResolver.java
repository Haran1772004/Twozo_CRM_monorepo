package com.tlc.sql.olap.resolver;

import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.olap.api.dml.OlapDerivedTable;
import com.tlc.sql.olap.api.dml.OlapTable;

public class OlapTableResolver implements TableResolver {

    private static final class Instance {
        private static final TableResolver TABLE_RESOLVER = new OlapTableResolver();
    }

    private OlapTableResolver() {}

    public static TableResolver get() {
        return Instance.TABLE_RESOLVER;
    }

    @Override
    public Table resolve(final String tableName) {
        return OlapTable.get(tableName);
    }

    @Override
    public Table resolve(final String name, final String alias) {
        return new OlapTable(name, alias);
    }

    @Override
    public DerivedTable get(final String name, final String alias, final SelectQuery selectQuery) {
        return OlapDerivedTable.get(name, alias, selectQuery);
    }
}