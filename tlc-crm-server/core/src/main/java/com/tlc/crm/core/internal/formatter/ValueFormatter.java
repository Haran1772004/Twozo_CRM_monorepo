package com.tlc.crm.core.internal.formatter;

/**
 * Formatter is designed to format a value of type <V> using an instance of <F>,
 * resulting in a formatted output of type <R> based on the desired formatting approach
 *
 * @param <F> The formatting object that can be used to format
 * @param <V> The value to be formatted
 * @param <R> The resulting type after formatting
 * @author sundar
 * @version 1.0
 */
@FunctionalInterface
public interface ValueFormatter<F, V, R> {

    /**
     * Formats the value according to the specified formatting object and returns the formatted value
     *
     * @param formatWith Represents the object that can be used to format the value
     * @param value      The value to be formatted
     * @param orgId      Represents the organization id
     * @return The formatted value
     */
    R format(final long orgId, final F formatWith, final V value);
}
