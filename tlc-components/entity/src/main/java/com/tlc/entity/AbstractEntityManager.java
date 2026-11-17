package com.tlc.entity;

import com.tlc.commons.functions.SafeConsumer;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.dml.DeleteQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.UpdateQuery;
import com.tlc.sql.common.api.dml.WhereClause;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * An abstract class providing a base implementation for the {@link EntityManager} interface.
 *
 * @param <ID> The type of the identifier (primary key) for the entities managed by the entity manager.
 * @param <E>  The type of the entities managed by the entity manager.
 *             These entities are assumed to implement the {@link Entity} interface with identifiers of type {@code ID}.
 */
abstract class AbstractEntityManager<ID, E extends Entity<ID>>
        implements EntityManager<ID, E>
{
    /**
     * {@inheritDoc}
     *
     * @param entity The entity to persist.
     * @return The ID of the persisted entity.
     * @throws Exception If the persistence fails.
     */
    @Override
    public @NonNull ID persist( @NonNull final E entity ) throws Exception
    {
        return persistAll( List.of( entity ) ).stream()
                                              .findFirst()
                                              .orElseThrow();
    }

    /**
     * {@inheritDoc}
     *
     * @param entity The entity to merge.
     * @throws Exception If the merge fails.
     */
    @Override
    public void update( @NonNull final E entity ) throws Exception
    {
        updateAll( List.of( entity ) );
    }

    /**
     * {@inheritDoc}
     *
     * @param id The ID of the entity to remove.
     * @throws Exception If the removal fails.
     */
    @Override
    public void deleteById( @NonNull final ID id ) throws Exception
    {
        deleteAllByIds( Set.of( id ) );
    }

    /**
     * {@inheritDoc}
     *
     * @param id The ID of the entity to find.
     * @return An Optional containing the entity if found, or empty if not found.
     * @throws Exception If the find fails.
     */
    @Override
    public @NonNull Optional<E> findById( @NonNull final ID id ) throws Exception
    {
        return findAllByIds( Set.of( id ) ).stream()
                                           .findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @param id The ID of the entity to find.
     * @return The entity if found.
     * @throws Exception If the entity is not found or find fails.
     */
    @Override
    public @NonNull E findByIdOrThrow( @NonNull final ID id ) throws Exception
    {
        return findById( id ).orElseThrow();
    }

    /**
     * {@inheritDoc}
     *
     * @param supplier The supplier providing the where clause.
     * @return An Optional containing the entity if found, or empty if not found.
     * @throws Exception If the find fails.
     */
    @Override
    public @NonNull Optional<E> findOne( @NonNull final Supplier<WhereClause> supplier ) throws Exception
    {
        return findAll( supplier ).stream()
                                  .findFirst();
    }

    /**
     * {@inheritDoc}
     *
     * @param supplier The supplier providing the where clause.
     * @return The entity if found.
     * @throws Exception If the entity is not found or find fails.
     */
    @Override
    public @NonNull E findOneOrThrow( @NonNull final Supplier< WhereClause > supplier ) throws Exception
    {
        return findOne( supplier ).orElseThrow();
    }

    /**
     * {@inheritDoc}
     *
     * @param id       The ID of the entity to retrieve.
     * @param consumer The consumer to modify the select query.
     * @return A DataContainer containing the retrieved data.
     * @throws Exception If the retrieval fails.
     */
    @Override
    public @NonNull DataContainer customFindById( @NonNull final ID id,
                                                  @NonNull final SafeConsumer<SelectQuery> consumer ) throws Exception
    {
        return customFindAllByIds( Set.of( id ), consumer );
    }

    /**
     * {@inheritDoc}
     *
     * @param id       The ID of the entity to update.
     * @param consumer The consumer to modify the update query.
     * @throws Exception If the update fails.
     */
    @Override
    public void customUpdateById( @NonNull final ID id, @NonNull final SafeConsumer<UpdateQuery> consumer ) throws
            Exception
    {
        customUpdateAllByIds( Set.of( id ), consumer );
    }

    /**
     * {@inheritDoc}
     *
     * @param id       The ID of the entity to delete.
     * @param consumer The consumer to modify the delete query.
     * @throws Exception If the deletion fails.
     */
    @Override
    public void customDeleteById( @NonNull final ID id, @NonNull final SafeConsumer<DeleteQuery> consumer ) throws
            Exception
    {
        customDeleteAllByIds( Set.of( id ), consumer );
    }
}
