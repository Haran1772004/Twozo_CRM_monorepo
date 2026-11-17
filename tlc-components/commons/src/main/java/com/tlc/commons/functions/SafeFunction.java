package com.tlc.commons.functions;

/**
 * Functional interface for a function that can handle checked exceptions.
 *
 * @param <I> The type of the input to the function.
 * @param <R> The type of the result of the function.
 */
@FunctionalInterface
public interface SafeFunction<I, R>
{
    /**
     * Applies this function to the given argument, potentially throwing an exception.
     *
     * @param input The input argument.
     * @return The result of the function.
     * @throws Exception if an exception occurs during the operation.
     */
    R apply( final I input ) throws Exception;
}
