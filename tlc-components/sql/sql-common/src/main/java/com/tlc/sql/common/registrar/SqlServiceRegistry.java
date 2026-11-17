package com.tlc.sql.common.registrar;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.meta.DatabaseType;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link SQLService} into the registry.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class SqlServiceRegistry implements ServiceRegistry<SQLService> {

    private final Map<DatabaseType, AtomicReference<SQLService>> sqlServiceMap;

    private SqlServiceRegistry() {
        this.sqlServiceMap = new EnumMap<>(DatabaseType.class);
    }
    private static final class Instance {
        private static final SqlServiceRegistry SQL_SERVICE_REGISTRY = new SqlServiceRegistry();
    }

    public static SqlServiceRegistry getInstance() {
        return Instance.SQL_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final SQLService service) {
        final DatabaseType databaseType = service.getDatabaseType();

        if (sqlServiceMap.containsKey(databaseType)) {
            final AtomicReference<SQLService> sqlServiceReference = sqlServiceMap.get(databaseType);

            if (!sqlServiceReference.compareAndSet(null, service)) {
                throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
            }
        } else {
            sqlServiceMap.put(databaseType, new AtomicReference<>(service));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public SQLService get() {
        return sqlServiceMap.get(DatabaseType.OLTP).get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link SQLService}.
     */
    @Override
    public SQLService unregister() {
        return sqlServiceMap.get(DatabaseType.OLTP).getAndSet(null);
    }

    public SQLService get(final DatabaseType databaseType) {
        return sqlServiceMap.get(databaseType).get();
    }

    public SQLService unregister(final DatabaseType databaseType) {
        return sqlServiceMap.get(databaseType).getAndSet(null);
    }
}