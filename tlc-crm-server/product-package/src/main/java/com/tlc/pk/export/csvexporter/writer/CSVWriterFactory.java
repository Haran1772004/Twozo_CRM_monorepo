package com.tlc.pk.export.csvexporter.writer;

import lombok.NonNull;

/**
 * Factory interface for creating instances of {@code CSVWriter}.
 *
 * <p>Implementations of this interface are responsible for providing configured
 * instances of {@code CSVWriter} based on specific requirements.</p>
 */
public interface CSVWriterFactory
{

    /**
     * Creates a new instance of {@code CSVWriter} using the provided write options.
     *
     * <p>This method returns a configured {@code CSVWriter} instance that writes data
     * based on the given {@code CSVWriteOptions}. The specific implementation of
     * {@code CSVWriter} returned may vary depending on the factory's configuration.</p>
     *
     * @param o the CSV writing options that define formatting rules and output destination (must not be {@code null})
     * @return a fully configured {@code CSVWriter} instance (never {@code null})
     */
    static @NonNull CSVWriter getWriter( final @NonNull CSVWriteOptions o )
    {
        return new DefaultCSVWriter( o );
    }

}
