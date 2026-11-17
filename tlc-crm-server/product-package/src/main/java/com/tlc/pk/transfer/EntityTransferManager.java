package com.tlc.pk.transfer;

import com.tlc.commons.functions.SafeFunction;

import com.tlc.entity.Entity;

import com.tlc.pk.transfer.model.EntityTransferConfig;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * <p>
 * Manages the process of moving entities based on the given configurations
 * </p>
 *
 * @param <ID> The type of the entity's identifier.
 * @param <E>  The type of the entity extending from the base {@link Entity} class.
 * @author pandiarajkumar
 * @version 1.0
 */
@NoArgsConstructor( access = AccessLevel.PROTECTED )
class EntityTransferManager<ID, E extends Entity<ID>> extends AbstractTransferManager<EntityTransferConfig<ID, E>>
{
    private static class Instance
    {
        private static final EntityTransferManager<?, ?> INSTANCE = new EntityTransferManager<>();
    }

    @SuppressWarnings( "unchecked" )
    public static <ID, E extends Entity<ID>> EntityTransferManager<ID, E> getInstance()
    {
        return ( EntityTransferManager<ID, E> ) Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param config The configuration containing details about the move operation.
     * @throws Exception
     */
    @Override
    protected void moveTo( final @NonNull EntityTransferConfig<ID, E> config ) throws Exception
    {
        final E entity = config.getEntityManager()
                               .findByIdOrThrow( config.getSourceId() );
        final SafeFunction<E, E> function = config.getEntityCustomizer();

        config.getEntityManager()
              .update( function.apply( entity ) );
    }
}
