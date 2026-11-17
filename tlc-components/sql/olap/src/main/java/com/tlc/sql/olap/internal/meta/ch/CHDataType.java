package com.tlc.sql.olap.internal.meta.ch;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.Objects;

public enum CHDataType implements DataType {

    LOW_CARDINALITY_STRING ("LOW_CARDINALITY_STRING",  Types.VARCHAR, false, true),
    LOW_CARDINALITY_INTEGER("LOW_CARDINALITY_INTEGER", Types.INTEGER, true,  false),
    AGGREGATE_FUNCTION     ("AGGREGATE_FUNCTION",      Types.OTHER,   false, false);

    private final String dataType;
    private final int sqlType;
    private final boolean numeric;
    private final boolean string;

    CHDataType(String dataType, int sqlType, boolean numeric, boolean string) {
        this.dataType = Objects.requireNonNull(dataType);
        this.sqlType = sqlType;
        this.numeric = numeric;
        this.string = string;
    }

    public static CHDataType get(final String dataTypeStr) {

        return switch (dataTypeStr.toUpperCase()) {
            case "LOW_CARDINALITY_STRING" -> LOW_CARDINALITY_STRING;
            case "LOW_CARDINALITY_INTEGER" -> LOW_CARDINALITY_INTEGER;
            case "AGGREGATE_FUNCTION" -> AGGREGATE_FUNCTION;
            default -> throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
        };
    }

    @Override
    public String getDataTypeStr() {
        return dataType;
    }

    @Override
    public int getSqlType() {
        return sqlType;
    }

    @Override
    public boolean isNumeric() {
        return numeric;
    }

    @Override
    public boolean isCommon() {
        return false;
    }

    @Override
    public boolean isString() {
        return string;
    }

    @Override
    public boolean isUnsignedInteger() {
        return false;
    }

    @Override
    public boolean isLargeString() {
        return false;
    }

    @Override
    public boolean isFilterableString() {
        return isString();
    }

    @Override
    public boolean isBoolean() {
        return false;
    }

    @Override
    public boolean isStorageType() {
        return false;
    }

    @Override
    public boolean canConvertTo(final DataType dataType) {
        return (this == LOW_CARDINALITY_INTEGER && (dataType == CommonDataType.INTEGER || dataType == CommonDataType.BIGINT));
    }

    @Override
    public Object getWrappedValue(final Object value) {
        return getValue(value);
    }

    @Override
    public Object getUnWrappedValue(final Object value) {
        return getValue(value);
    }

    @Override
    public boolean isEqual(final Object one, final Object two) {
        if (one == null || two == null) {
            return one == two;
        }
        return switch (this) {
            case LOW_CARDINALITY_STRING -> getValue(one).equals(getValue(two));
            case LOW_CARDINALITY_INTEGER, AGGREGATE_FUNCTION ->
                    Objects.equals(getValue(one), getValue(two));
        };
    }

    private Object getValue(Object value) {

        if (value == null) {
            return null;
        }
        switch (this) {
            case LOW_CARDINALITY_INTEGER -> {
                if (value instanceof Integer) {
                    return value;
                } else if (value instanceof final Number number) {
                    return number.intValue();
                } else {
                    return Integer.valueOf(value.toString());
                }
            }
            case AGGREGATE_FUNCTION -> {

                if (value instanceof final Double doubleValue) {
                    return doubleValue;
                } else if (value instanceof final BigDecimal bigDecimal) {
                    return bigDecimal.doubleValue();
                } else if (value instanceof final Number number) {
                    return number.longValue();
                } else {
                    return value.toString();
                }
            }
            case LOW_CARDINALITY_STRING -> {
                return value instanceof String ? value : value.toString();
            }
            default -> throw new IllegalStateException("Unexpected value: " + value);
        }
    }

    @Override
    public boolean matches(final Object one, final Object two) {
        if (one == null || two == null) {
            return one == two;
        }
        return switch (this) {
            case LOW_CARDINALITY_STRING -> one.toString().equalsIgnoreCase(two.toString());
            case LOW_CARDINALITY_INTEGER, AGGREGATE_FUNCTION ->
                    Objects.equals(getValue(one), getValue(two));
        };
    }

    @Override
    public boolean isDecimal() {
        return false;
    }

    @Override
    public boolean isBigint() {
        return false;
    }

    @Override
    public boolean isDate() {
        return false;
    }
}