package com.tlc.sql.common.api.meta;

import com.tlc.commons.code.ErrorCode;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

/**
 * Defines the metadata for a decimal-type database column.
 * Extends {@link ColumnDefinition} to add precision and scale information for numeric decimal columns.
 */
public interface DecimalColumnDefinition extends ColumnDefinition {

    int DEFAULT_PRECISION = 4;
    int DEFAULT_SCALE = 1;

    /**
     * Validates that the given data type is a decimal type and that precision and scale are valid.
     *
     * @param dataType  the data type to validate, must be a decimal type
     * @param precision the numeric precision (total number of digits), must be at least 1
     * @param scale     the numeric scale (number of digits after the decimal point), must be at least 1
     * @throws com.tlc.commons.code.ErrorCode if the data type is not decimal, or precision/scale is less than 1
     */
    default void validate(final DataType dataType, final int precision, final int scale) {

        if (dataType.isDecimal()) {
            if (precision < 1) {
                throw ErrorCode.get(SQLErrorCodes.DB_INVALID_NUMERIC_PRECISION);
            }

            if (scale < 1) {
                throw ErrorCode.get(SQLErrorCodes.DB_INVALID_NUMERIC_SCALE);
            }

        } else {
            throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
        }
    }

    /**
     * Returns the precision (total number of digits) of the decimal column.
     *
     * @return the numeric precision
     */
    int getPrecision();

    /**
     * Returns the scale (number of digits after the decimal point) of the decimal column.
     *
     * @return the numeric scale
     */
    int getScale();
}