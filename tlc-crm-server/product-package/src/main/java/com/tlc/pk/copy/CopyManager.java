package com.tlc.pk.copy;

import com.tlc.sql.common.api.DataContainer;

import lombok.NonNull;

/**
 * Interface for managing copy operations for a specific type of configuration.
 *
 * @param <CC> the type of the configuration used for the copy operation
 */
public interface CopyManager<CC>
{
    /**
     * Executes the copy operation based on the provided configuration.
     *
     * @param copyConfig the configuration required to perform the copy operation, must not be null
     * @param dataContainer    Holds the rows that will get committed
     * @throws Exception if any error occurs during the copy process
     */
    void copy( final @NonNull CC copyConfig, final DataContainer dataContainer ) throws Exception;
}