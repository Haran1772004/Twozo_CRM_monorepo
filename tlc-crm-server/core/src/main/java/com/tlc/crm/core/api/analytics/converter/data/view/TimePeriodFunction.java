package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.core.api.AxisValue;

import com.tlc.i18n.I18nResolver;

import lombok.NonNull;

import java.time.DayOfWeek;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.stream.IntStream;
import java.util.Collection;

/**
 * <p>
 * A enum that presents the {@link TimePeriodFunction} details
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */

public enum TimePeriodFunction {

    MONTH_OF_THE_YEAR {

        /**
         * <p>
         * Populates the value map with Y-axis values corresponding to the X-axis data map.
         * </p>
         *
         * @param yAxisValues  the list of Y-axis values aligned with the X-axis indices
         * @param xAxisDataMap the map containing X-axis values indexed by position
         * @param valueMap     the map to be populated with corresponding values
         * @param i18nResolver resolver used for localizing labels if required
         */
        @Override
        public void populate(final List<AxisValue<Object>> yAxisValues,
                             final Map<Integer, Object> xAxisDataMap,
                             final Map<?, Object> valueMap,
                             final @NonNull I18nResolver i18nResolver) {
            AxisValueConvertor.getMonthToValue(xAxisDataMap.size(),
                    safeSize(yAxisValues),
                    yAxisValues,
                    (Map<String, Object>) valueMap,
                    xAxisDataMap,
                    i18nResolver);
        }

        /**
         * <p>
         * Builds and returns the ordered list of keys for this axis.
         * </p>
         *
         * @param i18nResolver resolver used for localizing axis labels if required
         * @return a list of keys representing the X-axis values
         */
        @Override
        public Collection<String> buildKeys(final @NonNull I18nResolver i18nResolver) {
            return Arrays.stream(java.time.Month.values())
                    .map(month -> i18nResolver.get("i18n.analytics.month." + month.name().toLowerCase()))
                    .toList();
        }
    },

    HOUR_OF_THE_DAY {

        /**
         * <p>
         * Populates the value map with Y-axis values corresponding to the X-axis data map.
         * </p>
         *
         * @param yAxisValues  the list of Y-axis values aligned with the X-axis indices
         * @param xAxisDataMap the map containing X-axis values indexed by position
         * @param valueMap     the map to be populated with corresponding values
         * @param i18nResolver resolver used for localizing labels if required
         */
        @Override
        public void populate(final List<AxisValue<Object>> yAxisValues,
                             final Map<Integer, Object> xAxisDataMap,
                             final Map<?, Object> valueMap,
                             final @NonNull I18nResolver i18nResolver) {
            AxisValueConvertor.getHourToValue(xAxisDataMap.size(),
                    safeSize(yAxisValues),
                    yAxisValues,
                    (Map<Integer, Object>) valueMap,
                    xAxisDataMap);
        }

        /**
         * <p>
         * Builds and returns the ordered list of keys for this axis.
         * </p>
         *
         * @param i18nResolver resolver used for localizing axis labels if required
         * @return a list of keys representing the X-axis values
         */
        @Override
        public Collection<Integer> buildKeys(final @NonNull I18nResolver i18nResolver) {
            return IntStream.range(ZERO_INDEX, TOTAL_HOURS).boxed().toList();
        }
    },

    DAY_OF_THE_WEEK {

        /**
         * <p>
         * Populates the value map with Y-axis values corresponding to the X-axis data map.
         * </p>
         *
         * @param yAxisValues  the list of Y-axis values aligned with the X-axis indices
         * @param xAxisDataMap the map containing X-axis values indexed by position
         * @param valueMap     the map to be populated with corresponding values
         * @param i18nResolver resolver used for localizing labels if required
         */
        @Override
        public void populate(final List<AxisValue<Object>> yAxisValues,
                             final Map<Integer, Object> xAxisDataMap,
                             final Map<?, Object> valueMap,
                             final @NonNull I18nResolver i18nResolver) {
            final Collection<String> reordered = new ArrayList<>();

            AxisValueConvertor.getDayToValue(xAxisDataMap.size(),
                    safeSize(yAxisValues),
                    yAxisValues,
                    reordered,
                    (Map<String, Object>) valueMap,
                    xAxisDataMap,
                    i18nResolver);
        }

        /**
         * <p>
         * Builds and returns the ordered list of keys for this axis.
         * </p>
         *
         * @param i18nResolver resolver used for localizing axis labels if required
         * @return a list of keys representing the X-axis values
         */
        @Override
        public Collection<String> buildKeys(final @NonNull I18nResolver i18nResolver) {
            final String sunday = i18nResolver.get("i18n.analytics.day.sunday");
            final Collection<String> days = Arrays.stream(DayOfWeek.values())
                    .map(day -> i18nResolver.get("i18n.analytics.day." + day.name().toLowerCase()))
                    .toList();
            final Collection<String> reordered = new ArrayList<>();

            reordered.add(sunday);
            reordered.addAll(days.stream().filter(day -> !day.equals(sunday)).toList());
            return reordered;
        }
    },

    QUARTER_OF_THE_YEAR {

        /**
         * <p>
         * Populates the value map with Y-axis values corresponding to the X-axis data map.
         * </p>
         *
         * @param yAxisValues  the list of Y-axis values aligned with the X-axis indices
         * @param xAxisDataMap the map containing X-axis values indexed by position
         * @param valueMap     the map to be populated with corresponding values
         * @param i18nResolver resolver used for localizing labels if required
         */
        @Override
        public void populate(final List<AxisValue<Object>> yAxisValues,
                             final Map<Integer, Object> xAxisDataMap,
                             final Map<?, Object> valueMap,
                             final @NonNull I18nResolver i18nResolver) {
            AxisValueConvertor.getQuarterToValue(xAxisDataMap.size(),
                    safeSize(yAxisValues),
                    yAxisValues,
                    (Map<Integer, Object>) valueMap,
                    xAxisDataMap);
        }

        /**
         * <p>
         * Builds and returns the ordered list of keys for this axis.
         * </p>
         *
         * @param i18nResolver resolver used for localizing axis labels if required
         * @return a list of keys representing the X-axis values
         */
        @Override
        public Collection<Integer> buildKeys(final @NonNull I18nResolver i18nResolver) {
            return IntStream.rangeClosed(DEFAULT_START_VALUE, TOTAL_QUARTERS).boxed().toList();
        }
    },

    DAY_OF_THE_MONTH {

        /**
         * <p>
         * Populates the value map with Y-axis values corresponding to the X-axis data map.
         * </p>
         *
         * @param yAxisValues  the list of Y-axis values aligned with the X-axis indices
         * @param xAxisDataMap the map containing X-axis values indexed by position
         * @param valueMap     the map to be populated with corresponding values
         * @param i18nResolver resolver used for localizing labels if required
         */
        @Override
        public void populate(final List<AxisValue<Object>> yAxisValues,
                             final Map<Integer, Object> xAxisDataMap,
                             final Map<?, Object> valueMap,
                             final @NonNull I18nResolver i18nResolver) {
            AxisValueConvertor.getDateToValue(xAxisDataMap.size(),
                    safeSize(yAxisValues),
                    yAxisValues,
                    (Map<Integer, Object>) valueMap,
                    xAxisDataMap);
        }

        /**
         * <p>
         * Builds and returns the ordered list of keys for this axis.
         * </p>
         *
         * @param i18nResolver resolver used for localizing axis labels if required
         * @return a list of keys representing the X-axis values
         */
        @Override
        public Collection<Integer> buildKeys(final @NonNull I18nResolver i18nResolver) {
            return IntStream.rangeClosed(DEFAULT_START_VALUE, TOTAL_DATES).boxed().toList();
        }
    },

    BY_MONTH {

        /**
         * <p>
         * Populates the value map with Y-axis values corresponding to the X-axis data map.
         * The month number (1-12) is converted to the localized month name.
         * Only months with data are included, inserted in calendar order (January to December).
         * </p>
         *
         * @param yAxisValues  the list of Y-axis values aligned with the X-axis indices
         * @param xAxisDataMap the map containing X-axis values indexed by position
         * @param valueMap     the map to be populated with corresponding values
         * @param i18nResolver resolver used for localizing labels if required
         */
        @Override
        public void populate(final List<AxisValue<Object>> yAxisValues,
                             final Map<Integer, Object> xAxisDataMap,
                             final Map<?, Object> valueMap,
                             final @NonNull I18nResolver i18nResolver) {
            final Map<String, Object> tempMap = new java.util.HashMap<>();

            AxisValueConvertor.getMonthToValue(xAxisDataMap.size(),
                    safeSize(yAxisValues),
                    yAxisValues,
                    tempMap,
                    xAxisDataMap,
                    i18nResolver);

            final Map<String, Object> orderedMap = (Map<String, Object>) valueMap;

            Arrays.stream(java.time.Month.values())
                    .map(month -> i18nResolver.get("i18n.analytics.month." + month.name().toLowerCase()))
                    .filter(tempMap::containsKey)
                    .forEach(monthName -> orderedMap.put(monthName, tempMap.get(monthName)));
        }

        /**
         * <p>
         * Builds and returns the ordered list of keys for this axis.
         * Since populate() already inserts only months with data in calendar order,
         * this method is not used for BY_MONTH (the caller uses valueMap.keySet() directly).
         * </p>
         *
         * @param i18nResolver resolver used for localizing axis labels if required
         * @return an empty list as keys are derived from valueMap
         */
        @Override
        public Collection<String> buildKeys(final @NonNull I18nResolver i18nResolver) {
            return List.of();
        }
    };

    private static final int ZERO_INDEX = 0;
    private static final int DEFAULT_START_VALUE = 1;
    private static final int TOTAL_HOURS = 24;
    private static final int TOTAL_DATES = 31;
    private static final int TOTAL_QUARTERS = 4;

    /**
     * <p>
     * Populates the value map with Y-axis values corresponding to the X-axis data map.
     * </p>
     *
     * @param yAxisValues  the list of Y-axis values aligned with the X-axis indices
     * @param xAxisDataMap the map containing X-axis values indexed by position
     * @param valueMap     the map to be populated with corresponding values
     * @param i18nResolver resolver used for localizing labels if required
     */
    public abstract void populate(final List<AxisValue<Object>> yAxisValues,
                                  final Map<Integer, Object> xAxisDataMap,
                                  final Map<?, Object> valueMap,
                                  final @NonNull I18nResolver i18nResolver);

    /**
     * <p>
     * Builds and returns the ordered list of keys for this axis.
     * </p>
     *
     * @param i18nResolver resolver used for localizing axis labels if required
     * @return a list of keys representing the X-axis values
     */
    public abstract Collection<?> buildKeys(final @NonNull I18nResolver i18nResolver);

    /**
     * <p>
     * Safely returns the size of the provided list.
     * </p>
     *
     * @param list the list whose size is to be determined
     * @return the size of the list, or zero if the list is null
     */
    private static int safeSize(final List<AxisValue<Object>> list) {
        return Objects.nonNull(list) ? list.size() : ZERO_INDEX;
    }

    /**
     * <p>
     * Returns the corresponding {@link TimePeriodFunction} enum for the given string label.
     * </p>
     *
     * @param label the string label representing an axis type
     * @return the corresponding {@link TimePeriodFunction}, or null if no match is found
     */
    public static TimePeriodFunction get(final String label) {
        if (Objects.isNull(label)) {
            return null;
        }
        final TimePeriodFunction[] values = TimePeriodFunction.values();

        for (TimePeriodFunction timePeriodFunction : values) {

            if (timePeriodFunction.name().equalsIgnoreCase(label)) {
                return timePeriodFunction;
            }
        }

        return null;
    }
}
