package com.tlc.crm.common.config.manager.search;

import com.tlc.commons.json.JsonObject;

import com.tlc.search.model.FilterClause;

import com.tlc.validator.TlcModel;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides an abstract way to manage the search related actions of entities.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface IndexManager<T extends TlcModel> {

    /**
     * <p>
     * Documents the provided entities.
     * </p>
     *
     * @param models Active entities.
     */
    void create(final Collection<T> models);

    /**
     * <p>
     * Updates the documented entities.
     * </p>
     *
     * @param models Active entities.
     */
    void update(final Collection<T> models);

    /**
     * <p>
     * Updates follower ids.
     * </p>
     *
     * @param orgId       Organization's id.
     * @param id          Model id.
     * @param followerIds Follower ids.
     */
    void updateFollowerIds(final Long orgId, final Long id, final Collection<Long> followerIds);

    /**
     * <p>
     * Updates the documented entities partially.
     *
     * @param models Active entities.
     * @implNote - To be only used for alteration case i.e., value change, if we are using this fn for
     * cases like contact/company pUpdate use update fn above.
     * </p>
     */
    void partialUpdate(final Collection<T> models);

    /**
     * <p>
     * Updates the documented entities partially.
     * </p>
     *
     * @param orgId Organization's id.
     * @param id    Model id.
     * @param doc   Field name & it's value pair.
     */
    void partialUpdate(final Long orgId, final Long id, final Map<String, Object> doc);

    /**
     * <p>
     * Fetches the document for the given id or {@link FilterClause}.
     * </p>
     *
     * @param orgId          Represents the organisation id.
     * @param id             For fetch the required data.
     * @param includedFields Holds the details of the included fields.
     * @param filterClause   {@link FilterClause} Holds the criteria of required data.
     * @return {@link JsonObject} holds the details corresponding to the given id or {@link FilterClause}.
     */
    JsonObject get(final Long orgId, final Long id, final Collection<String> includedFields, final FilterClause filterClause);

    /**
     * <p>
     * Deletes the documented entities.
     * </p>
     *
     * @param models Active entities.
     */
    void delete(final Collection<T> models);

    /**
     * <p>
     * Processes merge by indexing the retained entity and removing / replacing duplicate references.
     * </p>
     *
     * @param retainedModel Retained entity after merge.
     * @param duplicateIds  Duplicate entity ids that were merged away.
     */
    void merge(final T retainedModel, final Collection<Long> duplicateIds);
}