package com.tlc.pk.export.csvexporter;

import com.tlc.pk.export.ExportException;
import lombok.NonNull;

/**
 * Exception thrown to indicate an error occurred during CSV export operations.
 *
 * <p>This exception extends {@code ExportException} and is specifically used for failures
 * encountered while exporting data to CSV format. Possible causes include issues with data
 * formatting, file writing errors, or invalid CSV structure.
 */
public class CSVExportException extends ExportException
{

    public CSVExportException( final @NonNull String message )
    {
        super( message );
    }

    public CSVExportException( final @NonNull Throwable t )
    {
        super( t );
    }

}
