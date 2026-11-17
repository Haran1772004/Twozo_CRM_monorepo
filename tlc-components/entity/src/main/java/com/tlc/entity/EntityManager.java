package com.tlc.entity;

import com.tlc.commons.functions.SafeConsumer;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;

import lombok.NonNull;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * An interface for managing entities in a database.
 *
 * @param <ID> The type of the identifier (primary key) for the entities managed by the entity manager.
 * @param <E>  The type of the entities managed by the entity manager.
 *             These entities are assumed to implement the {@link Entity} interface with identifiers of type {@code ID}.
 */
public interface EntityManager<ID, E extends Entity<ID>>
{
    /**
     * Loads data into the given {@link DataContainer} for an update action using the provided entities.
     *
     * @param entities      The non-null collection of entities to populate the data container.
     * @param dataContainer The data container to be loaded with updated values.
     * @throws Exception    If an error occurs while loading update data into the container.
     */
    void loadDataIntoContainer( final @NonNull Collection<E> entities,
                                final DataContainer dataContainer ) throws Exception;

    /**
     * Loads data into the given {@link DataContainer} for an update action using the provided entities.
     *
     * @param entities      The non-null collection of entities to populate the data container.
     * @param dataContainer The data container to be loaded with updated values.
     * @throws Exception    If an error occurs while loading update data into the container.
     */
    void loadUpdateDataIntoContainer( final @NonNull Collection<E> entities,
                                      final DataContainer dataContainer ) throws Exception;

    /**
     * Persists a new entity to the data store.
     *
     * @param entity The entity to persist.
     * @return The ID of the persisted entity.
     * @throws Exception If the persistence fails.
     */
    @NonNull
    ID persist( @NonNull final E entity ) throws Exception;

    /**
     * Persists multiple entities to the data store.
     *
     * @param entities The collection of entities to persist.
     * @return A set of IDs of the persisted entities.
     * @throws Exception If the persistence fails.
     */
    @NonNull
    Set<ID> persistAll( @NonNull final Collection<E> entities ) throws Exception;

    /**
     * Merges an existing entity in the data store.
     *
     * @param entity The entity to merge.
     * @throws Exception If the merge fails.
     */
    void update( @NonNull final E entity ) throws Exception;

    /**
     * Merges multiple entities in the data store.
     *
     * @param entities The collection of entities to merge.
     * @throws Exception If the merge fails.
     */
    void updateAll( @NonNull final Collection<E> entities ) throws Exception;

    /**
     * Removes an entity from the data store by its ID.
     *
     * @param id The ID of the entity to remove.
     * @throws Exception If the removal fails.
     */
    void deleteById( @NonNull final ID id ) throws Exception;

    /**
     * Removes multiple entities from the data store by their IDs.
     *
     * @param idSet The set of IDs of the entities to remove.
     * @throws Exception If the removal fails.
     */
    void deleteAllByIds( @NonNull final Set<ID> idSet ) throws Exception;

    /**
     * Finds an entity by its ID.
     *
     * @param id The ID of the entity to find.
     * @return An Optional containing the entity if found, or empty if not found.
     * @throws Exception If the find fails.
     */
    @NonNull
    Optional<E> findById( @NonNull final ID id ) throws Exception;

    /**
     * Finds an entity by its ID, throwing an exception if not found.
     *
     * @param id The ID of the entity to find.
     * @return The entity if found.
     * @throws Exception If the entity is not found or find fails.
     */
    @NonNull
    E findByIdOrThrow( @NonNull final ID id ) throws Exception;

    /**
     * Finds multiple entities by their IDs.
     *
     * @param idSet The set of IDs of the entities to find.
     * @return A collection of the found entities.
     * @throws Exception If the find fails.
     */
    @NonNull
    Collection<E> findAllByIds( @NonNull final Set<ID> idSet ) throws Exception;

    /**
     * Finds an entity that matches the criteria provided by the supplier.
     *
     * @param supplier The supplier providing the where clause.
     * @return An Optional containing the entity if found, or empty if not found.
     * @throws Exception If the find fails.
     */
    @NonNull
    Optional<E> findOne( @NonNull final Supplier< WhereClause > supplier ) throws Exception;

    /**
     * Finds an entity that matches the criteria provided by the supplier, throwing an exception if not found.
     *
     * @param supplier The supplier providing the where clause.
     * @return The entity if found.
     * @throws Exception If the entity is not found or find fails.
     */
    @NonNull
    E findOneOrThrow( @NonNull final Supplier< WhereClause > supplier ) throws Exception;

    /**
     * Finds multiple entities that match the criteria provided by the supplier.
     *
     * @param supplier The supplier providing the where clause.
     * @return A collection of the found entities.
     * @throws Exception If the find fails.
     */
    @NonNull
    Collection<E> findAll( @NonNull final Supplier<WhereClause> supplier ) throws Exception;

    /**
     * Performs a custom find operation using a provided consumer to modify the select query.
     *
     * @param id       The ID of the entity to retrieve.
     * @param consumer The consumer to modify the select query.
     * @return A DataContainer containing the retrieved data.
     * @throws Exception If the retrieval fails.
     */
    @NonNull
    DataContainer customFindById( @NonNull final ID id, @NonNull final SafeConsumer<SelectQuery> consumer ) throws Exception;

    /**
     * Performs a custom find operation using a provided consumer to modify the select query for multiple entities.
     *
     * @param idSet    The set of IDs of the entities to retrieve.
     * @param consumer The consumer to modify the select query.
     * @return A DataContainer containing the retrieved data.
     * @throws Exception If the retrieval fails.
     */
    @NonNull
    DataContainer customFindAllByIds( @NonNull final Set<ID> idSet, @NonNull final SafeConsumer<SelectQuery> consumer ) throws
            Exception;

    /**
     * Performs a custom find operation using a provided consumer to modify the select query.
     *
     * @param consumer The consumer to modify the select query.
     * @return A DataContainer containing the retrieved data.
     * @throws Exception If the retrieval fails.
     */
    @NonNull
    DataContainer customFind( @NonNull final SafeConsumer<SelectQuery> consumer ) throws Exception;

    /**
     * Performs a custom update operation using a provided consumer to modify the update query for a single entity.
     *
     * @param id       The ID of the entity to update.
     * @param consumer The consumer to modify the update query.
     * @throws Exception If the update fails.
     */
    void customUpdateById( @NonNull final ID id, @NonNull final SafeConsumer<UpdateQuery> consumer ) throws Exception;

    /**
     * Performs a custom update operation using a provided consumer to modify the update query for multiple entities.
     *
     * @param idSet    The set of IDs of the entities to update.
     * @param consumer The consumer to modify the update query.
     * @throws Exception If the update fails.
     */
    void customUpdateAllByIds( @NonNull final Set<ID> idSet, @NonNull final SafeConsumer<UpdateQuery> consumer ) throws Exception;

    /**
     * Performs a custom update operation using a provided consumer to modify the update query.
     *
     * @param consumer The consumer to modify the update query.
     * @throws Exception If the update fails.
     */
    void customUpdate( @NonNull final SafeConsumer<UpdateQuery> consumer ) throws Exception;

    /**
     * Performs a custom delete operation using a provided consumer to modify the delete query for a single entity.
     *
     * @param id       The ID of the entity to delete.
     * @param consumer The consumer to modify the delete query.
     * @throws Exception If the deletion fails.
     */
    void customDeleteById( @NonNull final ID id, @NonNull final SafeConsumer<DeleteQuery> consumer ) throws Exception;

    /**
     * Performs a custom delete operation using a provided consumer to modify the delete query for multiple entities.
     *
     * @param idSet    The set of IDs of the entities to delete.
     * @param consumer The consumer to modify the delete query.
     * @throws Exception If the deletion fails.
     */
    void customDeleteAllByIds( @NonNull final Set<ID> idSet, @NonNull final SafeConsumer<DeleteQuery> consumer ) throws Exception;

    /**
     * Performs a custom delete operation using a provided consumer to modify the delete query.
     *
     * @param consumer The consumer to modify the delete query.
     * @throws Exception If the deletion fails.
     */
    void customDelete( @NonNull final SafeConsumer<DeleteQuery> consumer ) throws Exception;

    /**
     * Counts the number of entities in the data store.
     *
     * @return The count of entities.
     */
    long count();

    /**
     * Processes each entity in the data store using the provided consumer.
     *
     * @param consumer The consumer to process each entity.
     */
    void forEach( @NonNull final SafeConsumer<E> consumer );

    /**
     * Finds the PK value that match the criteria provided by the supplier.
     *
     * @param supplier The supplier providing the where clause.
     * @return A collection of the found entities.
     * @throws Exception If the find fails.
     */
    @NonNull
    Long findPKValue( @NonNull final Supplier< WhereClause > supplier ) throws Exception;

}

