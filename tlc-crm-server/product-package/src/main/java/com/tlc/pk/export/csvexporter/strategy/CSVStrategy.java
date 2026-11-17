package com.tlc.pk.export.csvexporter.strategy;

import lombok.NonNull;

/**
 * Defines the strategy for formatting CSV output, including delimiter, enclosing character,
 * and line termination sequence.
 *
 * <p>Implementations of this interface provide specific configurations for CSV generation,
 * allowing customization of CSV formatting rules.</p>
 *
 * <p>Typical implementations include default strategies and custom variations based on
 * specific export requirements.</p>
 */
public interface CSVStrategy
{

    /**
     * Gets the delimiter character used to separate values in the CSV output.
     *
     * @return the delimiter character
     */
    char getDelimiter();

    /**
     * Gets the character used to enclose values in the CSV output, typically for values
     * containing special characters such as the delimiter or line breaks.
     *
     * @return the enclosing character
     */
    char getEnclosedBy();

    /**
     * Gets the string used to represent an escaped enclosing character in the CSV output.
     * This is typically used when a value contains the enclosing character itself, requiring
     * it to be escaped by doubling it (e.g., "example""text" in CSV).
     *
     * @return the escaped enclosing character as a string
     */
    String getDoubleEnclosedBy();

    /**
     * Gets the sequence of characters used to terminate lines in the CSV output.
     *
     * @return the line termination sequence as a {@code String}
     */
    @NonNull String getLinesTerminatedBy();

    /**
     * Gets the escaped representation of the line termination sequence.
     * Unlike {@link #getLinesTerminatedBy()}, this method returns the string representation
     * of the newline character as "\\n", which is useful for displaying or debugging purposes.
     *
     * @return the escaped line termination sequence as a {@code String}, typically {@code "\\n"}
     */
    @NonNull String getEscapedLinesTerminatedBy();

    /**
     * Gets the character used as a delimiter for tab-separated values (TSV) output.
     * This is typically the tab character ('\t'), used to separate fields in a row.
     *
     * @return the tab delimiter as a {@code String}
     */
    @NonNull String getTabDelimiter();

    /**
     * Gets the escaped representation of the tab delimiter.
     * Unlike {@link #getTabDelimiter()}, this method returns the string representation
     * of the tab character (e.g., "\\t"), which is useful for displaying or debugging purposes.
     *
     * @return the escaped tab delimiter as a {@code String}, typically {@code "\\t"}
     */
    @NonNull String getEscapedTabDelimiter();

}
