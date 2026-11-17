package com.tlc.pk.export.csvexporter.writer;

import lombok.NonNull;

/**
 * Exception thrown when an error occurs during CSV writing.
 *
 * <p>This exception is used to indicate failures related to writing CSV data,
 * such as issues with formatting, escaping, or output stream errors.</p>
 */
public class CSVWriteException extends Exception
{

    public CSVWriteException( final @NonNull String message )
    {
        super( message );
    }

    public CSVWriteException( final @NonNull Throwable t )
    {
        super( t );
    }

}
