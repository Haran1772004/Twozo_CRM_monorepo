package com.tlc.sql.olap.api.meta;

import com.tlc.sql.common.api.meta.ArrayColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;

/**
 * OLAP-specific implementation of {@link ArrayColumnDefinition}.
 * Represents an array column definition used in OLAP database tables,
 * storing the element type separately while using {@link CommonDataType#ARRAY} as the column data type.
 */
public class OlapArrayColumnDefinition extends OlapColumnDefinition implements ArrayColumnDefinition {

    private final DataType elementType;

    /**
     * Constructs an OLAP array column definition with all properties specified.
     * The column's data type is set to {@link CommonDataType#ARRAY}, while the provided
     * {@code dataType} is stored as the element type.
     *
     * @param columnName   the name of the column, must not be {@code null}
     * @param dataType     the data type of the array elements, must not be {@code null}
     * @param maxLength    the maximum length of the column, must not be negative
     * @param defaultValue the default value for the column, or {@code null} if none
     * @param nullable     whether the column allows {@code null} values
     */
    public OlapArrayColumnDefinition(final String columnName, final DataType dataType, final int maxLength,
                                     final Object defaultValue, final boolean nullable) {
        super(columnName, CommonDataType.ARRAY, maxLength, defaultValue, nullable);
        this.elementType = dataType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getElementType() {
        return elementType;
    }
}