package com.tlc.pk.export.csvexporter.writer;

/**
 * Defines the contract for writing data in CSV format.
 *
 * <p>This interface provides methods for handling CSV output, including writing headers,
 * data rows, and managing the underlying output stream or file.</p>
 *
 * <p>Implementations of this interface are responsible for ensuring that data is correctly
 * formatted and written in compliance with CSV standards, handling delimiters, escaping,
 * and encoding where necessary.</p>
 */
public interface CSVWriter
{

    /**
     * Writes CSV data to the configured output stream.
     *
     * <p>Implementations of this method should format the data according to the specified
     * {@code CSVWriteOptions} and write it to the provided {@code OutputStream}.</p>
     *
     * @throws CSVWriteException if an error occurs while writing to the output stream.
     */
    void writeCSV() throws CSVWriteException;

}