package com.tlc.crm.common.config.manager;

import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;

import java.util.Collection;

public interface ConfigManager<T extends TlcModel> {

    /**
     * <p>
     * Checks whether any rows are having the id of given entity
     * </p>
     *
     * @param model
     * @return If no rows are having the matched entity id, it will return false
     */
    boolean exists(final T model);

    /**
     * <p>
     * Checks whether any rows are having the given id in the organization
     * </p>
     *
     * @param orgId
     * @param id
     * @return If no rows are having the given id, it will return false
     */
    boolean exists(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the row that matches the given id
     * </p>
     *
     * @param orgId
     * @param id
     * @return If no rows are having the given ids, it will return empty list otherwise it will return the rows
     * that match the one of the given ids
     */
    T get(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the rows that matches the given ids
     * </p>
     *
     * @param orgId
     * @param ids
     * @return If no rows are having the given ids, it will return empty list otherwise it will return the rows
     * that match the one of the given ids
     */
    Collection<T> get(final Long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Partially fetches values of an entity
     * </p>
     *
     * @param orgId
     * @param id
     * @return Partial values of an entity
     */
    T partialGet(final Long orgId, final Long id);

    /**
     * <p>
     * Fetches the rows that matches the given ids
     * </p>
     *
     * @param orgId represents the organization ID
     * @param ids   which contains the collection of ids
     * @return If no rows are having the given ids, it will return empty list otherwise it will return the rows
     * that match the one of the given ids
     */
    Collection<T> partialGet(final Long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Saves the given record
     * </p>
     *
     * @param model
     * @return Once the record has been saved, it will return the model with an identifier
     */
    T create(final T model);

    /**
     * <p>
     * Saves the given records into the database.
     * Failure of one record doesn't impact other records from create operation
     * </p>
     *
     * @param orgId
     * @param models
     * @return The {@link ProcessOutcome}
     */
    ProcessOutcome create(final Long orgId, final Collection<T> models);

    /**
     * <p>
     * Updates the row that matches the given record
     * </p>
     *
     * @param model
     * @return Updated entity which is a subclass of {@link TlcModel}
     */
    T update(final T model);

    /**
     * <p>
     * Updates the rows that are matches the given records
     * Failure of one record doesn't impact other records from update operation
     * </p>
     *
     * @param orgId
     * @param models
     * @return Collection of updated entities
     */
    ProcessOutcome bulkUpdate(Long orgId, Collection<T> models);

    /**
     * <p>
     * Deletes the row that matches the given record
     * </p>
     *
     * @param model
     */
    void delete(final T model);

    /**
     * <p>
     *     Deletes the rows that matches the given records
     * </p>
     *
     * @param orgId  Organisation ID
     * @param models Collection of models
     */
    void bulkDelete(final Long orgId, final Collection<T> models);

    /**
     * <p>
     * Deletes the rows that matches the given records
     * </p>
     *
     * @param orgId
     * @param models
     */
    void delete(final Long orgId, final Collection<T> models);
}
