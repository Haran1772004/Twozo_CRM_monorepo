package com.tlc.commons.functions;

/**
 * Functional interface for a consumer that can handle checked exceptions.
 *
 * @param <T> The type of the first input to the operation.
 * @param <U> The type of the second input to the operation.
 */
@FunctionalInterface
public interface SafeBiConsumer<T, U>
{
    /**
     * Performs this operation on the given arguments, potentially throwing an exception.
     *
     * @param value1 The first input argument.
     * @param value2 The second input argument.
     * @throws Exception if an exception occurs during the operation.
     */
    void accept( final T value1, final U value2 ) throws Exception;
}
