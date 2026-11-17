package com.tlc.crm.core.action.analytics.reports.data;

import com.tlc.analytics.core.api.AxisValue;
import com.tlc.analytics.core.api.GraphAxis;

import com.tlc.commons.util.constants.NumericalConstant;

import com.tlc.crm.common.util.NumberUtils;

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Shared utility methods for extracting and parsing axis values from
 * {@link GraphAxis} data returned by ClickHouse.
 *
 * @author karthick V
 * @version 1.0
 */
final class AxisValueUtils {

    private AxisValueUtils() {
    }

    /**
     * Checks whether the given value represents an empty array.
     *
     * <p>When an array field has no values, ClickHouse may return:</p>
     * <ul>
     *   <li>The string {@code "[]"}</li>
     *   <li>An empty Java array (e.g. {@code long[0]})</li>
     *   <li>An empty {@link Collection}</li>
     * </ul>
     *
     * @param value the value to check
     * @return {@code true} if the value represents an empty array, {@code false} otherwise
     */
    static boolean isEmptyArrayLike(final Object value) {
        if (value instanceof String str) {
            return "[]".equals(str.trim());
        }

        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }

        if (value.getClass().isArray()) {
            return Array.getLength(value) == NumericalConstant.ZERO;
        }

        return false;
    }

    /**
     * Collects numeric entity IDs from a {@link GraphAxis}.
     *
     * <p>If the axis has a non-null, non-empty top-level value, IDs are extracted
     * from that value alone. Otherwise every {@link AxisValue} in the axis is
     * examined.</p>
     *
     * @param graphAxis the axis to extract IDs from
     * @param entityIds the collection to accumulate IDs into
     */
    static void collectEntityIds(final GraphAxis<Object> graphAxis,
                                 final Collection<Long> entityIds) {
        final Object value = graphAxis.getValue();

        if (Objects.nonNull(value) && !isEmptyArrayLike(value)) {
            addAllParseableIds(value, entityIds);
        } else {
            for (final AxisValue<Object> axisValue : graphAxis.getAxisValues()) {
                final Object axisVal = axisValue.getValue();

                if (Objects.nonNull(axisVal) && !isEmptyArrayLike(axisVal)) {
                    addAllParseableIds(axisVal, entityIds);
                }
            }
        }
    }

    /**
     * Extracts parseable numeric IDs from the given value, handling scalars,
     * Java arrays, {@link Collection}s, and string array representations
     * (e.g.&nbsp;{@code "[1, 2]"}).
     *
     * @param value     the value to extract IDs from
     * @param entityIds the collection to add extracted IDs to
     */
    static void addAllParseableIds(final Object value, final Collection<Long> entityIds) {
        final List<Long> ids = mapArrayLikeValue(value, element -> parseToLong(element).orElse(null));

        if (ids != null) {
            ids.stream().filter(Objects::nonNull).forEach(entityIds::add);
        } else {
            addIfParseable(value, entityIds);
        }
    }

    /**
     * Iterates over each element of an array-like value (Collection, Java array,
     * or string representation like {@code "[1, 2, 3]"}) and applies the given
     * mapper, collecting results into a {@link List}.
     *
     * <p>Returns {@code null} if the value is not array-like (i.e. scalar), so
     * callers can distinguish between an empty array result and a non-array value.</p>
     *
     * @param value  the value to iterate; must not be {@code null}
     * @param mapper transform function applied to each element
     * @param <T>    result element type
     * @return list of mapped values, or {@code null} if the value is scalar
     */
    static <T> List<T> mapArrayLikeValue(final Object value, final Function<Object, T> mapper) {
        if (value instanceof Collection<?> collection) {
            final List<T> result = new ArrayList<>(collection.size());

            for (final Object element : collection) {
                result.add(mapper.apply(element));
            }

            return result;
        }

        if (value.getClass().isArray()) {
            final int length = Array.getLength(value);
            final List<T> result = new ArrayList<>(length);

            for (int index = NumericalConstant.ZERO; index < length; index++) {
                result.add(mapper.apply(Array.get(value, index)));
            }

            return result;
        }

        if (value instanceof String str && str.trim().startsWith("[") && str.trim().endsWith("]")) {
            final String inner = str.trim().substring(NumericalConstant.ONE, str.trim().length() - NumericalConstant.ONE).trim();

            if (!inner.isEmpty()) {
                final List<T> result = new ArrayList<>();

                for (final String element : inner.split(",")) {
                    final String trimmed = element.trim();

                    if (!trimmed.isEmpty()) {
                        result.add(mapper.apply(trimmed));
                    }
                }

                return result;
            }
        }

        return null;
    }

    /**
     * Safely parses any axis value into {@link Long}.
     *
     * <p>Returns {@code null} for null values, empty array-like values,
     * Java arrays, collections, or when parsing fails.</p>
     *
     * @param value the value to parse
     * @return an {@link Optional} containing the parsed long value, or empty when the value cannot be converted
     */
    static Optional<Long> parseToLong(final Object value) {
        if (value == null || isEmptyArrayLike(value)) {
            return Optional.empty();
        }

        if (value instanceof Collection<?> || value.getClass().isArray()) {
            return Optional.empty();
        }

        try {
            final Number parsedNumber = NumberUtils.parseNumber(value);

            return Optional.ofNullable(parsedNumber != null ? parsedNumber.longValue() : null);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    /**
     * Parses the value to {@link Long} and adds it to the collection if successful.
     *
     * @param value     the value to parse
     * @param entityIds the collection to add the parsed ID to
     */
    private static void addIfParseable(final Object value, final Collection<Long> entityIds) {
        parseToLong(value).ifPresent(entityIds::add);
    }
}

