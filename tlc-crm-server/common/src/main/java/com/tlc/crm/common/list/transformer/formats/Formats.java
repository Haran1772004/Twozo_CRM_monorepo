package com.tlc.crm.common.list.transformer.formats;

/**
 * <p>
 * Provides the list of formats which helps to format the required strings.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum Formats {

    COLUMN_FORMAT("%s_%s"),
    ;

    private final String format;

    Formats(final String format) {
        this.format = format;
    }

    public String get() {
        return format;
    }
}
