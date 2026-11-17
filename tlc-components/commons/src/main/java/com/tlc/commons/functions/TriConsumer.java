package com.tlc.commons.functions;

/**
 * Functional interface for a consumer with three arguments.
 *
 * @param <T> The type of the first input to the operation.
 * @param <U> The type of the second input to the operation.
 * @param <V> The type of the third input to the operation.
 */
@FunctionalInterface
public interface TriConsumer<T, U, V>
{
    /**
     * Performs this operation on the given arguments.
     *
     * @param i1 The first input argument.
     * @param i2 The second input argument.
     * @param i3 The third input argument.
     */
    void accept( final T i1, final U i2, final V i3 );
}
