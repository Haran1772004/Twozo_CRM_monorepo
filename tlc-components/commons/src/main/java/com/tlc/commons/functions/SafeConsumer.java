package com.tlc.commons.functions;

/**
 * Functional interface for a consumer that can handle checked exceptions.
 *
 * @param <V> The type of the input to the operation.
 */
@FunctionalInterface
public interface SafeConsumer<V>
{
    /**
     * Performs this operation on the given argument, potentially throwing an exception.
     *
     * @param value The input argument.
     * @throws Exception if an exception occurs during the operation.
     */
    void accept( final V value ) throws Exception;
}
