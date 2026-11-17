package com.tlc.pk.export.csvexporter.writer;

import lombok.NonNull;

class BatchedCSVWriter extends DefaultCSVWriter
{

    public BatchedCSVWriter( final @NonNull CSVWriteOptions writeOptions )
    {
        super( writeOptions );
    }

}
