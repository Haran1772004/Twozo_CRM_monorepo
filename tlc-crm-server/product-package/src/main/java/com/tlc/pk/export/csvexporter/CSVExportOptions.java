package com.tlc.pk.export.csvexporter;

import com.tlc.pk.export.csvexporter.strategy.CSVStrategy;
import com.tlc.pk.export.csvexporter.strategy.CSVStrategyFactory;
import com.tlc.pk.export.csvexporter.writer.CSVWriteOptions;
import com.tlc.pk.export.options.AbstractExportOptions;
import com.tlc.pk.export.options.ExportOptions;

import jakarta.validation.constraints.NotNull;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.io.OutputStream;

/**
 * Configuration options for exporting data in CSV format.
 *
 * <p>This class extends {@code AbstractExportOptions} and provides additional
 * settings specific to CSV export, such as formatting rules and output
 * stream handling.</p>
 */
@SuperBuilder
@Getter
public class CSVExportOptions extends AbstractExportOptions implements ExportOptions
{

    @Builder.Default
    private final CSVStrategy csvStrategy = CSVStrategyFactory.getDefaultStrategy();

    @Builder.Default
    private final boolean includeHeaders = true;

    @NotNull
    private final OutputStream oStream;

    @NotNull
    private final CSVWriteOptions writeOptions;

}
