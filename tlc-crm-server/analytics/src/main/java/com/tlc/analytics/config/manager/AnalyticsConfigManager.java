package com.tlc.analytics.config.manager;

import com.tlc.analytics.config.model.BaseModel;

import com.tlc.crm.common.config.manager.ConfigManager;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import lombok.NonNull;

import java.util.Collection;

/**
 * Interface for managing configurations.
 * It extends the {@link ConfigManager} interface, allowing operations on entities of type M
 * that extend {@link BaseModel}, providing a common structure for entities with shared attributes.
 * @param <M> The type of entity extending {@link BaseModel}.
 */
public interface AnalyticsConfigManager<M extends BaseModel> extends ConfigManager<M>
{
    /**
     * Loads data into the given {@link DataContainer} for a create action using the provided entities.
     *
     * @param entities      The non-null collection of entities to populate the data container.
     * @param dataContainer The data container to be loaded with values.
     * @throws Exception    If an error occurs while loading data into the container.
     */
    void loadDataIntoContainer( final @NonNull Collection<M> entities,
                                final DataContainer dataContainer ) throws Exception;

    /**
     * Loads data into the given {@link DataContainer} for an update action using the provided entities.
     *
     * @param entities      The non-null collection of entities to populate the data container.
     * @param dataContainer The data container to be loaded with updated values.
     * @throws Exception    If an error occurs while loading update data into the container.
     */
    void loadUpdateDataIntoContainer( final @NonNull Collection<M> entities,
                                      final DataContainer dataContainer ) throws Exception;

    /**
     * Retrieves the data store associated with the entity managed by this entity manager.
     *
     * @return The data store associated with the entity.
     */
    OrgDataStore getOrgDataStore( final @NonNull Long orgId ) throws Exception;

    @NonNull
    Table getTable();

    /**
     * Retrieves the models for the given criteria.
     *
     * @param orgId    The organization ID associated with the models.
     * @param criteria Holds the details of retrieval criteria.
     * @return A PK value corresponding to the provided criteria.
     */
    @NonNull
    Long getPKValue( final @NonNull Long orgId, final @NonNull Criteria criteria );

    /**
     * Retrieves the models for the given IDs from the data store.
     *
     * @param orgId    The organization ID associated with the models.
     * @param criteria Holds the details of retrieval criteria.
     * @return A collection of models corresponding to the provided IDs.
     */
    @NonNull
    Collection<M> getAll( final @NonNull Long orgId, final @NonNull Criteria criteria );

    /**
     * {@inheritDoc}
     *
     * @param orgId    The organization ID associated with the models.
     * @param whereClause Holds the details of retrieval whereClause.
     * @return A collection of models corresponding to the provided IDs.
     */
    @NonNull
    Collection<M> getAll( final @NonNull Long orgId, final @NonNull WhereClause whereClause );
}
