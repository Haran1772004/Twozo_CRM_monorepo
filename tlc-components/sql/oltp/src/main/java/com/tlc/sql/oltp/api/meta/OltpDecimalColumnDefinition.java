package com.tlc.sql.oltp.api.meta;

import com.tlc.sql.common.api.meta.ColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.DecimalColumnDefinition;

/**
 * OLTP-specific implementation of {@link DecimalColumnDefinition}.
 * Represents a decimal column definition used in OLTP (Online Transaction Processing) database tables,
 * combining {@link OltpColumnDefinition} with decimal precision and scale metadata.
 */
public class OltpDecimalColumnDefinition extends OltpColumnDefinition implements DecimalColumnDefinition {

    private final int scale;
    private final int precision;

    /**
     * Constructs an OLTP decimal column definition with the specified properties,
     * using {@link CommonDataType#DECIMAL} as the data type.
     *
     * @param columnName   the name of the column, must not be {@code null}
     * @param maxLength    the maximum length of the column, must not be negative
     * @param precision    the numeric precision (total number of digits), must be at least 1
     * @param scale        the numeric scale (number of digits after the decimal point), must be at least 1
     * @param defaultValue the default value for the column, or {@code null} if none
     * @param nullable     whether the column allows {@code null} values
     */
    public OltpDecimalColumnDefinition(final String columnName, final int maxLength, final int precision, final int scale,
                                       final Object defaultValue, final boolean nullable) {
        this(columnName, CommonDataType.DECIMAL, maxLength, precision, scale, defaultValue, nullable);
    }

    /**
     * Constructs an OLTP decimal column definition with all properties specified.
     *
     * @param columnName   the name of the column, must not be {@code null}
     * @param dataType     the data type of the column, must be a decimal type
     * @param maxLength    the maximum length of the column, must not be negative
     * @param precision    the numeric precision (total number of digits), must be at least 1
     * @param scale        the numeric scale (number of digits after the decimal point), must be at least 1
     * @param defaultValue the default value for the column, or {@code null} if none
     * @param nullable     whether the column allows {@code null} values
     * @throws com.tlc.commons.code.ErrorCode if the data type is not decimal, or precision/scale is less than 1
     */
    public OltpDecimalColumnDefinition(final String columnName, final DataType dataType, final int maxLength,
                                       final int precision, final int scale, final Object defaultValue, final boolean nullable) {
        super(columnName, dataType, maxLength, defaultValue, nullable);

        validate(dataType, precision, scale);
        this.precision = precision;
        this.scale = scale;
    }

    /**
     * Constructs an OLTP decimal column definition with default precision, scale, length,
     * no default value, and nullable set to {@code true}.
     *
     * @param columnName the name of the column, must not be {@code null}
     */
    public OltpDecimalColumnDefinition(final String columnName) {
        this(columnName, CommonDataType.DECIMAL, ColumnDefinition.DEFAULT_COLUMN_LENGTH, DecimalColumnDefinition.DEFAULT_PRECISION, DecimalColumnDefinition.DEFAULT_SCALE, null, true);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int getPrecision() {
        return this.precision;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getScale() {
        return this.scale;
    }
}