package com.tlc.commons.functions;

/**
 * Functional interface for a consumer that can handle checked exceptions with three arguments.
 *
 * @param <T> The type of the first input to the operation.
 * @param <U> The type of the second input to the operation.
 * @param <V> The type of the third input to the operation.
 */
public interface SafeTriConsumer<T, U, V>
{
    /**
     * Functional interface for a consumer that can handle checked exceptions with three arguments.
     *
     * @param i1 The type of the first input to the operation.
     * @param i2 The type of the second input to the operation.
     * @param i3 The type of the third input to the operation.
     */
    void accept( final T i1, final U i2, final V i3 ) throws Exception;
}
