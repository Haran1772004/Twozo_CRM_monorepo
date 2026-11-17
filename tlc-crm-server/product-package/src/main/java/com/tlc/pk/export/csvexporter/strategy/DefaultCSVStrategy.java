package com.tlc.pk.export.csvexporter.strategy;

import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NonNull;

/**
 * Provides a default implementation of {@code CSVStrategy} with standard CSV formatting rules.
 *
 * <p>This class defines commonly used CSV conventions, including default delimiter,
 * escape character, and line ending. It serves as a pre-configured
 * strategy for general CSV writing needs.</p>
 *
 * <p>Applications can use this class directly or extend it to customize CSV formatting
 * based on specific requirements.</p>
 */
@Getter
class DefaultCSVStrategy implements CSVStrategy
{

    private static class Instance
    {

        private static final CSVStrategy INSTANCE = new DefaultCSVStrategy( DEFAULT_DELIMITER,
                                                                            DEFAULT_ENCLOSED_BY,
                                                                            DEFAULT_DOUBLE_ENCLOSED_BY,
                                                                            DEFAULT_LINES_TERMINATED_BY,
                                                                            DEFAULT_ESCAPED_LINES_TERMINATED_BY,
                                                                            DEFAULT_TAB_DELIMITER,
                                                                            DEFAULT_ESCAPED_TAB_DELIMITER);

    }

    private final char delimiter;
    final char enclosedBy;
    final String doubleEnclosedBy;
    final @NotNull String linesTerminatedBy;
    final @NotNull String escapedLinesTerminatedBy;
    final @NotNull String tabDelimiter;
    final @NotNull String escapedTabDelimiter;

    private static final char DEFAULT_DELIMITER = ',';
    private static final char DEFAULT_ENCLOSED_BY = '\"';
    private static final String DEFAULT_DOUBLE_ENCLOSED_BY = "\"\"";
    private static final @NotNull String DEFAULT_LINES_TERMINATED_BY = "\n";
    private static final @NotNull String DEFAULT_ESCAPED_LINES_TERMINATED_BY = "\\n";
    private static final @NotNull String DEFAULT_TAB_DELIMITER = "\t";
    private static final @NotNull String DEFAULT_ESCAPED_TAB_DELIMITER = "\\t";

    private DefaultCSVStrategy( final char delimiter, final char enclosedBy, final String doubleEnclosedBy,
                                final @NonNull String linesTerminatedBy, final @NonNull String escapedLinesTerminatedBy,
                                final @NonNull String tabDelimiter, final @NonNull String escapedTabDelimiter )
    {
        this.delimiter = delimiter;
        this.enclosedBy = enclosedBy;
        this.doubleEnclosedBy = doubleEnclosedBy;
        this.linesTerminatedBy = linesTerminatedBy;
        this.escapedLinesTerminatedBy = escapedLinesTerminatedBy;
        this.tabDelimiter = tabDelimiter;
        this.escapedTabDelimiter = escapedTabDelimiter;
    }

    /**
     * Returns the singleton instance of {@code DefaultCSVStrategy}.
     *
     * <p>This method ensures that only one instance of {@code DefaultCSVStrategy} is created
     * using a static inner class for lazy initialization.
     *
     * @return the singleton instance of {@code DefaultCSVStrategy}.
     */
    public static @NonNull CSVStrategy getInstance()
    {
        return Instance.INSTANCE;
    }

}
