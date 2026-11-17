package com.tlc.queue.internal.manager;

import com.tlc.sql.common.SQLService;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.ReadableDataStore;
import com.tlc.sql.common.api.ds.WritableDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

/**
 * Manager to handle the DB operations
 *
 * @param <T> Represents the type of the manager
 * @author sundar
 * @version 1.0
 */
public abstract class AbstractManager<T> {

    private SQLService sqlService;

    protected AbstractManager() {
    }

    /**
     * Gets the {@link SQLService} instance with lazy initialization
     *
     * @return {@link SQLService} instance
     */
    private SQLService getSqlService() {
        if (sqlService == null) {
            sqlService = SqlServiceProvider.get();
        }
        return sqlService;
    }

    /**
     * Gets the {@link ReadableDataStore} instance
     *
     * @return {@link ReadableDataStore} instance
     */
    protected ReadableDataStore getReadableDataStore() {
        return getSqlService().getAdminDataStore();
    }

    /**
     * Gets the {@link WritableDataStore} instance
     *
     * @return {@link WritableDataStore} instance
     */
    protected WritableDataStore getWritableDataStore() {
        return getSqlService().getAdminDataStore();
    }

    /**
     * Converts the {@link Row} into {@link T}
     *
     * @param row To be converted
     * @return {@link T}
     */
    protected abstract T convertRowToModel(final Row row);

    /**
     * Gets the relevant table
     *
     * @return {@link Table}
     */
    protected abstract Table getTable();
}