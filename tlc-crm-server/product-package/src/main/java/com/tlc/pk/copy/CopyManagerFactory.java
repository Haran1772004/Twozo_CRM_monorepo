package com.tlc.pk.copy;

import com.tlc.entity.Entity;

/**
 * Factory class for creating instances of various copy managers.
 */
public class CopyManagerFactory
{
    /**
     * Returns the singleton instance of the {@link EntityCopyManager} for copying entities.
     *
     * @param <ID> The type of the entity's identifier.
     * @param <E>  The type of the entity being copied.
     * @return The {@link EntityCopyManager} instance.
     */
    public static <ID, E extends Entity<ID>> CopyManager<EntityCopyConfig<ID, E>> getEntityCopyManager()
    {
        return EntityCopyManager.INSTANCE();
    }
}
