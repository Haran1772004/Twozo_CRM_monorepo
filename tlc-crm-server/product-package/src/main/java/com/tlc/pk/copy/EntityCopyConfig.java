package com.tlc.pk.copy;

import com.tlc.commons.functions.SafeFunction;
import com.tlc.commons.util.executor.CodeBlockExecutor;
import com.tlc.entity.Entity;
import com.tlc.entity.EntityManager;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

/**
 * Configuration for copying an entity from a source to a target.
 *
 * @param <ID> The type of the entity's identifier.
 * @param <E>  The type of the entity.
 */
@Getter
@SuperBuilder
public final class EntityCopyConfig<ID, E extends Entity<ID>> extends CopyConfig<E>
{
    /**
     * Manages database operations for the entity.
     */
    @NonNull
    private final EntityManager<ID, E> entityManager;

    /**
     * Customizes the copied entity before persisting.
     */
    @NonNull
    private final SafeFunction<E, E> entityCustomizer;

    /**
     * The ID of the source entity to be copied.
     */
    @NonNull
    private final ID sourceId;

    /**
     * Retrieves the original entity that needs to be copied by fetching it from the data source
     * using its identifier. Ensures the entity exists or throws an exception if not found.
     *
     * @return the original entity to be copied, never null
     */
    @Override
    protected @NonNull E getOriginalItem()
    {
        return CodeBlockExecutor.getWithUnchecked( () -> getEntityManager().findByIdOrThrow( sourceId ) );
    }
}