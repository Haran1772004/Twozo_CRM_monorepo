package com.tlc.crm.common.action.list.pagination;

import com.tlc.crm.security.sql.DataStoreProvider;

import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;

/**
 * Abstract base class for managing pagination state persistence.
 *
 * @param <T> the type of {@link PaginationState} managed.
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public abstract class PaginationStateManager<T extends PaginationState> {

    protected PaginationStateManager() {
    }

    /**
     * Returns {@link OrgDataStore} of an organization.
     *
     * @return {@link OrgDataStore}.
     */
    protected final OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * Persists the updated pagination state.
     *
     * @param paginationState pagination state to update
     */
    protected abstract void update(final T paginationState);

    /**
     * Returns the database table associated with the pagination state.
     *
     * @return pagination state table
     */
    protected abstract Table getTable();

    /**
     * Gets the pagination state for the given model.
     *
     * @param model Reference model for which pagination state is to be retrieved.
     * @return The pagination state associated with the given model.
     */
    protected abstract PaginationState get(final T model);
}
