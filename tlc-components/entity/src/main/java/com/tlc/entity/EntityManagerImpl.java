package com.tlc.entity;

import com.tlc.commons.functions.SafeConsumer;
import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.commons.util.executor.NullSafeExecutor;
import com.tlc.entity.converter.ConverterFactory;
import com.tlc.entity.converter.ConverterUtil;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.Query;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.OrgDataStore;
import com.tlc.sql.common.internal.data.DataContainerImpl;
import com.tlc.sql.common.internal.dml.CountQueryImpl;
import com.tlc.sql.common.internal.dml.DeleteQueryImpl;
import com.tlc.sql.common.internal.dml.UpdateQueryImpl;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * An abstract implementation of the {@link EntityManager} interface providing common functionality.
 * This class extends {@link AbstractEntityManager} and serves as a base for concrete implementations
 * of entity managers.
 *
 * @param <ID> The type of the identifier (primary key) for the entities managed by the entity manager.
 * @param <E>  The type of the entities managed by the entity manager.
 *             These entities are assumed to implement the {@link Entity} interface with identifiers of type {@code ID}.
 */
abstract class EntityManagerImpl<ID, E extends Entity<ID>>
        extends AbstractEntityManager<ID, E>
{
    /**
     * {@inheritDoc}
     *
     * @param entities The collection of entities to persist.
     * @return A set of IDs of the persisted entities.
     * @throws Exception If the persistence fails.
     */
    @Override
    public @NonNull Set<ID> persistAll( @NonNull final Collection<E> entities ) throws Exception
    {
        final DataContainer dataContainer = new DataContainerImpl();
        persistOrUpdate( entities, dataContainer );
        return entities.stream()
                       .map( Entity::id )
                       .collect( Collectors.toSet() );
    }

    /**
     * {@inheritDoc}
     *
     * @param entities The collection of entities to merge.
     * @throws Exception If the merge fails.
     */
    @Override
    public void updateAll( @NonNull final Collection<E> entities ) throws Exception
    {
        final Set<ID> ids = entities.stream()
                                    .map( Entity::id )
                                    .collect( Collectors.toSet() );

        final DataContainer dataContainer = customFindAllByIds( ids, selectQuery ->
        {
            final E dummy = ConverterUtil.newInstance( getEntityClass() );
            ConverterFactory.getEntityToSelQueryConverter().convert( dummy, selectQuery );
        } );
        persistOrUpdate( entities, dataContainer );
    }

    /**
     * Persists or updates a collection of entities using the provided data container.
     *
     * @param entities      The collection of entities to persist or update.
     * @param dataContainer The data container used for persistence or update.
     * @throws Exception If an error occurs during persistence or update.
     */
    private void persistOrUpdate( @NonNull final Collection<E> entities, final DataContainer dataContainer ) throws Exception
    {
        CodeBlockExecutor.forEachSafely( entities, entity ->
                ConverterFactory.getEntityToDataContainerConverter().convert( entity, dataContainer ) );
        getOrgDataStore().commitChanges( dataContainer );
    }

    /**
     * Loads data into the given {@link DataContainer} for a create action using the provided entities.
     *
     * @param entities      The non-null collection of entities to populate the data container.
     * @param dataContainer The data container to be loaded with values.
     * @throws Exception    If an error occurs while loading data into the container.
     */
    @Override
    public void loadDataIntoContainer( final @NonNull Collection<E> entities,
                                       final DataContainer dataContainer ) throws Exception
    {
        CodeBlockExecutor.forEachSafely( entities, entity ->
                ConverterFactory.getEntityToDataContainerConverter().convert( entity, dataContainer ) );
    }

    /**
     * Loads data into the given {@link DataContainer} for an update action using the provided entities.
     *
     * @param entities      The non-null collection of entities to populate the data container.
     * @param dataContainer The data container to be loaded with updated values.
     * @throws Exception    If an error occurs while loading update data into the container.
     */
    @Override
    public void loadUpdateDataIntoContainer( final @NonNull Collection<E> entities,
                                             final DataContainer dataContainer ) throws Exception
    {
        final Set<ID> ids = entities.stream()
                              .map( Entity::id )
                              .collect( Collectors.toSet() );

        dataContainer.append( customFindAllByIds( ids, selectQuery ->
        {
            final E dummy = ConverterUtil.newInstance( getEntityClass() );
            ConverterFactory.getEntityToSelQueryConverter().convert( dummy, selectQuery );
        } ) );
        loadDataIntoContainer( entities, dataContainer );
    }

    /**
     * {@inheritDoc}
     *
     * @param idSet The set of IDs of the entities to remove.
     * @throws Exception If the removal fails.
     */
    @Override
    public void deleteAllByIds( @NonNull final Set<ID> idSet ) throws Exception
    {
        customDeleteAllByIds( idSet, delQuery -> {} );
    }

    /**
     * {@inheritDoc}
     *
     * @param idSet The set of IDs of the entities to find.
     * @return A collection of the found entities.
     * @throws Exception If the find fails.
     */
    @Override
    public @NonNull Collection<E> findAllByIds( @NonNull final Set<ID> idSet ) throws Exception
    {
        final Table table = getTable();
        return findAll( () -> new WhereClause( Criteria.in( table.getPKColumn(), idSet ) ) );
    }

    /**
     * {@inheritDoc}
     *
     * @param supplier The supplier providing the where clause.
     * @return A collection of the found entities.
     * @throws Exception If the find fails.
     */
    @Override
    public @NonNull Collection<E> findAll( @NonNull final Supplier< WhereClause > supplier ) throws Exception
    {
        final DataContainer dataContainer = customFind( selQuery ->
        {
            final E dummy = ConverterUtil.newInstance( getEntityClass() );
            ConverterFactory.getEntityToSelQueryConverter().convert( dummy, selQuery );
            final WhereClause whereClause = supplier.get();
            selQuery.setWhereClause( whereClause );
        } );

        final List<E> entities = new ArrayList<>();
        dataContainer.getRows( getTable() )
                     .forEach( row -> CodeBlockExecutor.runAsUnchecked( () ->
                     {
                         final Object pkId = row.getPKValue();
                         final E entity = ConverterUtil.newInstance( getEntityClass() );
                         ConverterFactory.getDataContainerToEntityConverter( pkId ).convert( entity, dataContainer );
                         entities.add( entity );
                     } ) );
        return entities;
    }

    /**
     * {@inheritDoc}
     *
     * @param supplier The supplier providing the where clause.
     * @return A collection of the found entities.
     */
    @NonNull
    @Override
    public Long findPKValue( @NonNull final Supplier< WhereClause > supplier ) throws Exception
    {
        final DataContainer dataContainer = customFind( selQuery ->
        {
            final WhereClause whereClause = supplier.get();

            selQuery.setWhereClause( whereClause );
            selQuery.addSelectClause( getTable().getPKColumn() );
        });

        return dataContainer.getRow( getTable() )
                            .getPKValue();
    }

    /**
     * {@inheritDoc}
     *
     * @param idSet    The set of IDs of the entities to retrieve.
     * @param consumer The consumer to modify the select query.
     * @return A DataContainer containing the retrieved data.
     * @throws Exception If the retrieval fails.
     */
    @Override
    public @NonNull DataContainer customFindAllByIds( @NonNull final Set<ID> idSet,
                                                      @NonNull final SafeConsumer<SelectQuery> consumer ) throws Exception
    {
        final Table table = getTable();
        return customFind( selQuery ->
        {
            consumer.accept( selQuery );
            setCriteria( table, idSet, selQuery );
        } );
    }

    /**
     * {@inheritDoc}
     *
     * @param consumer The consumer to modify the select query.
     * @return A DataContainer containing the retrieved data.
     * @throws Exception If the retrieval fails.
     */
    @Override
    public @NonNull DataContainer customFind( @NonNull final SafeConsumer<SelectQuery> consumer ) throws Exception
    {
        final Table table = getTable();
        final SelectQuery selectQuery = SelectQuery.get( table );
        consumer.accept( selectQuery );
        return getOrgDataStore().get( selectQuery );
    }

    /**
     * {@inheritDoc}
     *
     * @param idSet    The set of IDs of the entities to update.
     * @param consumer The consumer to modify the update query.
     * @throws Exception If the update fails.
     */
    @Override
    public void customUpdateAllByIds( @NonNull final Set<ID> idSet,
                                      @NonNull final SafeConsumer<UpdateQuery> consumer ) throws Exception
    {
        final Table table = getTable();
        customUpdate( updateQuery ->
        {
           consumer.accept( updateQuery );
           setCriteria( table, idSet, updateQuery );
        } );
    }

    /**
     * {@inheritDoc}
     *
     * @param consumer The consumer to modify the update query.
     * @throws Exception If the update fails.
     */
    @Override
    public void customUpdate( @NonNull final SafeConsumer<UpdateQuery> consumer ) throws Exception
    {
        final Table table = getTable();
        final UpdateQuery updateQuery = new UpdateQueryImpl( table );
        consumer.accept( updateQuery );
        getOrgDataStore().update( updateQuery );
    }

    /**
     * {@inheritDoc}
     *
     * @param idSet    The set of IDs of the entities to delete.
     * @param consumer The consumer to modify the delete query.
     * @throws Exception If the deletion fails.
     */
    @Override
    public void customDeleteAllByIds( @NonNull final Set<ID> idSet,
                                      @NonNull final SafeConsumer<DeleteQuery> consumer ) throws Exception
    {
        customDelete( delQuery ->
        {
            consumer.accept( delQuery );
            final List<ID> sortedIds = new ArrayList<>( List.copyOf( idSet ) );
            sortedIds.sort( Collections.reverseOrder() );
            setCriteria( getTable(), sortedIds, delQuery );
        } );
    }

    /**
     * Sets the criteria for a query based on the provided table and collection of IDs.
     *
     * @param table The table to set the criteria on.
     * @param ids   The collection of IDs to include in the criteria.
     * @param query The query to which the criteria will be applied.
     */
    private void setCriteria( @NonNull final Table table, @NonNull final Collection<ID> ids, @NonNull final Query query )
    {
        final Column pkColumn = table.getPKColumn();
        final Criteria criteria = Criteria.in( pkColumn, ids );
        final WhereClause whereClause = NullSafeExecutor.getIfPresent( query.getWhereClause(), wClause ->
        {
            wClause.and( criteria );
            return wClause;
        } ).orElseGet( () -> new WhereClause( criteria ) );
        query.setWhereClause( whereClause );
    }

    /**
     * {@inheritDoc}
     *
     * @param consumer The consumer to modify the delete query.
     * @throws Exception If the deletion fails.
     */
    @Override
    public void customDelete( @NonNull final SafeConsumer<DeleteQuery> consumer ) throws Exception
    {
        final Table table = getTable();
        final DeleteQuery deleteQuery = new DeleteQueryImpl( table );
        consumer.accept( deleteQuery );
        getOrgDataStore().delete( deleteQuery );
    }

    /**
     * {@inheritDoc}
     *
     * @return The count of entities.
     */
    @Override
    public long count()
    {
        final Table table = getTable();
        final CountQuery countQuery = new CountQueryImpl( table );
        return getOrgDataStore().get( countQuery );
    }

    /**
     * {@inheritDoc}
     *
     * @param consumer The consumer to process each entity.
     */
    @Override
    public void forEach( @NonNull final SafeConsumer<E> consumer )
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves the class of the entity managed by this entity manager.
     *
     * @return The class of the entity.
     */
    @NonNull
    protected abstract Class<E> getEntityClass();

    /**
     * Retrieves the table associated with the entity managed by this entity manager.
     *
     * @return The table associated with the entity.
     */
    @NonNull
    protected abstract Table getTable();

    /**
     * Retrieves the data store associated with the entity managed by this entity manager.
     *
     * @return The data store associated with the entity.
     */
    @NonNull
    protected abstract OrgDataStore getOrgDataStore();
}
