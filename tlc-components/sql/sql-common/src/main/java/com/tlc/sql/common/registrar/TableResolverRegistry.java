package com.tlc.sql.common.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.resolver.TableResolver;

import java.util.EnumMap;
import java.util.Map;

public final class TableResolverRegistry {

    private final Map<DatabaseType, TableResolver> tableResolverMap;

    public TableResolverRegistry() {
        this.tableResolverMap = new EnumMap<>(DatabaseType.class);
    }

    private static class Instance {
        private static final TableResolverRegistry TABLE_RESOLVER_REGISTRY = new TableResolverRegistry();
    }

    public static TableResolverRegistry getInstance() {
        return Instance.TABLE_RESOLVER_REGISTRY;
    }

    public void register(final DatabaseType databaseType, final TableResolver tableResolver) {

        if (tableResolverMap.containsKey(databaseType)) {
            throw ErrorCode.get(SQLErrorCodes.TABLE_RESOLVER_ALREADY_EXISTS);
        }
        tableResolverMap.put(databaseType, tableResolver);
    }

    public TableResolver get(final DatabaseType databaseType) {
        return tableResolverMap.get(databaseType);
    }
}
