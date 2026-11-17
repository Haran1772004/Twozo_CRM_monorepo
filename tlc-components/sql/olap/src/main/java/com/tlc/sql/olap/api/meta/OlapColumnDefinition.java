package com.tlc.sql.olap.api.meta;

import com.tlc.sql.common.api.meta.AbstractColumnDefinition;
import com.tlc.sql.common.api.meta.DataType;

/**
 * OLAP-specific implementation of {@link AbstractColumnDefinition}.
 * Represents a column definition used in OLAP (Online Analytical Processing) database tables.
 */
public class OlapColumnDefinition extends AbstractColumnDefinition {

    /**
     * Constructs an OLAP column definition with the given name and data type,
     * using default length, no default value, and non-nullable.
     *
     * @param columnName the name of the column, must not be {@code null}
     * @param dataType   the data type of the column, must not be {@code null}
     */
    public OlapColumnDefinition(final String columnName, final DataType dataType) {
        super(columnName, dataType);
    }

    /**
     * Constructs an OLAP column definition with all properties specified.
     *
     * @param columnName   the name of the column, must not be {@code null}
     * @param dataType     the data type of the column, must not be {@code null}
     * @param maxLength    the maximum length of the column, must not be negative
     * @param defaultValue the default value for the column, or {@code null} if none
     * @param nullable     whether the column allows {@code null} values
     */
    public OlapColumnDefinition(final String columnName, final DataType dataType, final int maxLength,
                                final Object defaultValue, final boolean nullable) {
        super(columnName, dataType, maxLength, defaultValue, nullable);
    }
}