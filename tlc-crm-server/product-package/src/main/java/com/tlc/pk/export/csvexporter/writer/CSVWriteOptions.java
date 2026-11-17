package com.tlc.pk.export.csvexporter.writer;

import com.tlc.pk.export.csvexporter.data.Data;
import com.tlc.pk.export.csvexporter.strategy.CSVStrategy;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;

import java.io.OutputStream;

/**
 * Configuration options for writing CSV data.
 *
 * <p>This class defines various settings that control how CSV data is written,
 * including formatting strategy, headers, and the target output stream.</p>
 */
@Builder
@Getter
public class CSVWriteOptions
{

    @NotNull
    private final CSVStrategy csvStrategy;

    @Builder.Default
    private final boolean includeHeaders = true;

    @NotNull
    private final Data data;

    @NotNull
    private final OutputStream oStream;

}
