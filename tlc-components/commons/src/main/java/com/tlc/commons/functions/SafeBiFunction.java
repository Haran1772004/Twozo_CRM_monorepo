package com.tlc.commons.functions;

/**
 * Functional interface for a function that can handle checked exceptions.
 *
 * @param <T> The type of the first input to the function.
 * @param <U> The type of the second input to the function.
 * @param <R> The type of the result of the function.
 */
@FunctionalInterface
public interface SafeBiFunction<T, U, R>
{
    /**
     * Applies this function to the given arguments, potentially throwing an exception.
     *
     * @param i1 The first input argument.
     * @param i2 The second input argument.
     * @return The result of the function.
     * @throws Exception if an exception occurs during the operation.
     */
    R apply( final T i1, final U i2 ) throws Exception;
}
