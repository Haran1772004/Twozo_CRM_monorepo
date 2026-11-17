package com.tlc.pk.copy;

import com.tlc.sql.common.api.DataContainer;

import lombok.NonNull;

/**
 * Abstract class that defines a template for performing copy operations
 * for various types such as files, table data, objects, entities, metadata, etc.
 *
 * @param <I> the type of the object to be copied
 * @param <CC> the type of the configuration used for the copy operation
 */
public abstract class AbstractCopyManager<I, CC extends CopyConfig<I>> implements CopyManager<CC>
{
    /**
     * Executes the copy operation by validating the configuration and then performing the copy process.
     * This method is final to ensure a consistent flow of validation and copying across all implementations.
     *
     * @param copyConfig the configuration required to perform the copy operation, must not be null
     * @param dataContainer    Holds the rows that will get committed
     * @throws Exception if validation fails or an error occurs during the copy process
     */
    @Override
    public final void copy( @NonNull final CC copyConfig, final DataContainer dataContainer ) throws Exception
    {
        try
        {
            validateConfiguration( copyConfig );

            final I originalItem = copyConfig.getOriginalItem();

            copyConfig.getCopyHandler()
                      .beforeCopy( originalItem );

            final I copiedItem = performCopy( originalItem, copyConfig, dataContainer );

            copyConfig.getCopyHandler()
                      .afterCopy( copiedItem );
        }
        catch( Exception exception )
        {
            handleCopyException( copyConfig, exception );
        }
    }

    /**
     * Validates the copy configuration and throws an exception if it is invalid.
     * Subclasses should override this method to provide specific validation logic.
     *
     * @param copyConfig the configuration to be validated
     * @throws IllegalArgumentException if the configuration is invalid
     * @throws Exception                if any other error occurs during validation
     */
    protected void validateConfiguration( final CC copyConfig ) throws Exception
    {
        // Default implementation does nothing. Subclasses should provide custom validation logic.
    }

    /**
     * Handles exceptions that occur during the copy process. Provides a centralized
     * mechanism for error handling, such as logging or retrying. Subclasses can override
     * this method to define custom behavior.
     *
     * @param copyConfig the configuration for which the error occurred
     * @param exception  the exception that was thrown during the copy process
     * @throws Exception if the error needs to be rethrown (default behavior)
     */
    protected void handleCopyException( final CC copyConfig, final Exception exception ) throws Exception
    {
        // Default behavior: rethrow the exception. Subclasses can override to add custom handling.
        throw exception;
    }

    /**
     * Defines the core logic for performing the copy operation.
     * Subclasses must implement this method to specify how the data, files, objects, or entities should be copied.
     *
     * @param originalItem the original object to be copied
     * @param copyConfig   the configuration required to perform the copy operation
     * @return the copied object
     * @throws Exception if any error occurs during the copy process
     */
    protected abstract I performCopy( final I originalItem, final CC copyConfig, final DataContainer dataContainer ) throws Exception;
}