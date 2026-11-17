package com.tlc.commons.functions;

/**
 * Functional interface representing a tri-function that accepts three arguments and produces a result.
 *
 * @param <T> The type of the first argument.
 * @param <U> The type of the second argument.
 * @param <V> The type of the third argument.
 * @param <R> The type of the result.
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R>
{
    /**
     * Applies this tri-function to the given arguments.
     *
     * @param t The first argument.
     * @param u The second argument.
     * @param v The third argument.
     * @return The result of applying this tri-function to the provided arguments.
     */
    R apply( final T t, final U u,  final V v );
}