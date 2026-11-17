package com.tlc.pk.export.options;


import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum ExportTypes
{
    CSV( 1 ),

    PDF( 2 );

    private final int exportType;
}
