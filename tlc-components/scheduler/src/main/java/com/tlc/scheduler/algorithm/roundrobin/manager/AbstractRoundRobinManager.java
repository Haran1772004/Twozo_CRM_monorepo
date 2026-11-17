package com.tlc.scheduler.algorithm.roundrobin.manager;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

/**
 * Base abstract class providing common DB operations for
 * managing round-robin related entities.
 *
 * @param <T> The model type this manager handles.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractRoundRobinManager<T> {

    protected final AdminDataStore adminDataStore;

    protected AbstractRoundRobinManager() {
        this.adminDataStore = SqlServiceProvider.get().getAdminDataStore();
    }

    /**
     * Retrieves the {@link OrgDataStore} corresponding to the given organization ID.
     *
     * @param orgId The unique identifier of the organization.
     * @return The corresponding {@link OrgDataStore} instance.
     */
    protected final OrgDataStore orgDataStore(final Long orgId) {
        return SqlServiceProvider.get().getOrgDataStore(orgId);
    }

    /**
     * Gets the {@link Table}.
     *
     * @return The {@link Table}.
     */
    protected abstract Table getTable();

    /**
     * Converts a database {@link Row} object into the corresponding model instance.
     *
     * @param row The database row retrieved from a query.
     * @return A fully populated model object.
     */
    protected abstract T convertRowToModel(final Row row);

}
