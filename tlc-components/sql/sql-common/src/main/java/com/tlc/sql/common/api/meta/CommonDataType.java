package com.tlc.sql.common.api.meta;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.sql.common.internal.status.SQLErrorCodes;

import java.sql.Array;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public enum CommonDataType implements DataType {

    SMALLINT       ("SMALLINT",          Types.SMALLINT, true,  false, false, false, false, false, false),
    INTEGER        ("INTEGER",           Types.INTEGER,  true,  false, false, false, false, false, false),
    BOOLEAN        ("BOOLEAN",           Types.BOOLEAN,  false, false, false, true,  false, false, false),
    BIGINT         ("BIGINT",            Types.BIGINT,   true,  false, false, false, false, true,  false),
    DECIMAL        ("DECIMAL",           Types.DECIMAL,  true,  false, false, false, true,  false, false),
    TEXT           ("TEXT",              Types.VARCHAR,  false, true,  true,  false, false, false, false),
    ARRAY          ("ARRAY",             Types.ARRAY,    false, false, false, false, false, false, true),
    NULLABLE_BIGINT("NULLABLE(BIGINT)",  Types.BIGINT,   true,  false, false, false, false, false, false);

    private final String name;
    private final int sqlType;
    private final boolean numeric;
    private final boolean string;
    private final boolean largeString;
    private final boolean booleanType;
    private final boolean decimal;
    private final boolean bigint;
    private final boolean array;

    CommonDataType(final String name, final int sqlType, final boolean numeric, final boolean string,
                   final boolean largeString, final boolean booleanType, final boolean decimal,
                   final boolean bigint, final boolean array) {
        this.name = name;
        this.sqlType = sqlType;
        this.numeric = numeric;
        this.string = string;
        this.largeString = largeString;
        this.booleanType = booleanType;
        this.decimal = decimal;
        this.bigint = bigint;
        this.array = array;
    }

    public static DataType get(final String dataTypeStr) {
        return switch (dataTypeStr.toUpperCase()) {
            case "SMALLINT" -> SMALLINT;
            case "INTEGER" -> INTEGER;
            case "BOOLEAN" -> BOOLEAN;
            case "BIGINT" -> BIGINT;
            case "DECIMAL" -> DECIMAL;
            case "TEXT" -> TEXT;
            case "ARRAY" -> ARRAY;
            case "NULLABLE(BIGINT)" -> NULLABLE_BIGINT;
            default -> throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
        };
    }

    @Override
    public String getDataTypeStr() {
        return name;
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
        return true;
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
        return largeString;
    }

    @Override
    public boolean isFilterableString() {
        return false;
    }

    @Override
    public boolean isBoolean() {
        return booleanType;
    }

    @Override
    public boolean isStorageType() {
        return false;
    }

    @Override
    public boolean canConvertTo(final DataType dataType) {
        return (this == SMALLINT && (dataType == INTEGER || dataType == BIGINT)) || (this == INTEGER && dataType == BIGINT);
    }

    @Override
    public Object getWrappedValue(final Object value) {
        return getValue(value, true);
    }

    @Override
    public Object getUnWrappedValue(final Object value) {
        return getValue(value, false);
    }

    @Override
    public boolean isEqual(final Object one, final Object two) {
        if (one == null && two == null) {
            return true;
        } else if (one != null && two != null) {
            switch (this) {
                case TEXT -> {
                    final String first = one instanceof final String firstValue ? firstValue : one.toString();
                    final String second = two instanceof final String secondValue ? secondValue : two.toString();
                    return first.equals(second);
                }
                case SMALLINT -> {
                    final Short first = one instanceof final Short firstValue ? firstValue : Short.parseShort(one.toString());
                    final Short second = two instanceof final Short secondValue ? secondValue : Short.parseShort(two.toString());
                    return first.equals(second);
                }
                case INTEGER -> {
                    final Integer first = one instanceof final Integer firstValue ? firstValue : Integer.parseInt(one.toString());
                    final Integer second = two instanceof final Integer secondValue ? secondValue : Integer.parseInt(two.toString());
                    return first.equals(second);
                }
                case BIGINT -> {
                    final Long first = one instanceof final Long firstValue ? firstValue : Long.parseLong(one.toString());
                    final Long second = two instanceof final Long secondValue ? secondValue : Long.parseLong(two.toString());
                    return first.equals(second);
                }
                case DECIMAL -> {
                    final Double first = one instanceof final Double firstValue ? firstValue : Double.parseDouble(one.toString());
                    final Double second = two instanceof final Double secondValue ? secondValue : Double.parseDouble(two.toString());
                    final double epsilon = 1e-9;
                    return Math.abs(first - second) < epsilon;
                }
                case BOOLEAN -> {
                    final Boolean first = one instanceof final Boolean firstValue ? firstValue : Boolean.parseBoolean(one.toString());
                    final Boolean second = two instanceof final Boolean secondValue ? secondValue : Boolean.parseBoolean(two.toString());
                    return first.equals(second);
                }
                case ARRAY -> {
                    if (one instanceof final List<?> firstList && two instanceof final List<?> secondList) {
                        return firstList.equals(secondList);
                    }
                    return false;
                }
                case NULLABLE_BIGINT -> {
                    if (one == null && two == null) return true;
                    if (one == null || two == null) return false;

                    final Long first = (one instanceof Long d) ? d : Long.parseLong(one.toString());
                    final Long second = (two instanceof Long d) ? d : Long.parseLong(two.toString());

                    return first.equals(second);
                }
                default -> {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean matches(final Object one, final Object two) {

        if (one == null) {
            return two == null;
        } else {
            if (this == TEXT && two != null) {
                return one.toString().equalsIgnoreCase(two.toString());
            }
            return one.equals(two);
        }
    }

    @Override
    public boolean isDecimal() {
        return decimal;
    }

    @Override
    public boolean isBigint() {
        return bigint;
    }

    @Override
    public boolean isArray() {
        return array;
    }

    @Override
    public boolean isDate() {
        return false;
    }

    private Object getValue(Object value, boolean wrap) {
        if (value == null) {
            return null;
        }

        switch (this) {
            case SMALLINT:
                if (value instanceof Short) {
                    return value;
                } else if (value instanceof Number number) {
                    return number.shortValue();
                } else {
                    return Short.valueOf(value.toString());
                }
            case INTEGER:
                if (value instanceof Integer) {
                    return value;
                } else if (value instanceof Number number) {
                    return number.intValue();
                } else {
                    return Integer.valueOf(value.toString());
                }
            case BIGINT:
                if (value instanceof Long) {
                    return value;
                } else if (value instanceof Number number) {
                    return number.longValue();
                } else {
                    return Long.valueOf(value.toString());
                }
            case DECIMAL:
                if (value instanceof Double) {
                    return value;
                } else if (value instanceof Number number) {
                    return number.doubleValue();
                } else {
                    return Double.valueOf(value.toString());
                }
            case BOOLEAN:
                if (!(value instanceof Boolean)) {
                    return Boolean.valueOf(value.toString());
                } else {
                    return value;
                }
            case TEXT:
                return value.toString();
            case ARRAY:
                try {
                    if (value instanceof Array sqlArray && sqlArray.getArray() instanceof Object[] arrayValue) {
                        return Arrays.asList(arrayValue);
                    } else if (value instanceof List<?>) {
                        return value;
                    }
                } catch (SQLException e) {
                    throw ErrorCode.get(SQLErrorCodes.DB_DATA_INVALID_COLUMN_VALUE, "Unexpected value: " + value, e);
                }
                return value;
            case NULLABLE_BIGINT:
                if (value == null) {
                    return null;
                }
                if (value instanceof Long) {
                    return value;
                } else if (value instanceof Number number) {
                    return number.longValue();
                } else if (value instanceof Date date) {
                    return date.getTime();
                } else {
                    return Long.valueOf(value.toString());
                }
            default:
                throw ErrorCode.get(SQLErrorCodes.DB_DATA_INVALID_COLUMN_VALUE, "Unexpected value: " + value);
        }
    }
}