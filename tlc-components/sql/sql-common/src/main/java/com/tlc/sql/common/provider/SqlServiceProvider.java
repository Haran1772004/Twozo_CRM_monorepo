package com.tlc.sql.common.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.meta.DatabaseType;
import com.tlc.sql.common.registrar.SqlServiceRegistry;

import java.util.Objects;

/**
 * Provides the services that enable the {@link SQLService} to execute its functions effectively.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SqlServiceProvider {

    private static final SqlServiceRegistry SQL_SERVICE_REGISTRY;

    private SqlServiceProvider() {
    }

    static {
        SQL_SERVICE_REGISTRY = SqlServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link SQLService}.
     */
    public static SQLService get() {
        return get(DatabaseType.OLTP);
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link SQLService}.
     */
    public static SQLService getService(final DatabaseType databaseType) {
        return get(databaseType);
    }

    private static SQLService get(final DatabaseType databaseType) {
        final SQLService service = SQL_SERVICE_REGISTRY.get(databaseType);

        if (Objects.isNull(service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }
}