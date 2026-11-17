package com.tlc.crm.core.api.analytics.entity.config.function;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.analytics.config.model.view.filter.OperatorType;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;


/**
 * <p>
 * A model that manages the function details
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
public enum CRMFunction
{
    TODAY(3, "TODAY", "i18n.crm.analytics.function.today", true),
    TOMORROW(4, "TOMORROW", "i18n.crm.analytics.function.tomorrow", false),
    HOUR_OF_THE_DAY(5, "HOUR_OF_THE_DAY", "i18n.crm.analytics.function.hour.of.the.day", false),
    DAY_OF_THE_WEEK(6, "DAY_OF_THE_WEEK", "i18n.crm.analytics.function.day.of.the.week", false),
    DAY_OF_THE_MONTH(7, "DAY_OF_THE_MONTH", "i18n.crm.analytics.function.day.of.the.month", false),
    MONTH_OF_THE_YEAR(8, "MONTH_OF_THE_YEAR", "i18n.crm.analytics.function.month.of.the.year", false),
    QUARTER_OF_THE_YEAR(9, "QUARTER_OF_THE_YEAR", "i18n.crm.analytics.function.quarter.of.the.year", false),
    BY_MONTH(10, "BY_MONTH", "i18n.crm.analytics.function.by.month", false),
    BY_WEEK(11, "BY_WEEK", "i18n.crm.analytics.function.by.week", false),
    BY_YEAR(12, "BY_YEAR", "i18n.crm.analytics.function.by.year", false),
    PREVIOUS_PERIOD(14, "PREVIOUS_PERIOD", "i18n.crm.analytics.function.previous.period", false),
    PREVIOUS_PERIOD_RELATIVE(15, "PREVIOUS_PERIOD_RELATIVE", "i18n.crm.analytics.function.previous.period.relative", false),
    SAME_PERIOD_LAST_YEAR(16, "SAME_PERIOD_LAST_YEAR", "i18n.crm.analytics.function.same.period.last.year", false),
    THIS_WEEK(18, "THIS_WEEK", "i18n.crm.analytics.function.this.week", true),
    THIS_MONTH(19, "THIS_MONTH", "i18n.crm.analytics.function.this.month", true),
    THIS_QUARTER(20, "THIS_QUARTER", "i18n.crm.analytics.function.this.quarter", true),
    THIS_YEAR(21, "THIS_YEAR", "i18n.crm.analytics.function.this.year", true),
    SECONDS(22, "SECONDS", "i18n.crm.analytics.function.seconds", true),
    MINUTES(23, "MINUTES", "i18n.crm.analytics.function.minutes", true),
    HOURS(24, "HOURS", "i18n.crm.analytics.function.hours", true),
    DAYS(25, "DAYS", "i18n.crm.analytics.function.days", true),
    WEEKS(26, "WEEKS", "i18n.crm.analytics.function.weeks", true),
    MONTHS(27, "MONTHS", "i18n.crm.analytics.function.months", true),
    YEARS(28, "YEARS", "i18n.crm.analytics.function.years", true),
    YESTERDAY(29, "YESTERDAY", "i18n.crm.analytics.function.yesterday", true),
    LAST_WEEK(30, "LAST_WEEK", "i18n.crm.analytics.function.last.week", true),
    LAST_MONTH(31, "LAST_MONTH", "i18n.crm.analytics.function.last.month", true),
    LAST_QUARTER(32, "LAST_QUARTER", "i18n.crm.analytics.function.last.quarter", true),
    LAST_YEAR(33, "LAST_YEAR", "i18n.crm.analytics.function.last.year", true),
    SUNDAY(34, "SUNDAY", "i18n.crm.analytics.function.sunday", true),
    MONDAY(35, "MONDAY", "i18n.crm.analytics.function.monday", true),
    TUESDAY(36, "TUESDAY", "i18n.crm.analytics.function.tuesday", true),
    WEDNESDAY(37, "WEDNESDAY", "i18n.crm.analytics.function.wednesday", true),
    THURSDAY(38, "THURSDAY", "i18n.crm.analytics.function.thursday", true),
    FRIDAY(39, "FRIDAY", "i18n.crm.analytics.function.friday", true),
    SATURDAY(40, "SATURDAY", "i18n.crm.analytics.function.saturday", true),
    CUSTOM(41, "CUSTOM", "i18n.crm.analytics.function.custom", true),
    DATE(42, "DATE", "i18n.crm.analytics.function.date", true);

    private final int typeCode;
    private final String functionName;
    private final String i18nKeyOfDisplayName;
    private final boolean isPeriodicFilter;

    CRMFunction(int typeCode, String functionName, String i18nKeyOfDisplayName, boolean isPeriodicFilter) {
        this.typeCode = typeCode;
        this.functionName = functionName;
        this.i18nKeyOfDisplayName = i18nKeyOfDisplayName;
        this.isPeriodicFilter = isPeriodicFilter;
    }

    /**
     * Retrieves the list of supported Functions for a given OperatorType.
     *
     * @param operatorTypeId The OperatorType code to check.
     * @return A Set of supported Functions.
     */
    public static Set<CRMFunction> getSupportedFunctions(final int operatorTypeId) {
        final OperatorType operatorType = OperatorType.getOperatorType(operatorTypeId);

        return switch (operatorType) {
            case ON -> EnumSet.of(TODAY, THIS_WEEK, THIS_MONTH, THIS_QUARTER, THIS_YEAR,
                    YESTERDAY, LAST_WEEK, LAST_MONTH, LAST_QUARTER, LAST_YEAR);

            case DAY_IS -> EnumSet.of(SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY);

            case AFTER, BEFORE, SINCE -> EnumSet.of(TODAY, THIS_WEEK, THIS_MONTH, THIS_QUARTER, THIS_YEAR,
                    YESTERDAY, LAST_WEEK, LAST_MONTH, LAST_QUARTER, LAST_YEAR, CUSTOM);

            case IN_THE_LAST, NOT_IN_THE_LAST -> EnumSet.of(SECONDS, MINUTES, HOURS, DAYS, WEEKS, MONTHS, YEARS);

            default -> Collections.emptySet();
        };
    }

    /**
     * Retrieves the {@link CRMFunction} corresponding to the given numeric ID.
     *
     * @param id The numeric type code of the function.
     * @return The matching {@link CRMFunction} constant.
     * @throws ErrorCode if the provided ID does not match any known function.
     */
    public static CRMFunction getFunctionById(final int id) {
        return switch (id) {
            case 3 -> TODAY;
            case 4 -> TOMORROW;
            case 5 -> HOUR_OF_THE_DAY;
            case 6 -> DAY_OF_THE_WEEK;
            case 7 -> DAY_OF_THE_MONTH;
            case 8 -> MONTH_OF_THE_YEAR;
            case 9 -> QUARTER_OF_THE_YEAR;
            case 10 -> BY_MONTH;
            case 11 -> BY_WEEK;
            case 12 -> BY_YEAR;
            case 14 -> PREVIOUS_PERIOD;
            case 15 -> PREVIOUS_PERIOD_RELATIVE;
            case 16 -> SAME_PERIOD_LAST_YEAR;
            case 18 -> THIS_WEEK;
            case 19 -> THIS_MONTH;
            case 20 -> THIS_QUARTER;
            case 21 -> THIS_YEAR;
            case 22 -> SECONDS;
            case 23 -> MINUTES;
            case 24 -> HOURS;
            case 25 -> DAYS;
            case 26 -> WEEKS;
            case 27 -> MONTHS;
            case 28 -> YEARS;
            case 29 -> YESTERDAY;
            case 30 -> LAST_WEEK;
            case 31 -> LAST_MONTH;
            case 32 -> LAST_QUARTER;
            case 33 -> LAST_YEAR;
            case 34 -> SUNDAY;
            case 35 -> MONDAY;
            case 36 -> TUESDAY;
            case 37 -> WEDNESDAY;
            case 38 -> THURSDAY;
            case 39 -> FRIDAY;
            case 40 -> SATURDAY;
            case 41 -> CUSTOM;
            case 42 -> DATE;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_FUNCTION,
                    "i18n.analytics.invalid.function");
        };
    }

    /**
     * Retrieves the {@link CRMFunction} corresponding to the given function name (case-insensitive).
     *
     * @param functionName The name of the function (e.g., {@code "today"}, {@code "last_week"}).
     * @return The matching {@link CRMFunction} constant.
     * @throws ErrorCode if the provided name does not match any known function.
     */
    public static CRMFunction getFunctionByName(final String functionName) {
        return switch (functionName.toLowerCase()) {
            case "today" -> TODAY;
            case "tomorrow" -> TOMORROW;
            case "hour_of_the_day" -> HOUR_OF_THE_DAY;
            case "day_of_the_week" -> DAY_OF_THE_WEEK;
            case "day_of_the_month" -> DAY_OF_THE_MONTH;
            case "month_of_the_year" -> MONTH_OF_THE_YEAR;
            case "quarter_of_the_year" -> QUARTER_OF_THE_YEAR;
            case "by_month" -> BY_MONTH;
            case "by_week" -> BY_WEEK;
            case "by_year" -> BY_YEAR;
            case "previous_period" -> PREVIOUS_PERIOD;
            case "previous_period_relative" -> PREVIOUS_PERIOD_RELATIVE;
            case "same_period_last_year" -> SAME_PERIOD_LAST_YEAR;
            case "this_week" -> THIS_WEEK;
            case "this_month" -> THIS_MONTH;
            case "this_quarter" -> THIS_QUARTER;
            case "this_year" -> THIS_YEAR;
            case "seconds" -> SECONDS;
            case "minutes" -> MINUTES;
            case "hours" -> HOURS;
            case "days" -> DAYS;
            case "weeks" -> WEEKS;
            case "months" -> MONTHS;
            case "years" -> YEARS;
            case "yesterday" -> YESTERDAY;
            case "last_week" -> LAST_WEEK;
            case "last_month" -> LAST_MONTH;
            case "last_quarter" -> LAST_QUARTER;
            case "last_year" -> LAST_YEAR;
            case "sunday" -> SUNDAY;
            case "monday" -> MONDAY;
            case "tuesday" -> TUESDAY;
            case "wednesday" -> WEDNESDAY;
            case "thursday" -> THURSDAY;
            case "friday" -> FRIDAY;
            case "saturday" -> SATURDAY;
            case "custom" -> CUSTOM;
            case "date" -> DATE;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_FUNCTION,
                    "i18n.analytics.invalid.function");
        };
    }
}