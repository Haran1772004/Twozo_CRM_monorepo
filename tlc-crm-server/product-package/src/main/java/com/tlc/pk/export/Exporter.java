package com.tlc.pk.export;

import com.tlc.pk.export.options.ExportOptions;
import lombok.NonNull;

/**
 * An interface for exporting data to a specific format.
 * <br> Implementations of this interface provide a way to take data and export it, potentially using different
 * export options.
 *
 * @param <O> The type of export options used by this exporter.  This allows
 *            for flexibility in configuring the export process.  The export
 *            options class should typically extend or implement a common
 *            interface or base class if there are multiple types of export
 *            options.
 */
public interface Exporter<O extends ExportOptions>
{

    /**
     * Performs the actual export operation using the provided export options.
     * This method contains the core logic for exporting the data. Implementations
     * should handle writing to the target destination.
     *
     * @param exportOptions The export options to use for the export. Must not be null.
     * @throws ExportException If an error occurs during the export process.
     */
    void doExport( final @NonNull O exportOptions ) throws ExportException;

}

