package com.tlc.sql.oltp.resolver;

import com.tlc.sql.common.api.dml.DerivedTable;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.resolver.TableResolver;
import com.tlc.sql.oltp.api.dml.OltpDerivedTable;
import com.tlc.sql.oltp.api.dml.OltpTable;

public class OltpTableResolver implements TableResolver {

    private static final class Instance {
        private static final TableResolver TABLE_RESOLVER = new OltpTableResolver();
    }

    private OltpTableResolver() {}

    public static TableResolver get() {
        return Instance.TABLE_RESOLVER;
    }

    @Override
    public Table resolve(final String tableName) {
        return OltpTable.get(tableName);
    }

    @Override
    public Table resolve(final String name, final String alias) {
        return new OltpTable(name, alias);
    }

    @Override
    public DerivedTable get(final String name, final String alias, final SelectQuery selectQuery) {
        return OltpDerivedTable.get(name, alias, selectQuery);
    }
}