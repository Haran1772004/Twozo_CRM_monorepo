package com.tlc.pk.transfer;

import com.tlc.entity.Entity;

import com.tlc.pk.transfer.model.EntityTransferConfig;

/**
 * <p>
 * Factory class for creating instances of various transfer managers
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
public class TransferManagerFactory
{
    /**
     * <p>
     *     Returns the instance of the {@link EntityTransferManager} for transfer entities
     * </p>
     *
     * @param <ID> The type of the entity's identifier
     * @param <E>  The type of the entity being transferred
     * @return The {@link EntityTransferManager} instance
     */
    public static <ID, E extends Entity<ID>> TransferManager<EntityTransferConfig<ID, E>> getEntityTransferManager()
    {
        return EntityTransferManager.getInstance();
    }
}
