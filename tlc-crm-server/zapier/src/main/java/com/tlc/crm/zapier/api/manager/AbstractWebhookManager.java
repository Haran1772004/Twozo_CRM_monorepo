package com.tlc.crm.zapier.api.manager;

import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.TlcModel;

import com.tlc.crm.security.sql.DataStoreProvider;

/**
 * Abstract manager class for handling Zapier webhook operations
 * by defining common functionalities such as subscription handling, event processing, and data conversion.
 *
 * @param <T> The type of model that extends {@link TlcModel}
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class AbstractWebhookManager<T extends TlcModel> {

    /**
     * Gets the {@link OrgDataStore}.
     *
     * @param orgId Refers the organization id.
     * @return The {@link OrgDataStore}.
     */
    protected OrgDataStore orgDataStore(final Long orgId) {
        return DataStoreProvider.getOrgDataStore(orgId);
    }

    /**
     * Retrieves the table associated with this entity.
     *
     * @return The {@link Table}.
     */
    protected abstract Table getTable();

    /**
     * Saves the given model to the database and returns the saved instance.
     *
     * @param model The model object to be saved.
     * @return The saved instance of type {@code T}.
     */
    protected abstract T save(final T model);

    /**
     * Retrieves an entity of type {@code T} from the database using the given organization ID and entity ID.
     *
     * @param orgId The ID of the organization to which the entity belongs.
     * @param id    The unique identifier of the entity.
     * @return The retrieved entity of type {@code T}.
     */
    protected abstract T get(final Long orgId, final Long id);

    /**
     * Converts the {@link Row} into an instance of type {@code T}.
     *
     * @param row The row containing entity data.
     * @return The converted model of type {@code T}.
     */
    protected abstract T convertRowToModel(final Row row);


}
