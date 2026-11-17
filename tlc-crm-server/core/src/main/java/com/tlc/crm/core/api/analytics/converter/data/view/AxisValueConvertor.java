package com.tlc.crm.core.api.analytics.converter.data.view;

import com.tlc.analytics.core.api.AxisValue;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;
import com.tlc.crm.common.util.NumberUtils;

import com.tlc.commons.code.ErrorCode;

import com.tlc.i18n.I18nResolver;

import java.time.DayOfWeek;
import java.time.Month;

import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.Collection;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A enum that presents the {@link AxisValueConvertor} details
 * </p>
 *
 * @author Ryan
 * @version 1.0
 */

@NoArgsConstructor( access = AccessLevel.PRIVATE )
public final class AxisValueConvertor {
    private static final Logger LOGGER = LoggerFactory.getLogger(AxisValueConvertor.class);
    private static final String EMPTY_STRING = "";

    /**
     * <p>
     * Populates the mapping of hour values to corresponding Y-axis values.
     * </p>
     *
     * @param xAxisDataMapSize the number of entries in the X-axis data map
     * @param yAxisValuesSize  the number of entries in the Y-axis values list
     * @param yAxisValues      the list of Y-axis values aligned with the X-axis indices
     * @param hourToValue      the map to be populated with hour-to-value mappings
     * @param xAxisDataMap     the map containing X-axis values indexed by position
     * @throws ErrorCode if the X-axis value cannot be parsed into a valid hour
     */
    public static void getHourToValue(final int xAxisDataMapSize,
                                      final int yAxisValuesSize,
                                      final List<AxisValue<Object>> yAxisValues,
                                      final Map<Integer, Object> hourToValue,
                                      final Map<Integer, Object> xAxisDataMap) {
        for (int index = NumericalConstant.ZERO; index < xAxisDataMapSize; index++) {

            try {
                final String hourValue = Objects.toString(xAxisDataMap.get(index), EMPTY_STRING).trim();

                hourToValue.put(hourValue.isEmpty() ? NumericalConstant.ZERO
                                : NumberUtils.parseNumber(hourValue).intValue(),
                        getValue(yAxisValues, yAxisValuesSize, index));
            } catch (final Exception error) {
                LOGGER.error("CRM Analytics: Invalid hour value", error);
                throw ErrorCode.get(AnalyticsErrorCodes.INVALID_DATE_VALUE);
            }

        }
    }

    /**
     * <p>
     * Populates the mapping of month names to corresponding Y-axis values.
     * The month number (1-12) from the X-axis data is converted to the localized month name.
     * </p>
     *
     * @param xAxisDataMapSize the number of entries in the X-axis data map
     * @param yAxisValuesSize  the number of entries in the Y-axis values list
     * @param yAxisValues      the list of Y-axis values aligned with the X-axis indices
     * @param monthToValue     the map to be populated with month-to-value mappings
     * @param xAxisDataMap     the map containing X-axis values indexed by position
     * @param i18nResolver     the resolver used for fetching localized month names
     * @throws ErrorCode if the month value cannot be parsed into a valid integer
     */
    public static void getMonthToValue(final int xAxisDataMapSize,
                                       final int yAxisValuesSize,
                                       final List<AxisValue<Object>> yAxisValues,
                                       final Map<String, Object> monthToValue,
                                       final Map<Integer, Object> xAxisDataMap,
                                       final I18nResolver i18nResolver) {
        for (int index = NumericalConstant.ZERO; index < xAxisDataMapSize; index++) {

            try {
                final String monthValue = Objects.toString(xAxisDataMap.get(index), EMPTY_STRING).trim();
                final int monthNumber = monthValue.isEmpty() ? NumericalConstant.ZERO
                        : NumberUtils.parseNumber(monthValue).intValue();
                final String monthName = monthNumber >= NumericalConstant.ONE && monthNumber <= NumericalConstant.TWELVE
                        ? i18nResolver.get("i18n.analytics.month." + Month.of(monthNumber).name().toLowerCase())
                        : EMPTY_STRING;

                monthToValue.put(monthName, getValue(yAxisValues, yAxisValuesSize, index));
            } catch (final Exception error) {
                LOGGER.error("CRM Analytics: Invalid month value", error);
                throw ErrorCode.get(AnalyticsErrorCodes.INVALID_DATE_VALUE);
            }

        }
    }

    /**
     * <p>
     * Populates the mapping of localized day names to corresponding Y-axis values.
     * </p>
     *
     * @param xAxisDataMapSize the number of entries in the X-axis data map
     * @param yAxisValuesSize  the number of entries in the Y-axis values list
     * @param yAxisValues      the list of Y-axis values aligned with the X-axis indices
     * @param reorderedDays    the list to be populated with reordered localized day names (Sunday first)
     * @param dayToValue       the map to be populated with day-to-value mappings
     * @param xAxisDataMap     the map containing X-axis values indexed by position
     * @param i18nResolver     the resolver used for fetching localized day names
     */
    public static void getDayToValue(final int xAxisDataMapSize,
                                     final int yAxisValuesSize,
                                     final List<AxisValue<Object>> yAxisValues,
                                     final Collection<String> reorderedDays,
                                     final Map<String, Object> dayToValue,
                                     final Map<Integer, Object> xAxisDataMap,
                                     final I18nResolver i18nResolver) {
        final String sunday = i18nResolver.get("i18n.analytics.day.sunday");

        reorderedDays.add(sunday);
        reorderedDays.addAll(Arrays.stream(DayOfWeek.values())
                .map(day -> i18nResolver.get("i18n.analytics.day." + day.name().toLowerCase()))
                .filter(day -> !day.equals(sunday))
                .toList());

        for (int index = NumericalConstant.ZERO; index < xAxisDataMapSize; index++) {

            try {
                final String dayValue = Objects.toString(xAxisDataMap.get(index), EMPTY_STRING).trim();
                final int dayNumber = dayValue.isEmpty() ? NumericalConstant.ZERO
                        : NumberUtils.parseNumber(dayValue).intValue();
                final String dayName = getDayName(dayNumber, i18nResolver);

                dayToValue.put(dayName, getValue(yAxisValues, yAxisValuesSize, index));
            } catch (final Exception error) {
                LOGGER.error("CRM Analytics: Invalid day value", error);
                throw ErrorCode.get(AnalyticsErrorCodes.INVALID_DATE_VALUE);
            }

        }
    }

    /**
     * <p>
     * Converts a weekday number (1-7) to the localized day name.
     * 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday.
     * </p>
     *
     * @param dayNumber    the weekday number (1-7)
     * @param i18nResolver the resolver used for fetching localized day names
     * @return the localized day name, or empty string if the day number is invalid
     */
    private static String getDayName(final int dayNumber, final I18nResolver i18nResolver) {
        if (dayNumber >= NumericalConstant.ONE && dayNumber <= NumericalConstant.SEVEN) {
            return i18nResolver.get("i18n.analytics.day." + DayOfWeek.of(dayNumber).name().toLowerCase());
        }

        return EMPTY_STRING;
    }

    /**
     * <p>
     * Populates the mapping of quarter numbers to corresponding Y-axis values.
     * </p>
     *
     * @param xAxisDataMapSize the number of entries in the X-axis data map
     * @param yAxisValuesSize  the number of entries in the Y-axis values list
     * @param yAxisValues      the list of Y-axis values aligned with the X-axis indices
     * @param quarterToValue   the map to be populated with quarter-to-value mappings
     * @param xAxisDataMap     the map containing X-axis values indexed by position
     * @throws ErrorCode if the quarter value cannot be parsed into a valid integer
     */
    public static void getQuarterToValue(final int xAxisDataMapSize,
                                         final int yAxisValuesSize,
                                         final List<AxisValue<Object>> yAxisValues,
                                         final Map<Integer, Object> quarterToValue,
                                         final Map<Integer, Object> xAxisDataMap) {
        for (int index = NumericalConstant.ZERO; index < xAxisDataMapSize; index++) {

            try {
                final String quarterValue = Objects.toString(xAxisDataMap.get(index), EMPTY_STRING).trim();

                quarterToValue.put(quarterValue.isEmpty() ? NumericalConstant.ZERO
                        : NumberUtils.parseNumber(quarterValue).intValue(), getValue(yAxisValues, yAxisValuesSize, index));
            } catch (final Exception error) {
                LOGGER.error("CRM Analytics: Invalid quarter value", error);
                throw ErrorCode.get(AnalyticsErrorCodes.INVALID_DATE_VALUE);
            }

        }
    }

    /**
     * <p>
     * Populates the mapping of day numbers (dates) to corresponding Y-axis values.
     * </p>
     *
     * @param xAxisDataMapSize the number of entries in the X-axis data map
     * @param yAxisValuesSize  the number of entries in the Y-axis values list
     * @param yAxisValues      the list of Y-axis values aligned with the X-axis indices
     * @param dateToValue      the map to be populated with date-to-value mappings
     * @param xAxisDataMap     the map containing X-axis values indexed by position
     * @throws ErrorCode if the date value cannot be parsed into a valid integer
     */
    public static void getDateToValue(final int xAxisDataMapSize,
                                      final int yAxisValuesSize,
                                      final List<AxisValue<Object>> yAxisValues,
                                      final Map<Integer, Object> dateToValue,
                                      final Map<Integer, Object> xAxisDataMap) {
        for (int index = NumericalConstant.ZERO; index < xAxisDataMapSize; index++) {

            try {
                final String dateValue = Objects.toString(xAxisDataMap.get(index), EMPTY_STRING).trim();

                dateToValue.put(dateValue.isEmpty() ? NumericalConstant.ZERO
                        : NumberUtils.parseNumber(dateValue).intValue(), getValue(yAxisValues, yAxisValuesSize, index));
            } catch (final Exception error) {
                LOGGER.error("CRM Analytics: Invalid date value", error);
                throw ErrorCode.get(AnalyticsErrorCodes.INVALID_DATE_VALUE);
            }

        }
    }

    /**
     * <p>
     * Returns value from yAxis Values
     * </p>
     *
     * @param yAxisValues      the list of Y-axis values aligned with the X-axis indices
     * @param yAxisValueSize   the number of entries in the Y-axis values list
     * @param index            index of xAxisDataMap
     */
    private static Object getValue(final List<AxisValue<Object>> yAxisValues, final int yAxisValueSize, final int index){
        return (Objects.nonNull(yAxisValues) && index < yAxisValueSize) ? yAxisValues.get(index).getValue() : null;
    }

}
