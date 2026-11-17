package com.tlc.crm.entity.export.internal.formatter;

/**
 * <p>
 * Holds the data export format patterns
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
final class DataExportFormatPatterns {

    private DataExportFormatPatterns() {

    }

    public static final String DATE_FORMAT_PATTERN = "dd/MM/yyyy";
    public static final String TIME_FORMAT_PATTERN = "hh:mm a";
    public static final String DATE_TIME_FORMAT_PATTERN =
            String.format("%s %s", DATE_FORMAT_PATTERN, TIME_FORMAT_PATTERN);
    public static final String CONCATENATION_PATTERN = "%s%s%s";
}
