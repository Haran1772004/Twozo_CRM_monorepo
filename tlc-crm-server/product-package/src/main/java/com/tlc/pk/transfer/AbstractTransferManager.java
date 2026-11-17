package com.tlc.pk.transfer;

import lombok.NonNull;

/**
 * <p>
 * Provides an abstract way to configure the {@link AbstractTransferManager}
 * </p>
 *
 * @param <T> The type of the configuration used for the copy operation
 * @author pandiarajkumarG
 * @version 1.0
 */
public abstract class AbstractTransferManager<T> implements TransferManager<T>
{
    /**
     * <p>
     * Executes the move operation based on the provided configuration.
     * </p>
     *
     * @param config The configuration containing details about the move operation.
     */
    @Override
    public void move( final @NonNull T config ) throws Exception
    {
        moveTo( config );
    }

    /**
     * <p>
     * Executes the move operation based on the provided configuration.
     * </p>
     *
     * @param config The configuration containing details about the move operation.
     */
    protected abstract void moveTo( final @NonNull T config ) throws Exception;
}
