package com.tlc.commons.functions;

/**
 * Functional interface for a function that can handle checked exceptions with three arguments.
 *
 * @param <T> The type of the first input to the function.
 * @param <U> The type of the second input to the function.
 * @param <V> The type of the third input to the function.
 * @param <R> The type of the result of the function.
 */
@FunctionalInterface
public interface SafeTriFunction<T, U, V, R>
{
    /**
     * Applies this function to the given arguments, potentially throwing an exception.
     *
     * @param i1 The first input argument.
     * @param i2 The second input argument.
     * @param i3 The third input argument.
     * @return The result of the function.
     * @throws Exception if an exception occurs during the operation.
     */
    R apply( final T i1, final U i2, final V i3 ) throws Exception;
}
