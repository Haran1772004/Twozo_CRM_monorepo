package com.tlc.pk.export;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Defines the available export types for analytics data.
 *
 * <p>This enum represents different formats in which analytics data can be exported,
 * such as CSV and PDF.</p>
 */
@Getter
@RequiredArgsConstructor( access = AccessLevel.PRIVATE )
public enum ExportType
{
    CSV( 1 ),
    PDF( 2 );

    private final int exportType;
}
