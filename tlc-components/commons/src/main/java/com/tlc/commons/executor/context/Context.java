package com.tlc.commons.executor.context;

/**
 * Denotes contextual value
 *
 * @author sundar
 * @version  1.0
 */
public interface Context {

    /**
     * Gets the type of the context
     *
     * @return {@link Type} that represents the type of the context
     */
    Type getType();

    /**
     * Creates a clone of the context
     *
     * @return a new instance of the context, identical to the original
     */
    Context copy();

    /**
     * Represents the type of the context
     *
     * @author sundar
     * @version 1.0
     */
    interface Type {

        /**
         * Gets the type name
         *
         * @return the type name
         */
        String getName();
    }
}
