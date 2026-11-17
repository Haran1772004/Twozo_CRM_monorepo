package com.tlc.commons.json.resolver.model;

import java.util.Objects;

/**
 * <p>
 * An immutable POJO representing a single scalar placeholder mapping.
 * Holds the placeholder key (without braces) and its replacement value.
 * </p>
 *
 * <p>
 * The value can be provided as either a {@link PlaceholderValue} for type safety
 * or as a raw {@link String} for convenience and backward compatibility.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see PlaceholderEntries
 * @see PlaceholderValue
 */
public final class PlaceholderEntry {

    private final PlaceholderKey key;
    private final PlaceholderValue value;

    /**
     * Constructs a new {@link PlaceholderEntry} with a {@link PlaceholderValue}.
     *
     * @param key   The placeholder key (without braces).
     * @param value The replacement value.
     */
    public PlaceholderEntry(final PlaceholderKey key, final PlaceholderValue value) {
        this.key = Objects.requireNonNull(key, "Placeholder key must not be null");
        this.value = value;
    }

    /**
     * Returns the placeholder key.
     *
     * @return The placeholder key (without braces).
     */
    public PlaceholderKey getKey() {
        return key;
    }

    /**
     * Returns the replacement value as a string.
     *
     * @return The replacement value string, or {@code null} if the value is null.
     */
    @SuppressWarnings("unchecked")
    public <T> T getValue() {
        return Objects.isNull(value) ? null : value.getValue();
    }

    /**
     * Returns the {@link PlaceholderValue} instance.
     *
     * @return The placeholder value instance.
     */
    public PlaceholderValue getPlaceholderValue() {
        return value;
    }
}

