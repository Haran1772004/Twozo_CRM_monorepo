package com.tlc.sql.common.api.meta;

/**
 * @author Abishek
 * @version 1.0
 */
public interface DataType {

    String getDataTypeStr();

    int getSqlType();

    boolean isNumeric();

    boolean isCommon();

    boolean isString();

    boolean isUnsignedInteger();

    boolean isLargeString();

    boolean isFilterableString();

    boolean isBoolean();

    boolean isStorageType();

    boolean canConvertTo(DataType dataType);

    Object getWrappedValue(Object value);

    Object getUnWrappedValue(Object value);

    boolean isEqual(Object one, Object two);

    boolean matches(Object one, Object two);

    boolean isDecimal();

    boolean isBigint();

    default boolean isArray() {
        return false;
    }

    default boolean isJson() {
        return false;
    }

    default boolean isKChar() { return false;}

    boolean isDate();
}