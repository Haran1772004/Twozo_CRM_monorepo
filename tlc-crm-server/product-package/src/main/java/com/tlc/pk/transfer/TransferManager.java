package com.tlc.pk.transfer;

/**
 * <p>
 * Performs the functionality for move operation based on the given type
 * </p>
 *
 * @param <T> The type of the configuration used for the copy operation
 * @author pandiarajkumarG
 * @version 1.0
 */
public interface TransferManager<T>
{
    /**
     * <p>
     * Executes the move operation based on the provided configuration.
     * </p>
     *
     * @param config The configuration containing details about the move operation.
     */
    void move( T config ) throws Exception;
}
