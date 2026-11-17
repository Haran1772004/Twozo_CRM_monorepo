package com.tlc.analytics.general.utils;

import com.tlc.commons.util.constants.NumericalConstant;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class that provides methods for normalizing and converting raw values
 * into collections
 *
 * @author saravanan
 * @version 1.0
 */
public class ValueNormalizer {

    /**
     * Normalizes the given raw value into a {@link Collection} of values.
     *
     * @param rawValue the raw value to normalize; may be {@code null}, a {@link Collection}, a {@link String}, or any object
     * @return a non-null {@link Collection} of normalized string values
     */
    public static Collection<?> normalizeToCollection(final Object rawValue) {
        if (Objects.isNull(rawValue)) {
            return List.of();
        }

        if (rawValue instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).map(String::trim).filter(value -> !value.isEmpty())
                    .collect(Collectors.toList());
        } else if (rawValue instanceof String valueAsString) {

            if (valueAsString.startsWith("[") && valueAsString.endsWith("]")) {
                valueAsString = valueAsString.substring(NumericalConstant.ONE, valueAsString.length() - NumericalConstant.ONE);
            }
            return Arrays.stream(valueAsString.split(",")).map(String::trim).filter(value -> !value.isEmpty())
                    .collect(Collectors.toList());
        } else {
            return List.of(String.valueOf(rawValue).trim());
        }
    }

    /**
     * Normalizes the given value to String
     *
     * @param filterValue the raw value to normalize;
     * @return normalized string value
     */
    public static String normalizeToString (final Object filterValue) {
        if (Objects.isNull(filterValue)) {
            return null;
        }

        String value = String.valueOf(filterValue).trim();

        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(NumericalConstant.ONE, value.length() - NumericalConstant.ONE).trim();
        }

        return value;
    }
}
