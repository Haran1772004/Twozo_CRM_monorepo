package com.tlc.pk.copy;

import lombok.NonNull;

/**
 * <p>
 * Handles the copy operation of before and after operation
 * </p>
 *
 * @author pandiarajkumar G
 * @version 1.0
 */
public interface CopyHandler<I>
{
    /**
     * <p>
     * Performs before the start of the copy process
     * </p>
     *
     * @param originalItem Represents the object to be copied
     */
    void beforeCopy( final @NonNull I originalItem );

    /**
     * <p>
     * Performs after the start of the copy process
     * </p>
     *
     * @param copiedItem Represents the copied object
     */
    void afterCopy( final @NonNull I copiedItem );

    /**
     * <p>
     * Implements of the {@link DefaultHandler} interface that provides functionality for handling default copy operation
     * </p>
     *
     * @author pandiarajkumar G
     */
    class DefaultHandler<I> implements CopyHandler<I>
    {
        /**
         * {@inheritDoc}
         */
        @Override
        public void beforeCopy( final @NonNull I originalItem )
        {}

        /**
         * {@inheritDoc}
         */
        @Override
        public void afterCopy( final @NonNull I copiedItem )
        {}
    }
}


