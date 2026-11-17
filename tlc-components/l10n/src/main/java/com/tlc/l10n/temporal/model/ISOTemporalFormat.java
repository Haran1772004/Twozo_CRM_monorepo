package com.tlc.l10n.temporal.model;

/**
 * Represents supported ISO 8601 temporal formats with timezone offset.
 * These formats are used for formatting and parsing date/time values with offsets.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ISOTemporalFormat {

    /**
     * ISO 8601 format that includes date, time, and offset.
     * Example: {@code 2025-05-16T14:23:00+05:30}
     */
    ISO_OFFSET_DATE_TIME,

    /**
     * ISO 8601 format that includes only time and offset.
     * Example: {@code 14:23:00+05:30}
     */
    ISO_OFFSET_TIME,

    /**
     * ISO 8601 format that includes only date and offset.
     * Example: {@code 2025-05-16+05:30}
     */
    ISO_OFFSET_DATE
}
