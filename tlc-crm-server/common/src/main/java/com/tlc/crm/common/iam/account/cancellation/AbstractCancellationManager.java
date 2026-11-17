package com.tlc.crm.common.iam.account.cancellation;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.crm.security.sql.DataStoreProvider;
import com.tlc.validator.TlcModel;

/**
 * Provides an abstract way to achieve the cancellation related action
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public abstract class AbstractCancellationManager<T extends TlcModel> {

    /**
     * <p>
     * Gets the secure org data store
     * </p>
     *
     * @param orgId Represents the organization id
     * @return The {@link OrgDataStore}
     */
    protected OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getSecureOrgDataStore(orgId);
    }

    /**
     * <p>
     * Gets the {@link Table}
     * </p>
     *
     * @return The cancellation {@link Table}
     */
    protected abstract Table getTable();

    /**
     * <p>
     * Processes the cancellation
     * </p>
     *
     * @param cancellationData The model having cancellation details
     */
    protected abstract void cancelAccount(final T cancellationData);

    /**
     * <p>
     * Checks whether a cancellation request has already been raised.
     * </p>
     *
     * @param id The ID to check for an existing cancellation request.
     * @return True if a cancellation request has already been raised, false otherwise
     */
    protected abstract boolean exists(final Long id);

    /**
     * <p>
     * Gets the {@link T}
     * </p>
     *
     * @param id Represents the Id to get the cancellation data
     * @return The {@link T}
     */
    protected abstract T get(final Long id);

    /**
     * <p>
     * Retains the cancellation
     * </p>
     *
     * @param id Represents the id to retain the cancellation
     */
    protected abstract void retain(final Long id);

    /**
     * <p>
     * Checks whether the user can retain the cancellation request
     * </p>
     *
     * @param id  Represents the Id to retain the cancellation
     * @return True if the user can retain, false otherwise
     */
    protected abstract boolean canRetain(final Long id);

    /**
     * <p>
     * Converts the {@link Row} to {@link T} model
     * </p>
     *
     * @param row The {@link Row}
     * @return The {@link T} model
     */
    protected abstract T convertRowToModel(final Long orgId, final Row row);
}