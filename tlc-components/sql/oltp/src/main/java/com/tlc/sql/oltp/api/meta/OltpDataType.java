package com.tlc.sql.oltp.api.meta;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.i18n.I18nKey;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.l10n.temporal.model.Time;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;
import lombok.Getter;

import java.sql.Types;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Map;

/**
 * @author Sundar
 * @version 1.0
 *
 * Enum representing OLTP data types with their SQL type mappings.
 */
public enum OltpDataType implements DataType {

    CHAR    ("CHAR",     Types.VARCHAR, false, true,  false, true,  false, false, false),
    SCHAR   ("SCHAR",    Types.VARCHAR, false, true,  false, true,  false, false, false),
    KCHAR   ("KCHAR",    Types.VARCHAR, false, true,  false, true,  false, true, false),

    BLOB    ("BLOB",     Types.BLOB,    false, false, false, false, true,  false, false),
    STEXT   ("STEXT",    Types.VARCHAR, false, true,  true,  false, true,  false, false),
    DATE    ("DATE",     Types.BIGINT,  true, false, false, false, false, false, false),
    TIME    ("TIME",     Types.BIGINT,  true,  false, false, false, false, false, false),
    DATETIME("DATETIME", Types.BIGINT,  true,  false, false, false, false, false, false),

    INTEGER_ARRAY("INTEGER[]", Types.ARRAY, false, false, false, false, false, false, true),
    CHAR_ARRAY("CITEXT[]", Types.ARRAY, false, false, false, false, false, false, true),
    BIGINT_ARRAY("BIGINT[]", Types.ARRAY, false, false, false, false, false, false, true),
    DECIMAL_ARRAY("DECIMAL[]", Types.ARRAY, false, false, false, false, false, false, true),
    SMALLINT_ARRAY("SMALLINT[]", Types.ARRAY, false, false, false, false, false, false, true),
    ;

    private final String dataType;
    @Getter
    private final int sqlType;
    private final boolean numeric;
    private final boolean string;
    private final boolean largeString;
    private final boolean filterableString;
    private final boolean storageType;
    private final boolean kChar;
    private final boolean isArray;

    /**
     * Constructor for OltpDataType.
     *
     * @param dataType         the name of the data type
     * @param sqlType          the corresponding SQL type constant
     * @param numeric          whether the type is numeric
     * @param string           whether the type is a string type
     * @param largeString      whether the type is a large string
     * @param filterableString whether the type is a filterable string
     * @param storageType      whether the type is a storage type
     * @param kChar            whether the type is KChar
     */
    OltpDataType(String dataType, int sqlType, boolean numeric, boolean string, boolean largeString,
                 boolean filterableString, boolean storageType, boolean kChar, boolean isArray) {
        this.sqlType = sqlType;
        this.dataType = Objects.requireNonNull(dataType);
        this.numeric = numeric;
        this.string = string;
        this.largeString = largeString;
        this.filterableString = filterableString;
        this.storageType = storageType;
        this.kChar = kChar;
        this.isArray = isArray;
    }

    /**
     * Maps legacy array enum values to their element CommonDataType.
     */
    private static final Map<OltpDataType, CommonDataType> ARRAY_ELEMENT_TYPE_MAP = Map.of(
            INTEGER_ARRAY, CommonDataType.INTEGER,
            BIGINT_ARRAY, CommonDataType.BIGINT,
            DECIMAL_ARRAY, CommonDataType.DECIMAL,
            SMALLINT_ARRAY, CommonDataType.SMALLINT,
            CHAR_ARRAY, CommonDataType.TEXT
    );

    /**
     * Returns the element type for a legacy array enum value.
     *
     * @return the element {@link CommonDataType}, or {@code null} if this is not an array type
     */
    public CommonDataType getElementDataType() {
        return ARRAY_ELEMENT_TYPE_MAP.get(this);
    }

    /**
     * Retrieves the DataType instance corresponding to the given type string.
     *
     * @param type the type string
     * @return the corresponding DataType instance
     * @throws ErrorCode if the type is not supported
     */
    public static DataType get(String type) {
        return switch (type) {
            case "CHAR" -> CHAR;
            case "SCHAR" -> SCHAR;
            case "KCHAR" -> KCHAR;
            case "STEXT" -> STEXT;
            case "BLOB" -> BLOB;
            case "DATE" -> DATE;
            case "TIME" -> TIME;
            case "DATETIME" -> DATETIME;
            case "CITEXT[]" -> CHAR_ARRAY;
            case "DECIMAL[]" -> DECIMAL_ARRAY;
            case "BIGINT[]" -> BIGINT_ARRAY;
            case "INTEGER[]" -> INTEGER_ARRAY;
            case "SMALLINT[]" -> SMALLINT_ARRAY;
            default -> throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
        };
    }

    /**
     * Gets the string representation of the data type.
     *
     * @return the data type string
     */
    public String getDataTypeStr() {
        return dataType;
    }

    /**
     * Checks if the data type is numeric.
     *
     * @return true if numeric, false otherwise
     */
    public boolean isNumeric() {
        return numeric;
    }

    /**
     * Checks if the data type is KChar.
     *
     * @return true if KChar, false otherwise
     */
    @Override
    public boolean isKChar() {
        return kChar;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the data type is common, false otherwise
     */
    @Override
    public boolean isCommon() {
        return false;
    }

    /**
     * Checks if the data type is a string type.
     *
     * @return true if string type, false otherwise
     */
    public boolean isString() {
        return string;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the data type is an unsigned integer, false otherwise
     */
    @Override
    public boolean isUnsignedInteger() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the data type is a large string, false otherwise
     */
    @Override
    public boolean isLargeString() {
        return largeString;
    }

    /**
     * Checks if the data type is a filterable string.
     *
     * @return true if filterable string, false otherwise
     */
    public boolean isFilterableString() {
        return filterableString;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the data type is boolean, false otherwise
     */
    @Override
    public boolean isBoolean() {
        return false;
    }

    /**
     * Checks if the data type is a storage type.
     *
     * @return true if storage type, false otherwise
     */
    public boolean isStorageType() {
        return storageType;
    }

    /**
     * Checks if the data type is array.
     *
     * @return true if numeric, false otherwise
     */
    @Override
    public boolean isArray() {
        return isArray;
    }

    /**
     * Checks if the data type can be converted to another data type.
     *
     * @param dataType the target data type
     * @return true if conversion is possible, false otherwise
     */
    public boolean canConvertTo(DataType dataType) {
        return (this == DATETIME && dataType == CommonDataType.BIGINT) || (this == DATE && dataType == CommonDataType.BIGINT) || (this == TIME && dataType == CommonDataType.BIGINT);
    }

    /**
     * Compares two objects for equality based on the data type.
     *
     * @param one the first object
     * @param two the second object
     * @return true if equal, false otherwise
     */
    public boolean matches(Object one, Object two) {
        if(one == null || two == null) {
            return one == two;
        }

        if(this == CHAR) {
            return one.toString().equalsIgnoreCase(two.toString());
        }
        else {
            return one.equals(two);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the data type is a big integer, false otherwise
     */
    @Override
    public boolean isDecimal() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the data type is decimal, false otherwise
     */
    @Override
    public boolean isBigint() {
        return false;
    }

    public Object getWrappedValue(Object value) {
        return getValue(value, true);
    }

    /**
     * Gets the unwrapped value of the given object based on the data type.
     *
     * @param value the value to unwrap
     * @return the unwrapped value
     */
    public Object getUnWrappedValue(Object value) {
        return getValue(value, false);
    }

    /**
     * Checks if two objects are equal based on the data type.
     *
     * @param one the first object
     * @param two the second object
     * @return true if equal, false otherwise
     */
    public boolean isEqual(Object one, Object two) {
        if (one == null && two == null) {
            return true;
        } else if (one != null && two != null) {
            switch (this) {
                case CHAR, SCHAR, STEXT -> {
                    return CommonDataType.TEXT.isEqual(one, two);
                }
                case DATE -> {
                    final Date first = one instanceof final Date firstValue ? firstValue : new Date(toEpoch(one));
                    final Date second = two instanceof final Date secondValue ? secondValue : new Date(toEpoch(two));

                    return first.equals(second);
                }
                case TIME -> {
                    final Time first = one instanceof final Time firstValue ? firstValue : new Time(toEpoch(one));
                    final Time second = two instanceof final Time secondValue ? secondValue : new Time(toEpoch(two));

                    return first.equals(second);
                }
                case DATETIME -> {
                    final DateTime first = one instanceof final DateTime firstValue ? firstValue : new DateTime(toEpoch(one));
                    final DateTime second = two instanceof final DateTime secondValue ? secondValue : new DateTime(toEpoch(two));

                    return first.equals(second);
                }
                case BLOB -> {
                    final byte[] first = one instanceof final byte[] firstValue ? firstValue : Base64.getDecoder().decode(one.toString());
                    final byte[] second = two instanceof final byte[] secondValue ? secondValue : Base64.getDecoder().decode(two.toString());
                    return Arrays.equals(first, second);
                }
                case KCHAR -> {
                    final String first = one instanceof final I18nKey firstValue ? firstValue.getKey() : one.toString();
                    final String second = two instanceof final I18nKey secondValue ? secondValue.getKey() : two.toString();
                    return first.equals(second);
                }
                case BIGINT_ARRAY, CHAR_ARRAY, DECIMAL_ARRAY, SMALLINT_ARRAY, INTEGER_ARRAY ->
                {
                    final List<?> first = (one instanceof List<?>) ? (List<?>) one : List.of(one);
                    final List<?> second = (two instanceof List<?>) ? (List<?>) two : List.of(two);

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

    /**
     * Retrieves the value of the given object based on the data type.
     *
     * @param value the value to process
     * @param wrap whether to wrap the value
     * @return the processed value
     */
    private Object getValue(Object value, boolean wrap) {
        if (value == null) {
            return null;
        }
        switch (this) {
            case DATE -> {
                if (value instanceof final Date date) {
                    return wrap ? date : date.getTimeStamp();
                } else {
                    final long epochMillis = toEpoch(value);
                    return wrap ? new Date(epochMillis) : epochMillis;
                }
            }
            case TIME -> {
                if (value instanceof final Time time) {
                    return wrap ? time : time.getTimeStamp();
                } else {
                    final long epochMillis = toEpoch(value);
                    return wrap ? new Time(epochMillis) : epochMillis;
                }
            }
            case DATETIME -> {
                if (value instanceof final DateTime dateTime) {
                    return wrap ? dateTime : dateTime.getUtcTimeStamp();
                } else {
                    final long epochMillis = toEpoch(value);
                    return wrap ? new DateTime(epochMillis) : epochMillis;
                }
            }
            case KCHAR -> {
                if (!(value instanceof I18nKey)) {
                    if (wrap) {
                        return new I18nKey(value.toString());
                    } else {
                        return value.toString();
                    }
                } else {
                    if (wrap) {
                        return value;
                    } else {
                        return ((I18nKey) value).getKey();
                    }
                }
            }
            case BLOB -> {
                if (!(value instanceof Byte[])) {
                    return Base64.getDecoder().decode(value.toString());
                } else {
                    return value;
                }
            }
            case CHAR_ARRAY, BIGINT_ARRAY, DECIMAL_ARRAY, INTEGER_ARRAY, SMALLINT_ARRAY -> {
                return value;
            }
            case CHAR, SCHAR, STEXT -> {
                return value.toString();
            }
            default -> throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
        }
    }

    /**
     * Coerces an object to an epoch-millisecond {@code long}.
     * Accepts any {@link Number} subtype (Integer, Long, BigInteger, BigDecimal, ...) and
     * numeric {@link String} values — covering JDBC, JSON, and audit-log payloads.
     *
     * @param value non-null source value
     * @return the epoch value as {@code long}
     * @throws NumberFormatException if {@code value} is a non-numeric String
     */
    private static long toEpoch(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.parseLong(value.toString());
    }

    @Override
    public boolean isDate() {
        return this == DATE;
    }
}