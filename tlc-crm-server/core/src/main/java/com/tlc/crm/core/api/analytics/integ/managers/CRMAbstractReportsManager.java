package com.tlc.crm.core.api.analytics.integ.managers;


import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.config.manager.AbstractConfigManager;

import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.core.api.analytics.integ.models.CRMBaseModel;

import com.tlc.commons.util.executor.CodeBlockExecutor;

import com.tlc.entity.EntityManager;
import com.tlc.entity.EntityManagerFactory;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorSummary;
import com.tlc.validator.model.ErrorDescriptor;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Managing CRM reports with support for CRUD operations.
 *
 * @author jamesh
 * @version 1.0
 */
public abstract class CRMAbstractReportsManager<M extends CRMBaseModel> extends AbstractConfigManager<M>
                                                                        implements CRMViewConfigManager<M>
{
    private static final ThreadLocal<Long> ORG_ID_THREAD_LOCAL = new ThreadLocal<>();
    private final Supplier<OrgDataStore> supplier              = () -> orgDataStore( ORG_ID_THREAD_LOCAL.get() );
    private final EntityManager<Long, M> entityManager         = EntityManagerFactory.create( getModelClass(), supplier );

    /**
     * Retrieves the {@link OrgDataStore} instance associated with the specified organization ID.
     * <p>This method provides access to the data store for a given organization, which can be used
     * to manage and persist organization-specific analytics data.</p>
     *
     * @param orgId         the unique identifier of the organization; must not be null
     * @return              the {@link OrgDataStore} corresponding to the given orgId
     * @throws Exception    if the data store cannot be retrieved or initialized
     */
    @Override
    public OrgDataStore getOrgDataStore( final @NonNull Long orgId ) throws Exception
    {
        return orgDataStore( orgId );
    }
    /**
     * Loads data into the given {@link DataContainer} for a create action using the provided entities.
     *
     * @param entities      The non-null collection of entities to populate the data container.
     * @param dataContainer The data container to be loaded with values.
     * @throws Exception    If an error occurs while loading data into the container.
     */
    @Override
    public void loadDataIntoContainer( final @NonNull Collection<M> entities,
                                       final DataContainer dataContainer ) throws Exception
    {
        entityManager.loadDataIntoContainer( entities, dataContainer );
    }

    /**
     * Loads data into the given {@link DataContainer} for an update action using the provided entities.
     *
     * @param entities      The non-null collection of entities to populate the data container.
     * @param dataContainer The data container to be loaded with updated values.
     * @throws Exception    If an error occurs while loading update data into the container.
     */
    @Override
    public void loadUpdateDataIntoContainer( final @NonNull Collection<M> entities,
                                             final DataContainer dataContainer ) throws Exception
    {
        entityManager.loadUpdateDataIntoContainer( entities, dataContainer );
    }

    /**
     * Creates a new record in the data store.
     *
     * @param m the model instance to be created
     * @return the created model instance, retrieved by its ID
     */
    @Override
    public final M create( final @NonNull M m )
    {
        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            ORG_ID_THREAD_LOCAL.set( m.orgId() );
            final Long id = entityManager.persist( m );
            return get( m.orgId(), id );
        });
    }

    /**
     * Retrieves a collection of models by their IDs.
     *
     * @param orgId    the organization ID for the models
     * @param modelIDs the IDs of the models to retrieve
     * @return a collection of models corresponding to the provided IDs
     */
    @Override
    public final @NonNull Collection<M> get( final @NonNull Long orgId, final @NonNull Collection<Long> modelIDs )
    {
        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            ORG_ID_THREAD_LOCAL.set( orgId );
            return entityManager.findAllByIds( Set.copyOf( modelIDs ) );
        });
    }

    /**
     * Retrieves all models based on the specified criteria.
     *
     * @param orgId    the organization ID for the models
     * @param criteria the criteria to filter models
     * @return a collection of models matching the criteria
     */
    public final @NonNull Collection<M> getAll( final @NonNull Long orgId, final @NonNull Criteria criteria ) throws Exception
    {
        ORG_ID_THREAD_LOCAL.set( orgId );
        final Supplier<WhereClause> whereClause = () -> new WhereClause( criteria );
        return entityManager.findAll( whereClause  );
    }

    /**
     * Retrieves all models based on the specified collection of criteria.
     *
     * @param orgId        the organization ID for the models
     * @param criteriaList the collection of {@link Criteria} to filter models
     */
    public final @NonNull Collection<M> getAll( final @NonNull Long orgId,
                                                final @NonNull Collection<Criteria> criteriaList ) throws Exception
    {
        ORG_ID_THREAD_LOCAL.set( orgId );
        WhereClause buildWhereClause = null;

        for ( final Criteria criteria : criteriaList )
        {
            buildWhereClause = Objects.isNull( buildWhereClause ) ? new WhereClause( criteria )
                                                                  : buildWhereClause.and( criteria );
        }

        final WhereClause whereClause = buildWhereClause;

        return entityManager.findAll( () -> whereClause );
    }

    /**
     * Retrieves a collection of models based on the specified organization ID and a collection of IDs.
     *
     * @param orgId      the organization ID for the models
     * @param collection the collection of IDs to retrieve
     * @return a collection of model details
     */
    @Override
    public final @NonNull Collection<M> partialGet( final @NonNull Long orgId, final @NonNull Collection<Long> collection )
    {
        // TODO: As of Now no partial get.
        return List.of();
    }

    /**
     * Updates a collection of models in the data store.
     *
     * @param orgId  the organization ID for the models
     * @param models the collection of models to update
     * @return a map containing the updated records and any failed updates
     */
    @Override
    protected ProcessOutcome update(final @NonNull Long orgId, final @NonNull Collection<M> models )
    {
        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();

        for( final M model : models )
        {
            try
            {
                ORG_ID_THREAD_LOCAL.set( orgId );
                entityManager.update( model );
                processOutcome.addProcessedRecord( model.id() );
            }
            catch( Exception exception )
            {
                LOGGER.error("Update action: Model update failed Org ID : {} ID : {} reason : {}", orgId, model.id(), exception.getMessage(), exception);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.UPDATE_ACTION_FAILED, model.id().toString()));
            }
        }

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * Get the class of the model type.
     *
     * @return the class of the model type
     */
    @NonNull
    abstract protected Class<M> getModelClass();
}
