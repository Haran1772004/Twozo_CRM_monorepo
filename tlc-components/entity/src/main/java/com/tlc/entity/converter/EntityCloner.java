package com.tlc.entity.converter;

import com.tlc.entity.Entity;

import lombok.NonNull;

/**
 * Defines a contract for cloning an entity by creating a copy of the provided source entity.
 *
 * @param <ID> The type of the entity's identifier.
 * @param <E>  The type of the entity to be cloned.
 */
public interface EntityCloner<ID, E extends Entity<ID>>
{
    /**
     * Creates a clone of the given source entity.
     *
     * @param sourceEntity The entity to be cloned. Must not be {@code null}.
     * @return A new instance of the entity that is a copy of the source entity.
     */
    E clone( final @NonNull E sourceEntity );
}
