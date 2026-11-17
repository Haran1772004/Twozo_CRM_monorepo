package com.tlc.sql.olap.internal.util;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.meta.ArrayColumnDefinition;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import com.tlc.sql.common.api.meta.DecimalColumnDefinition;
import com.tlc.sql.common.internal.status.SQLErrorCodes;
import com.tlc.sql.common.util.DDLDMLUtil;
import com.tlc.sql.olap.api.meta.OlapColumnDefinition;
import com.tlc.sql.olap.api.meta.OlapDataType;

import com.tlc.sql.olap.internal.meta.ch.AggregateFunctionColumnDefinition;
import com.tlc.sql.olap.internal.meta.ch.CHDataType;

/**
 * @author Sundar
 * @version 1.0
 *
 * Utility class for handling ClickHouse-specific DDL and DML operations.
 * Provides methods for generating data type strings and handling column definitions.
 */
public interface CHDdlDmlUtil {
    String LOW_CARDINALITY_UNKNOWN_TYPE = "LowCardinality(%s)";
    String NULLABLE_STRING_TYPE = "Nullable(String)";
    String NULLABLE_INTEGER_TYPE = "Nullable(Int32)";
    String NULLABLE_UNKNOWN_TYPE = "Nullable(%s)";
    String ARRAY_UNKNOWN_TYPE = "Array(%s)";
    String STRING_TYPE = "String";
    String INTEGER_TYPE = "Int32";



    /**
     * Retrieves the data type string for a given column definition.
     *
     * @param columnDefinition the column definition to evaluate
     * @return the data type string for the column
     */
    static String getDataType(final OlapColumnDefinition columnDefinition) {
        final DataType dataType = columnDefinition.getDataType();

        if (CommonDataType.ARRAY == dataType) {
            final ArrayColumnDefinition arrayColumnDefinition = (ArrayColumnDefinition) columnDefinition;
            final DataType elementType = arrayColumnDefinition.getElementType();
            final boolean isNullable = arrayColumnDefinition.isNullable();

            if (elementType.isDecimal()) {

                if (columnDefinition instanceof final DecimalColumnDefinition decimalColumnDefinition) {
                    return getDecimalArrayDataTypeString(decimalColumnDefinition.getPrecision(), decimalColumnDefinition.getScale(), isNullable);
                } else {
                    throw ErrorCode.get(SQLErrorCodes.UNSUPPORTED_OPERATION);
                }
            } else {
                return getArrayDataTypeString(elementType, columnDefinition);
            }
        } else {
            return getDataTypeString(columnDefinition, dataType);
        }
    }

    /**
     * Retrieves the data type string for a given data type.
     *
     * @param dataType the data type to evaluate
     * @return the data type string for the data type
     */
    static String getDataTypeString(final DataType dataType) {

        if (dataType.isCommon()) {
            final CommonDataType commonDataType = (CommonDataType) dataType;

            return switch (commonDataType) {
                case TEXT -> "String";
                case BIGINT -> "Int64";
                case INTEGER -> "Int32";
                case SMALLINT -> "Int16";
                case BOOLEAN -> "Bool";
                case NULLABLE_BIGINT -> "Nullable(Int64)";
                default -> DDLDMLUtil.getDataTypeStr(commonDataType);
            };
        } else if (dataType instanceof final OlapDataType oltpDataType) {

            return switch (oltpDataType) {
                case UNSIGNED_SMALLINT -> "UInt16";
                case UNSIGNED_INTEGER -> "UInt32";
                case UNSIGNED_BIGINT -> "UInt64";
                case DATE -> "DATE";
                case DATETIME -> "DATETIME64(6, 'UTC')";
                case JSON -> "JSON";
                case NULLABLE_DATE -> "Nullable(DATE)";
            };
        }
        throw ErrorCode.get(SQLErrorCodes.DB_SCHEMA_INVALID_DATA_TYPE);
    }

    /**
     * Retrieves the decimal data type string for a given precision and scale.
     *
     * @param precision the precision of the decimal type
     * @param scale the scale of the decimal type
     * @return the decimal data type string
     */
    static String getDecimalDataTypeString(final int precision, final int scale) {
        return DDLDMLUtil.getDecimalDataTypeStr(precision, scale);
    }

    /**
     * Retrieves the array data type string for a given element type and column definition.
     *
     * @param elementType the data type of the array elements
     * @param olapColumnDefinition the column definition for the array
     * @return the array data type string
     */
    static String getArrayDataTypeString(final DataType elementType, final OlapColumnDefinition olapColumnDefinition) {
        final String elementTypeStr = getDataTypeString(olapColumnDefinition, elementType);
        final String wrappedElementType = olapColumnDefinition.isNullable() ? String.format(NULLABLE_UNKNOWN_TYPE, elementTypeStr) : elementTypeStr;

        return String.format(ARRAY_UNKNOWN_TYPE, wrappedElementType);
    }

    /**
     * Retrieves the array data type string for a given element type and nullable flag.
     *
     * @param elementType the data type of the array elements
     * @param isNullable whether the array elements are nullable
     * @return the array data type string
     */
    static String getArrayDataTypeString(final DataType elementType, final boolean isNullable) {
        final String elementTypeStr = getDataTypeString(elementType);
        final String wrappedElementType = isNullable ? String.format(NULLABLE_UNKNOWN_TYPE, elementTypeStr) : STRING_TYPE;

        return String.format(ARRAY_UNKNOWN_TYPE, wrappedElementType);
    }

    /**
     * Retrieves the decimal array data type string for a given precision, scale, and nullable flag.
     *
     * @param precision the precision of the decimal type
     * @param scale the scale of the decimal type
     * @param isNullable whether the decimal type is nullable
     * @return the decimal array data type string
     */
    static String getDecimalArrayDataTypeString(final int precision, final int scale, final boolean isNullable) {
        final String decimalTypeStr = getDecimalDataTypeString(precision, scale);
        final String wrappedDecimalType = isNullable ? String.format(NULLABLE_UNKNOWN_TYPE, decimalTypeStr) : decimalTypeStr;

        return String.format(ARRAY_UNKNOWN_TYPE, wrappedDecimalType);
    }

    /**
     * Retrieves the data type string for a given column definition and data type.
     *
     * @param columnDefinition the column definition to evaluate
     * @param dataType the data type to evaluate
     * @return the data type string for the column and data type
     */
    static String getDataTypeString(final OlapColumnDefinition columnDefinition, final DataType dataType) {

        if (dataType.isDecimal() && columnDefinition instanceof final DecimalColumnDefinition decimalColumnDefinition) {
            return getDecimalDataTypeString(decimalColumnDefinition.getPrecision(), decimalColumnDefinition.getScale());
        } else if (dataType instanceof final CHDataType chDataType) {
            return getCHDataType(chDataType, columnDefinition);
        } else {
            return getDataTypeString(dataType);
        }
    }

    /**
     * Retrieves the ClickHouse-specific data type string for a given data type and column definition.
     *
     * @param chDataType the ClickHouse-specific data type
     * @param columnDefinition the column definition to evaluate
     * @return the ClickHouse-specific data type string
     */
    private static String getCHDataType(final CHDataType chDataType, final OlapColumnDefinition columnDefinition) {
        return switch (chDataType) {
            case LOW_CARDINALITY_STRING -> {
                final String innerType = columnDefinition.isNullable() ? NULLABLE_STRING_TYPE : STRING_TYPE;
                yield String.format(LOW_CARDINALITY_UNKNOWN_TYPE, innerType);
            }
            case LOW_CARDINALITY_INTEGER -> {
                final String innerType = columnDefinition.isNullable() ? NULLABLE_INTEGER_TYPE : INTEGER_TYPE;
                yield String.format(LOW_CARDINALITY_UNKNOWN_TYPE, innerType);
            }
            case AGGREGATE_FUNCTION -> {

                if (columnDefinition instanceof final AggregateFunctionColumnDefinition aggregateFunctionColumnDefinition) {
                    final String aggregateFunctionName = aggregateFunctionColumnDefinition.getAggregateFunctionName();
                    final StringBuilder aggregateFunctionColumnBuilder = new StringBuilder(String.format("AggregateFunction(%s,", aggregateFunctionName));

                    for (final DataType type : aggregateFunctionColumnDefinition.getParamTypeList()) {
                        aggregateFunctionColumnBuilder.append(String.format(" %s,", getDataTypeString(columnDefinition, type)));
                    }
                    yield StringUtil.replaceSuffix(aggregateFunctionColumnBuilder, NumericalConstant.ONE, ")").toString();
                } else {
                    throw ErrorCode.get(ErrorCodes.INVALID_FORMAT);
                }
            }
        };
    }

    /**
     * Retrieves the nullable data type string for a given data type.
     *
     * @param dataType the data type to evaluate
     * @return the nullable data type string
     */
    static String getWithNullable(final DataType dataType) {
        return String.format(NULLABLE_UNKNOWN_TYPE, getDataTypeString(dataType));
    }

    /**
     * Retrieves the nullable decimal data type string for a given precision and scale.
     *
     * @param precision the precision of the decimal type
     * @param scale the scale of the decimal type
     * @return the nullable decimal data type string
     */
    static String getDecimalWithNullable(final int precision, final int scale) {
        return String.format(NULLABLE_UNKNOWN_TYPE, getDecimalDataTypeString(precision, scale));
    }
}
