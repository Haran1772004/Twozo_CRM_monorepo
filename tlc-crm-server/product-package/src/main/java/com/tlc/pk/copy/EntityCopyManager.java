package com.tlc.pk.copy;

import com.tlc.entity.Entity;
import com.tlc.entity.EntityManager;

import com.tlc.sql.common.api.DataContainer;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Manages the copying of entities from a source to a target by creating a new instance
 * of the entity, updating its fields if necessary, and persisting the copied entity.
 *
 * @param <ID> The type of the entity's identifier.
 * @param <E>  The type of the entity extending from the base {@link Entity} class.
 */
@NoArgsConstructor( access = AccessLevel.PRIVATE )
class EntityCopyManager<ID, E extends Entity<ID>> extends AbstractCopyManager<E, EntityCopyConfig<ID, E>>
{
    /**
     * A private static class that holds the singleton instance of {@link EntityCopyManager}.
     * This ensures lazy initialization and thread-safety for accessing the instance.
     */
    private static class Instance
    {
        private static final EntityCopyManager<?, ?> INSTANCE = new EntityCopyManager<>();
    }

    /**
     * A static factory method to access the singleton instance of {@link EntityCopyManager}.
     * This method allows type-safe casting to the desired generic types.
     *
     * @param <ID> Type representing the identifier of the entity.
     * @param <E>  Type representing the entity, constrained to extend Entity<ID>.
     * @return The singleton instance of {@link EntityCopyManager} typed with the provided generics.
     */
    @SuppressWarnings( "unchecked" )
    public static <ID, E extends Entity<ID>> EntityCopyManager<ID, E> INSTANCE()
    {
        return ( EntityCopyManager<ID, E> ) Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param copyConfig the configuration required to perform the copy operation
     * @param dataContainer    Holds the rows that will get committed
     * @throws Exception if any error occurs during the copy process
     */
    @Override
    protected final E performCopy( final E originalEntity, final EntityCopyConfig<ID, E> copyConfig,
                                   final DataContainer dataContainer ) throws Exception
    {
        final EntityManager<ID, E> entityManager = copyConfig.getEntityManager();

        final E copiedEntity = copyConfig.getEntityCustomizer()
                                         .apply( originalEntity );

        entityManager.loadDataIntoContainer( List.of( copiedEntity ), dataContainer );

        return copiedEntity;
    }
}