package com.tlc.email.common.converter;

/**
 * <p>
 * A generic interface to convert data between different representations.
 * </p>
 *
 * @param <T> The source type for conversion.
 * @param <R> The target type after conversion.
 * @author KowsalyaSP
 * @version 1.0
 */
public interface Converter<T, R> {

    /**
     * <p>
     * Converts the given data to the target type.
     * </p>
     *
     * @param data The source data to convert.
     * @return The converted result of type {@code R}.
     */
    R convert(T data);
}
