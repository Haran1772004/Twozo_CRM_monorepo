package com.tlc.pk.export.options;

/**
 * Defines configuration options for data export operations.
 *
 * <p>This interface represents a set of options that control various aspects of data export,
 * such as formatting, output destination, and other export-related preferences.</p>
 *
 * <p>Implementing classes provide specific export configurations for different formats
 * such as CSV, PDF.</p>
 */
public interface ExportOptions
{

    int getExportType();

}
