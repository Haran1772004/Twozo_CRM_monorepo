package com.tlc.analytics.config.manager;

import com.tlc.analytics.config.model.BaseModel;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;
import com.tlc.crm.security.iam.LoggedInUserInfo;
import com.tlc.entity.EntityManager;
import com.tlc.entity.EntityManagerFactory;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;

import com.tlc.validator.ErrorKeys;
import com.tlc.validator.ProcessOutcome;
import com.tlc.validator.model.ErrorDescriptor;
import com.tlc.validator.model.ErrorSummary;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Abstract implementation of {@link AnalyticsConfigManager} providing common functionality
 * for managing configurations.
 * It extends {@link AbstractConfigManager} and implements {@link AnalyticsConfigManager},
 * operating on entities of type M that extend {@link BaseModel}.
 *
 * @param <M> The type of entity extending {@link BaseModel}.
 */
public abstract class AbstractAnalyticsConfigManager<M extends BaseModel> extends AbstractConfigManager<M> implements AnalyticsConfigManager<M>
{
    private final Supplier<OrgDataStore> supplier = () -> orgDataStore( LoggedInUserInfo.getUser().orgId() );
    private final EntityManager<Long, M> entityManager = EntityManagerFactory.create( getModelClass(), supplier );

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
     * Retrieves the data store associated with the entity managed by this entity manager.
     *
     * @return The data store associated with the entity.
     */
    @Override
    public OrgDataStore getOrgDataStore( final @NonNull Long orgId ) throws Exception
    {
        return orgDataStore( orgId );
    }

    /**
     * Updates the configurations for the given models.
     * This method updates the data store with the provided models and handles any exceptions that occur.
     *
     * @param orgId  The organization ID associated with the models.
     * @param models The collection of models to update.
     * @return A map containing the IDs of updated records and any failed records along with the reason for failure.
     */
    @Override
    protected final ProcessOutcome update(final Long orgId, final Collection<M> models )
    {
        LOGGER.debug("Analytics: Config Manager - Updating configurations for orgId: {}", orgId );

        final ProcessOutcome processOutcome = new ProcessOutcome();
        final List<ErrorDescriptor> failedEntities = new ArrayList<>();

        models.forEach( model ->
        {
            try
            {
                final Long modelId = model.id();

                entityManager.update( model );
                processOutcome.addProcessedRecord( modelId );
                LOGGER.info("Analytics: Config Manager - Configuration updated successfully for modelId: {}", modelId );
            }
            catch( Exception errorCode )
            {
                LOGGER.error("Analytics: Config Manager - Update failed for orgId: {}, modelId: {}, reason: {}", orgId, model.id(), errorCode.getMessage(), errorCode);
                failedEntities.add(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                        "i18n.core.update.failed"), ErrorKeys.CREATE_ACTION_FAILED, String.valueOf(model.id())));
            }
        } );

        if (!failedEntities.isEmpty()) {
            processOutcome.setErrorSummary(new ErrorSummary(new ErrorDescriptor(ErrorCode.get(CommonErrorCodes.UPDATE_ACTION_FAILED,
                    "i18n.core.update.failed")), failedEntities));
        }
        return processOutcome;
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId The organization ID associated with the models.
     * @param ids   The collection of IDs to retrieve.
     * @return A collection of models corresponding to the provided IDs.
     */
    @Override
    public final Collection<M> get( final Long orgId, final Collection<Long> ids )
    {
        LOGGER.debug("Analytics: Config Manager - Retrieving models for orgId: {} and ids: {}", orgId, ids );

        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            final Collection<M> models = entityManager.findAllByIds( Set.copyOf( ids ) );
            LOGGER.debug("Analytics: Config Manager - Retrieved {} models successfully", models.size() );
            return models;
        } );
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    The organization ID associated with the models.
     * @param criteria Holds the details of retrieval criteria.
     * @return A collection of models corresponding to the provided IDs.
     */
    @NonNull
    @Override
    public final Collection<M> getAll( final @NonNull Long orgId, final @NonNull Criteria criteria )
    {
        LOGGER.debug("Analytics: Config Manager - Retrieving models for orgId: {} and criteria: {}", orgId, criteria );

        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            final Collection<M> models = entityManager.findAll( () -> new WhereClause( criteria ) );
            LOGGER.debug("Analytics: Config Manager - Retrieved {} models successfully", models.size() );
            return models;
        } );
    }

    /**
     * {@inheritDoc}
     *
     * @param orgId    The organization ID associated with the models.
     * @param whereClause Holds the details of retrieval whereClause.
     * @return A collection of models corresponding to the provided IDs.
     */
    @NonNull
    @Override
    public final Collection<M> getAll( final @NonNull Long orgId, final @NonNull WhereClause whereClause )
    {
        LOGGER.debug("Analytics: Config Manager - Retrieving models for orgId: {} and criteria: {}", orgId, whereClause );

        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            final Collection<M> models = entityManager.findAll( () -> whereClause );
            LOGGER.debug("Analytics: Config Manager - Retrieved {} models successfully", models.size() );
            return models;
        } );
    }

    /**
     * Retrieves the models for the given criteria.
     *
     * @param orgId    The organization ID associated with the models.
     * @param criteria Holds the details of retrieval criteria.
     * @return A PK value corresponding to the provided criteria.
     */
    @NonNull
    @Override
    public final Long getPKValue( final @NonNull Long orgId, final @NonNull Criteria criteria )
    {
        LOGGER.debug("Analytics: Config Manager - Retrieving models for orgId: {} and criteria: {}", orgId, criteria );

        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            final Long pkValue = entityManager.findPKValue( () -> new WhereClause( criteria ) );
            LOGGER.debug("Analytics: Config Manager - Retrieved {} PK values successfully", pkValue );
            return pkValue;
        } );
    }

    /**
     * Retrieves the models for the given IDs from the data store.
     *
     * @param orgId The organization ID associated with the models.
     * @param ids   The collection of IDs to retrieve.
     * @return A collection of models corresponding to the provided IDs.
     */
    @Override
    public Collection<M> partialGet( final Long orgId, final Collection<Long> ids )
    {
        LOGGER.debug("Analytics: Config Manager - Retrieving partial configurations for orgId: {} and ids: {}", orgId, ids );
        return null;
    }

    /**
     * Creates a new model in the data store.
     *
     * @param model The model to create.
     * @return The newly created model.
     */
    @Override
    public final M create( final M model )
    {
        LOGGER.debug("Analytics: Config Manager - Creating configuration for modelId: {}", model.id() );

        return CodeBlockExecutor.getWithUnchecked( () ->
        {
            Long id = entityManager.persist( model );
            LOGGER.info("Analytics: Config Manager - New model created successfully with id: {}", id );
            return get( model.orgId(), id );
        } );
    }

    /**
     * Retrieves the class of the model.
     * This method needs to be implemented by subclasses to provide the concrete class of the model.
     *
     * @return The class of the model.
     */
    @NonNull
    abstract protected Class<M> getModelClass();
}
