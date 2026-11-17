package com.tlc.pk.export;

import lombok.NonNull;

/**
 * Exception thrown to indicate an error occurred during data export operations.
 *
 * <p>This exception is used to signal failures related to exporting data, such as issues with
 * formatting, writing to a file, or unsupported data structures. It extends {@code Exception},
 * making it a checked exception that must be handled explicitly.
 */
public class ExportException extends Exception
{

    public ExportException( final @NonNull String message )
    {
        super( message );
    }

    public ExportException( final @NonNull Throwable t )
    {
        super( t );
    }

}
