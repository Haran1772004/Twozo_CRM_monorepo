package com.tlc.sql.oltp.api.meta;

import com.tlc.sql.common.api.meta.ArrayColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;

/**
 * OLTP-specific implementation of {@link ArrayColumnDefinition}.
 * Represents an array column definition in PostgreSQL OLTP tables.
 * The column's overall data type is resolved to the appropriate {@link OltpDataType} array type
 * (e.g., BIGINT_ARRAY, INTEGER_ARRAY) based on the element type, ensuring that the JDBC driver
 * can determine the PostgreSQL array type name when setting prepared statement parameters.
 *
 * @author Muthu kumar v
 * @version 1.0
 */
public class OltpArrayColumnDefinition extends OltpColumnDefinition implements ArrayColumnDefinition {

    private final DataType elementType;

    /**
     * Constructs an OLTP array column definition.
     *
     * @param columnName   the name of the column, must not be {@code null}
     * @param elementType  the data type of the array elements, must not be {@code null}
     * @param maxLength    the maximum length of the column
     * @param defaultValue the default value for the column, or {@code null} if none
     * @param nullable     whether the column allows {@code null} values
     */
    public OltpArrayColumnDefinition(final String columnName, final DataType elementType, final int maxLength,
                                     final Object defaultValue, final boolean nullable) {
        super(columnName, resolveArrayDataType(elementType), maxLength, defaultValue, nullable);
        this.elementType = elementType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataType getElementType() {
        return elementType;
    }

    /**
     * Resolves the appropriate {@link OltpDataType} array type based on the element type.
     * This ensures that {@code PgsqlDMLHandler.handleArrayType} can determine the PostgreSQL
     * array type name via {@link OltpDataType#getElementDataType()} instead of relying on
     * runtime inference from list contents (which fails for empty lists).
     *
     * @param elementType the data type of the array elements
     * @return the resolved array {@link DataType}, either a specific {@link OltpDataType} or
     *         {@link CommonDataType#ARRAY} as fallback
     */
    private static DataType resolveArrayDataType(final DataType elementType) {
        if (elementType instanceof final CommonDataType commonType) {
            return switch (commonType) {
                case BIGINT -> OltpDataType.BIGINT_ARRAY;
                case INTEGER -> OltpDataType.INTEGER_ARRAY;
                case SMALLINT -> OltpDataType.SMALLINT_ARRAY;
                case DECIMAL -> OltpDataType.DECIMAL_ARRAY;
                case TEXT -> OltpDataType.CHAR_ARRAY;
                default -> CommonDataType.ARRAY;
            };
        } else if (elementType instanceof final OltpDataType oltpType) {
            return switch (oltpType) {
                case CHAR, SCHAR, KCHAR, STEXT -> OltpDataType.CHAR_ARRAY;
                case DATE, TIME, DATETIME -> OltpDataType.BIGINT_ARRAY;
                default -> CommonDataType.ARRAY;
            };
        }
        return CommonDataType.ARRAY;
    }
}