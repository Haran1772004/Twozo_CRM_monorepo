package com.tlc.pk.export.csvexporter;

import com.tlc.pk.export.AbstractExporter;
import com.tlc.pk.export.ExportException;
import com.tlc.pk.export.csvexporter.writer.CSVWriter;
import com.tlc.pk.export.csvexporter.writer.CSVWriterFactory;

import lombok.NonNull;

/**
 * Abstract base class for exporting data in CSV format.
 *
 * <p>This class provides the core export functionality for CSV-based exporters. It extends
 * {@link AbstractExporter} with {@link CSVExportOptions} as the configuration type.</p>
 *
 * <p>Subclasses must implement the specific details of how the CSV data is written using
 * the provided export options.</p>
 *
 * @see AbstractExporter
 * @see CSVExportOptions
 */
public abstract class CSVExporter extends AbstractExporter<CSVExportOptions>
{

    /**
     * Writes the provided data to an output stream in CSV format based on the given export options.
     *
     * <p>This method takes pre-formatted data from the client and writes it as a CSV file
     * according to the rules specified in {@code CSVExportOptions}. It ensures proper
     * formatting, escaping, and writing to the target {@code OutputStream}.</p>
     *
     * @param exportOptions the CSV export options defining formatting rules and output destination (must not be {@code null})
     * @throws ExportException if an error occurs during the export process
     */
    @Override
    protected final void export( final @NonNull CSVExportOptions exportOptions ) throws ExportException
    {
        try
        {
            final CSVWriter writer = CSVWriterFactory.getWriter( exportOptions.getWriteOptions() );
            writer.writeCSV();
        }
        catch( Exception exception)
        {
            throw new CSVExportException(exception);
        }
    }

}
