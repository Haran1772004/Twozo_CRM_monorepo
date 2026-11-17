package com.tlc.sql.olap.api.meta;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.l10n.temporal.model.Date;
import com.tlc.l10n.temporal.model.DateTime;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.common.api.meta.DataType;

import java.sql.Array;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.List;
import java.util.Arrays;

/**
 * @author Sundar
 * @version 1.0
 *
 * Enum representing OLAP-specific data types.
 * Provides methods for data type conversion, comparison, and SQL type mapping.
 */
public enum OlapDataType implements DataType {

    UNSIGNED_SMALLINT("UNSIGNED_SMALLINT", Types.SMALLINT,  true,  true,  false, false), //UINT16
    UNSIGNED_INTEGER ("UNSIGNED_INTEGER",  Types.INTEGER,   true,  true,  false, false), //UINT32
    UNSIGNED_BIGINT  ("UNSIGNED_BIGINT",   Types.BIGINT,    true,  true,  true,  false), //UINT64
    JSON             ("JSON",              Types.OTHER,     false, false, false, true),
    DATE             ("DATE",              Types.DATE,      true,  false, false, false),
    DATETIME         ("DATETIME",          Types.TIMESTAMP, true,  false, false, false),
    NULLABLE_DATE    ("NULLABLE(DATE)",    Types.DATE,      true,  false, false, false);

    private final String dataType;
    private final int sqlType;
    private final boolean numeric;
    private final boolean unsignedInteger;
    private final boolean bigint;
    private final boolean json;

    /**
     * Constructor for OlapDataType.
     *
     * @param dataType        the name of the data type
     * @param sqlType         the corresponding SQL type constant
     * @param numeric         whether the type is numeric
     * @param unsignedInteger whether the type is an unsigned integer
     * @param bigint          whether the type is a bigint
     * @param json            whether the type is JSON
     */
    OlapDataType(final String dataType, final int sqlType, final boolean numeric,
                 final boolean unsignedInteger, final boolean bigint, final boolean json) {
        this.dataType = Objects.requireNonNull(dataType);
        this.sqlType = sqlType;
        this.numeric = numeric;
        this.unsignedInteger = unsignedInteger;
        this.bigint = bigint;
        this.json = json;
    }

    /**
     * Retrieves the DataType instance corresponding to the given string representation.
     *
     * @param dataTypeStr the string representation of the data type
     * @return the corresponding DataType instance
     * @throws ErrorCode if the data type is not supported
     */
    public static DataType get(final String dataTypeStr) {
        return switch (dataTypeStr.toUpperCase()) {
            case "UNSIGNED_SMALLINT" -> UNSIGNED_SMALLINT;
            case "UNSIGNED_INTEGER" -> UNSIGNED_INTEGER;
            case "UNSIGNED_BIGINT" -> UNSIGNED_BIGINT;
            case "STRING" -> CommonDataType.TEXT;
            case "JSON" -> JSON;
            case "DATE" -> DATE;
            case "DATETIME" -> DATETIME;
            case "NULLABLE(DATE)" -> NULLABLE_DATE;
            default -> throw ErrorCode.get(ErrorCodes.NOT_SUPPORTED);
        };
    }

    /**
     * {@inheritDoc}
     *
     * @return the string representation of the data type
     */
    @Override
    public String getDataTypeStr() {
        return dataType;
    }

    /**
     * {@inheritDoc}
     *
     * @return the SQL type constant
     */
    @Override
    public int getSqlType() {
        return sqlType;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the data type is numeric, false otherwise
     */
    @Override
    public boolean isNumeric() {
        return numeric;
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
     * {@inheritDoc}
     *
     * @return true if the data type is a string, false otherwise
     */
    @Override
    public boolean isString() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the data type is an unsigned integer, false otherwise
     */
    @Override
    public boolean isUnsignedInteger() {
        return unsignedInteger;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the data type is a large string, false otherwise
     */
    @Override
    public boolean isLargeString() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the data type is a filterable string, false otherwise
     */
    @Override
    public boolean isFilterableString() {
        return isString();
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
     * {@inheritDoc}
     *
     * @return true if the data type is a storage type, false otherwise
     */
    @Override
    public boolean isStorageType() {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @param dataType the target data type
     * @return true if the data type can be converted, false otherwise
     */
    @Override
    public boolean canConvertTo(final DataType dataType) {
        return (this == UNSIGNED_SMALLINT && (dataType == CommonDataType.SMALLINT || dataType == CommonDataType.INTEGER || dataType == CommonDataType.BIGINT || dataType == UNSIGNED_INTEGER || dataType == UNSIGNED_BIGINT))
                || (this == UNSIGNED_INTEGER && (dataType == CommonDataType.INTEGER || dataType == CommonDataType.BIGINT || dataType == UNSIGNED_BIGINT))
                || (this == UNSIGNED_BIGINT && dataType == CommonDataType.BIGINT);
    }

    /**
     * {@inheritDoc}
     *
     * @param value the value to wrap
     * @return the wrapped value
     */
    @Override
    public Object getWrappedValue(final Object value) {
        return getValue(value, true);
    }

    /**
     * {@inheritDoc}
     *
     * @param value the value to unwrap
     * @return the unwrapped value
     */
    @Override
    public Object getUnWrappedValue(final Object value) {
        return getValue(value, false);
    }

    /**
     * {@inheritDoc}
     *
     * @param one the first object
     * @param two the second object
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean isEqual(final Object one, final Object two) {

        if (one == null && two == null) {
            return true;
        } else if (one != null && two != null) {
            switch (this) {
                case UNSIGNED_SMALLINT -> CommonDataType.SMALLINT.isEqual(one, two);
                case UNSIGNED_INTEGER -> CommonDataType.INTEGER.isEqual(one, two);
                case UNSIGNED_BIGINT -> CommonDataType.BIGINT.isEqual(one, two);
                case DATE -> {
                    final Date first = (one instanceof final Date date) ? date : new Date((Long) one);
                    final Date second = (two instanceof final Date date) ? date : new Date((Long) two);

                    return first.equals(second);
                }
                case DATETIME -> {
                    final DateTime first = (one instanceof final DateTime dateTime) ? dateTime : new DateTime((Long) one);
                    final DateTime second = (two instanceof final DateTime dateTime) ? dateTime : new DateTime((Long) two);

                    return first.equals(second);
                }
                case JSON -> {
                    final Map<String, Object> first = (one instanceof final JsonObject jsonObject) ? jsonObject.toMap() : Json.object(one.toString()).toMap();
                    final Map<String, Object> second = (two instanceof final JsonObject jsonObject) ? jsonObject.toMap() : Json.object(two.toString()).toMap();
                    return first.equals(second);
                }
                case NULLABLE_DATE -> {
                    if (one == null && two == null) return true;
                    if (one == null || two == null) return false;

                    final Date first = (one instanceof Date d) ? d : new Date((Long) one);
                    final Date second = (two instanceof Date d) ? d : new Date((Long) two);

                    return first.equals(second);
                }
                default -> {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves the value for the given object based on the data type and wrapping preference.
     *
     * @param value the value to process
     * @param wrap whether to wrap the value
     * @return the processed value
     */
    private Object getValue(final Object value, final boolean wrap) {
        if (value == null) {
            return null;
        }

        return switch (this) {
            case UNSIGNED_SMALLINT ->
                    wrap ? CommonDataType.SMALLINT.getWrappedValue(value) : CommonDataType.SMALLINT.getUnWrappedValue(value);
            case UNSIGNED_INTEGER ->
                    wrap ? CommonDataType.INTEGER.getWrappedValue(value) : CommonDataType.INTEGER.getUnWrappedValue(value);
            case UNSIGNED_BIGINT ->
                    wrap ? CommonDataType.BIGINT.getWrappedValue(value) : CommonDataType.BIGINT.getUnWrappedValue(value);
            case DATE -> {
                if (value instanceof final Date date) {
                    yield wrap ? date : date.getTimeStamp();
                } else {

                    if (value instanceof final java.sql.Date sqlDate) {
                        final long epochMilli = sqlDate.getTime();

                        yield wrap ? new Date(epochMilli) : epochMilli;
                    } else if (value instanceof final Long longValue) {
                        yield wrap ? new Date(longValue) : value;
                    }
                }
                yield value;
            }
            case DATETIME -> {
                if (value instanceof final DateTime dateTime) {
                    yield wrap ? dateTime : dateTime.getUtcTimeStamp();
                } else {

                    if (value instanceof final Timestamp sqlTimeStamp) {
                        final long epochMilli = sqlTimeStamp.toInstant().toEpochMilli();

                        yield wrap ? new DateTime(epochMilli) : epochMilli;
                    } else if (value instanceof final Long longValue) {
                        yield wrap ? new DateTime(longValue) : longValue;
                    }
                }
                yield value;
            }
            case JSON -> {

                if (value instanceof final Array sqlArray) {
                    try {
                        final Object arrayData = sqlArray.getArray();
                        if (arrayData instanceof Object[] objectArray) {
                            final List<Object> arrayList = Arrays.asList(objectArray);
                            yield wrap ? Json.array(arrayList) : arrayList;
                        }
                    } catch (final Exception error) {
                        throw ErrorCode.get(ErrorCodes.PARSE_FAILED, "Failed to parse SQL array value to JSON", error);
                    }
                    yield value;
                } else if (value instanceof final JsonObject jsonObject) {
                    yield wrap ? jsonObject : jsonObject.toMap();
                } else if (value instanceof final Map<?, ?> mapValue) {
                    final Map<String, Object> jsonMap = new LinkedHashMap<>();

                    for (final Map.Entry<?, ?> mapEntry : mapValue.entrySet()) {
                        jsonMap.put(mapEntry.getKey().toString(), sanitizeJsonValue(mapEntry.getValue()));  // ← sanitize here
                    }
                    yield wrap ? Json.object(jsonMap) : jsonMap;
                }
                yield value;
            }
            case NULLABLE_DATE -> {
                if (value == null) {
                    yield null;
                }

                if (value instanceof final Date date) {
                    yield wrap ? date : date.getTimeStamp();
                } else if (value instanceof final java.sql.Date sqlDate) {
                    final long epochMilli = sqlDate.toInstant().toEpochMilli();
                    yield wrap ? new Date(epochMilli) : epochMilli;
                } else if (value instanceof final Long longValue) {
                    yield wrap ? new Date(longValue) : longValue;
                }

                yield value;
            }
        };
    }

    private static Object sanitizeJsonValue(final Object value) {
        if (value instanceof final ZonedDateTime zdt) {
            return zdt.withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } else if (value instanceof final Map<?, ?> nestedMap) {
            final Map<String, Object> sanitized = new LinkedHashMap<>();
            nestedMap.forEach((k, v) -> sanitized.put(k.toString(), sanitizeJsonValue(v)));
            return sanitized;
        }
        return value;
    }

    /**
     * {@inheritDoc}
     *
     * @param one the first object to compare
     * @param two the second object to compare
     * @return true if the objects are equal, false otherwise
     */
    @Override
    public boolean matches(final Object one, final Object two) {
        if (one == null) {
            return two == null;
        } else {
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
        return bigint;
    }

    @Override
    public boolean isJson() {
        return json;
    }

    @Override
    public boolean isDate() {
        return this == DATE || this == NULLABLE_DATE;
    }
}