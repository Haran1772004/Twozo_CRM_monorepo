package com.tlc.commons.json.resolver.model;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;

import java.util.Objects;

/**
 * <p>
 * Represents a placeholder value used for resolving {@code {{KEY}}} placeholders
 * in JSON structures. This class can hold different types of values including
 * {@link String}, {@link Boolean}, and {@link Number} types.
 * </p>
 *
 * <p>
 * The value is stored in its native type and can be retrieved as a {@link String}
 * via {@link #getValue()}, or in its original type via the typed getters.
 * Use the {@code is*()} methods to check the type before calling a typed getter.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see PlaceholderKey
 * @see PlaceholderEntry
 * @see PlaceholderData
 */
public final class PlaceholderValue {

    private final Object value;
    private final ValueType type;

    /**
     * Supported value types.
     */
    public enum ValueType {
        STRING,
        BOOLEAN,
        NUMBER,
        JSON_OBJECT,
        JSON_ARRAY,
        NULL
    }

    private PlaceholderValue(final Object value, final ValueType type) {
        this.value = value;
        this.type = type;
    }

    // Factory Methods

    /**
     * Creates a {@link PlaceholderValue} from a {@link String}.
     *
     * @param value the string value
     * @return a new {@link PlaceholderValue} instance
     */
    public static PlaceholderValue of(final String value) {
        return new PlaceholderValue(value, ValueType.STRING);
    }

    /**
     * Creates a {@link PlaceholderValue} from a {@link Boolean}.
     *
     * @param value the boolean value
     * @return a new {@link PlaceholderValue} instance
     */
    public static PlaceholderValue of(final Boolean value) {
        return new PlaceholderValue(value, ValueType.BOOLEAN);
    }

    /**
     * Creates a {@link PlaceholderValue} from a {@link Number}.
     *
     * @param value the number value ({@link Integer}, {@link Long}, {@link Double}, etc.)
     * @return a new {@link PlaceholderValue} instance
     */
    public static PlaceholderValue of(final Number value) {
        return new PlaceholderValue(value, ValueType.NUMBER);
    }

    /**
     * Creates a {@link PlaceholderValue} from a {@link JsonObject}.
     *
     * @param value the json object value
     * @return a new {@link PlaceholderValue} instance
     */
    public static PlaceholderValue of(final JsonObject value) {
        return new PlaceholderValue(value, ValueType.JSON_OBJECT);
    }

    /**
     * Creates a {@link PlaceholderValue} from a {@link JsonArray}.
     *
     * @param value the json array value
     * @return a new {@link PlaceholderValue} instance
     */
    public static PlaceholderValue of(final JsonArray value) {
        return new PlaceholderValue(value, ValueType.JSON_ARRAY);
    }

    /**
     * Creates a {@link PlaceholderValue} representing a {@code null} value.
     *
     * @return a new {@link PlaceholderValue} instance representing null
     */
    public static PlaceholderValue ofNull() {
        return new PlaceholderValue(null, ValueType.NULL);
    }

    /**
     * Returns the value as a {@link String}.
     * For non-string types the native {@code toString()} representation is returned.
     *
     * @return the string representation, or {@code null} if the value is null
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return (T) value;
    }

    /**
     * Returns the value as a {@link Boolean}.
     *
     * @return the boolean value
     * @throws IllegalStateException if this value is not of type {@link ValueType#BOOLEAN}
     */
    public Boolean getAsBoolean() {
        if (type != ValueType.BOOLEAN) {
            throw new IllegalStateException("Value is not a Boolean, actual type: " + type);
        }
        return (Boolean) value;
    }

    /**
     * Returns the value as a {@link Number}.
     *
     * @return the number value
     * @throws IllegalStateException if this value is not of type {@link ValueType#NUMBER}
     */
    public Number getAsNumber() {
        if (type != ValueType.NUMBER) {
            throw new IllegalStateException("Value is not a Number, actual type: " + type);
        }
        return (Number) value;
    }

    /**
     * Returns the value as an {@link Integer}.
     *
     * @return the integer value
     * @throws IllegalStateException if this value is not of type {@link ValueType#NUMBER}
     */
    public Integer getAsInteger() {
        return getAsNumber().intValue();
    }

    /**
     * Returns the value as a {@link Long}.
     *
     * @return the long value
     * @throws IllegalStateException if this value is not of type {@link ValueType#NUMBER}
     */
    public Long getAsLong() {
        return getAsNumber().longValue();
    }

    /**
     * Returns the value as a {@link Double}.
     *
     * @return the double value
     * @throws IllegalStateException if this value is not of type {@link ValueType#NUMBER}
     */
    public Double getAsDouble() {
        return getAsNumber().doubleValue();
    }

    /**
     * Returns the raw underlying value object.
     *
     * @return the raw value
     */
    public Object getRawValue() {
        return value;
    }

    /**
     * Returns the {@link ValueType} of this instance.
     *
     * @return the value type
     */
    public ValueType getValueType() {
        return type;
    }

    /**
     * @return {@code true} if the value is {@code null} ({@link ValueType#NULL})
     */
    public boolean isNull() {
        return type == ValueType.NULL;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final PlaceholderValue that = (PlaceholderValue) o;
        return Objects.equals(value, that.value) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, type);
    }

    @Override
    public String toString() {
        return "PlaceholderValue{value=" + value + ", type=" + type + '}';
    }
}
