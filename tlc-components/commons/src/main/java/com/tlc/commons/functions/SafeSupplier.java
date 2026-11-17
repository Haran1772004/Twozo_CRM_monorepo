package com.tlc.commons.functions;

/**
 * Functional interface for a supplier that can handle checked exceptions.
 *
 * @param <V> The type of the result supplied by this supplier.
 */
@FunctionalInterface
public interface SafeSupplier<V>
{
    /**
     * Gets a result, potentially throwing an exception.
     *
     * @return The result.
     * @throws Exception if an exception occurs during execution.
     */
    V get() throws Exception;
}
