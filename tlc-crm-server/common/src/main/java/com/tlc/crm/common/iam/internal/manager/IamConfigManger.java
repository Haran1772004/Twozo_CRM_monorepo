package com.tlc.crm.common.iam.internal.manager;

import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.TlcModel;

import java.util.Collection;

/**
 * <p>
 * Performs the functionality for {@link IamConfigManger} DB related actions
 * </p>
 *
 * @author PandiarajkumarG
 * @version 1.0
 */
public interface IamConfigManger<T extends TlcModel> {

    /**
     * <p>
     * Checks whether the entity ids are exists or not
     * </p>
     *
     * @param orgId Represents an organization id
     * @param ids   Contains the collection of ids
     * @return If any id is exists it will return true, otherwise it will return false
     */
    boolean exists(final Long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Saves the given models in the database
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param models Contains the collection of models
     * @return The Collection of models
     */
    ProcessOutcome create(final Long orgId, final Collection<T> models);

    /**
     * <p>
     * Saves the given model in the database
     * </p>
     *
     * @param model Represents the model
     * @return Once the record has been saved, it will return the model
     */
    T create(final T model);

    /**
     * <p>
     * Partially updates the entities that are matches the given records
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param models Contains the collection of models
     * @return The {@link ProcessOutcome}
     */
    ProcessOutcome partialUpdate(final Long orgId, final Collection<T> models);

    /**
     * <p>
     * Updates the entities that are matches the given records
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param models Contains the collection of models
     * @return Collection of updated entities
     */
    ProcessOutcome update(final Long orgId, final Collection<T> models);

    /**
     * <p>
     * Deletes the entries that matches the given ids
     * </p>
     *
     * @param orgId  Represents an organization id
     * @param models Contains the collection of models
     */
    void delete(final Long orgId, final Collection<T> models);

    /**
     * <p>
     * Fetches the entity that matches the given entity id
     * </p>
     *
     * @param orgId Represents an organization id
     * @param id    Represents the entity id
     * @return The collection of model given by id
     */
    T get(final Long orgId, final Long id);

    /**
     * <p>
     * Retrieves the total count of entities
     * </p>
     *
     * @param orgId    Represents the organization id
     * @return The total count of entities
     */
    long getTotalCount(final Long orgId);

    /**
     * <p>
     * Retrieves the total count of entities
     * </p>
     *
     * @param orgId    Represents the organization id
     * @param criteria Represents the criteria
     * @return The total count of entities
     */
    long getTotalCount(final Long orgId, final Criteria criteria);
}
