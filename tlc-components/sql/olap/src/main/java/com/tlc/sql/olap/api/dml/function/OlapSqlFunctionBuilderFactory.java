package com.tlc.sql.olap.api.dml.function;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.sql.common.api.dml.Column;
import com.tlc.sql.common.api.dml.function.SimpleArgument;
import com.tlc.sql.common.api.dml.function.FunctionArgument;
import com.tlc.sql.common.api.dml.function.SqlFunction;
import com.tlc.sql.common.api.dml.function.StringValue;
import com.tlc.sql.common.api.dml.function.NumberValue;
import com.tlc.sql.common.api.meta.CommonDataType;
import com.tlc.sql.olap.api.meta.OlapDataType;
import lombok.NoArgsConstructor;

import java.util.Optional;

/**
 * @author Sundar
 * @version 1.0
 */
@NoArgsConstructor
public final class OlapSqlFunctionBuilderFactory {

    private static final String TO_START_OF_DAY = "toStartOfDay";
    private static final String TO_START_OF_WEEK = "toStartOfWeek";
    private static final String TO_START_OF_MONTH = "toStartOfMonth";
    private static final String TO_START_OF_QUARTER = "toStartOfQuarter";
    private static final String TO_START_OF_YEAR = "toStartOfYear";
    private static final String TO_TIME_ZONE = "toTimeZone";
    private static final String TO_DATE = "toDate";
    private static final String TO_DATE_TIME = "toDateTime";
    private static final String TO_DATE_TIME_64 = "toDateTime64";
    private static final String SUBTRACT_DAYS = "subtractDays";
    private static final String SUBTRACT_WEEKS = "subtractWeeks";
    private static final String SUBTRACT_MONTHS = "subtractMonths";
    private static final String SUBTRACT_QUARTERS = "subtractQuarters";
    private static final String SUBTRACT_YEARS = "subtractYears";
    private static final String ADD_DAYS = "addDays";
    private static final String ADD_WEEKS = "addWeeks";
    private static final String ADD_MONTHS = "addMonths";
    private static final String ADD_QUARTERS = "addQuarters";
    private static final String ADD_YEARS = "addYears";
    private static final String NOW = "now";
    private static final String NOW_64 = "now64";
    private static final String FROM_UNIX_TIMESTAMP_64_MILLI = "fromUnixTimestamp64Milli";
    private static final String CITY_HASH_64 = "cityHash64";
    private static final String ARG_MAX = "argMax";
    private static final String TO_YYYY_MM = "toYYYYMM";
    private static final String TO_HOUR = "toHour";
    private static final String TO_DAY_OF_MONTH = "toDayOfMonth";
    private static final String TO_QUARTER = "toQuarter";
    private static final String TO_MONTH = "toMonth";
    private static final String TO_DAY_OF_WEEK = "toDayOfWeek";
    private static final String TO_YEAR = "toYear";
    private static final String ARRAY_JOIN = "arrayJoin";
    private static final String UTC = "UTC";
    private static final String INTERVAL_DAY = "DAY";
    private static final String INTERVAL_WEEK = "WEEK";
    private static final String INTERVAL_MONTH = "MONTH";
    private static final String INTERVAL_QUARTER = "QUARTER";
    private static final String INTERVAL_YEAR = "YEAR";
    private static final String INTERVAL_SECOND = "SECOND";
    private static final String INTERVAL_MINUTE = "MINUTE";
    private static final String INTERVAL_HOUR = "HOUR";
    private static final String SUBTRACT_SECONDS = "subtractSeconds";

    /**
     * This method retrieves a SqlFunction builder based on the provided function name
     *
     * @param name The name of the ClickHouse function to retrieve, must not be null
     * @return An Optional containing the corresponding SqlFunction.Builder, or empty if the function is not found
     */
    public static Optional<SqlFunction.Builder> getBuilder(final String name) {

        return Optional.ofNullable(switch (name.toLowerCase()) {
            case "toyyyymm" -> toYyyyMm();
            case "cityhash64" -> cityHash64();
            case "tostartofquarter" -> toStartOfQuarter();
            default -> null;
        });
    }

    /**
     * This method creates a builder for the argMax ClickHouse function
     *
     * @return A SqlFunction.Builder configured with the argMax function name
     */
    public static SqlFunction.Builder argMax() {
        return SqlFunction.Builder.create().setName(ARG_MAX).setMaxArguments(NumericalConstant.TWO);
    }

    /**
     * This method creates a builder for the toYYYYMM ClickHouse function that extracts year and month
     *
     * @return A SqlFunction.Builder configured with the toYYYYMM function name and return type
     */
    private static SqlFunction.Builder toYyyyMm() {
        return SqlFunction.Builder.create().setName(TO_YYYY_MM).setReturnType(OlapDataType.UNSIGNED_INTEGER).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * This method creates a builder for the date conversion function that returns the date value
     *
     * @return A SqlFunction.Builder configured with the toDate function name and DATE return type
     */
    public static SqlFunction.Builder date() {
        return SqlFunction.Builder.create().setName(TO_DATE).setReturnType(OlapDataType.DATE).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * This method creates a builder for the hour of the day function that returns hour values (0-23)
     *
     * @return A SqlFunction.Builder configured with the toHour function name and INTEGER return type
     */
    public static SqlFunction.Builder hourOfTheDay() {
        return SqlFunction.Builder.create().setName(TO_HOUR).setReturnType(CommonDataType.INTEGER).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * This method creates a builder for the day of the month function that returns day values (1-31)
     *
     * @return A SqlFunction.Builder configured with the toDayOfMonth function name and INTEGER return type
     */
    public static SqlFunction.Builder dayOfTheMonth() {
        return SqlFunction.Builder.create().setName(TO_DAY_OF_MONTH).setReturnType(CommonDataType.INTEGER).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * This method creates a builder for the quarter of the year function that returns quarter values (1-4)
     *
     * @return A SqlFunction.Builder configured with the toQuarter function name and INTEGER return type
     */
    public static SqlFunction.Builder quarterOfTheYear() {
        return SqlFunction.Builder.create().setName(TO_QUARTER).setReturnType(CommonDataType.INTEGER).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * This method creates a builder for the by month function that returns formatted month information
     *
     * @return A SqlFunction.Builder configured with the toMonth function name and TEXT return type
     */
    public static SqlFunction.Builder byMonth() {
        return SqlFunction.Builder.create().setName(TO_MONTH).setReturnType(CommonDataType.TEXT).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * This method creates a builder for the day of the week function that returns day name
     *
     * @return A SqlFunction.Builder configured with the toDayOfWeek function name and TEXT return type
     */
    public static SqlFunction.Builder dayOfTheWeek() {
        return SqlFunction.Builder.create().setName(TO_DAY_OF_WEEK).setReturnType(CommonDataType.TEXT).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * This method creates a builder for the month of the year function that returns full month name
     *
     * @return A SqlFunction.Builder configured with the toMonth function name and TEXT return type
     */
    public static SqlFunction.Builder monthOfTheYear() {
        return SqlFunction.Builder.create().setName(TO_MONTH).setReturnType(CommonDataType.TEXT).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * This method creates a builder for the by week function that returns formatted week information
     *
     * @return A SqlFunction.Builder configured with the toStartOfWeek function name and TEXT return type
     */
    public static SqlFunction.Builder byWeek() {
        return SqlFunction.Builder.create().setName(TO_START_OF_WEEK).setReturnType(CommonDataType.TEXT).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * This method creates a builder for the by year function that returns formatted year information
     *
     * @return A SqlFunction.Builder configured with the toYear function name and TEXT return type
     */
    public static SqlFunction.Builder byYear() {
        return SqlFunction.Builder.create().setName(TO_YEAR).setReturnType(CommonDataType.TEXT).setMaxArguments(NumericalConstant.ONE);
    }

    /**
     * Day of the week function: Returns day number (1=Monday..7=Sunday).
     * ClickHouse equivalent: toDayOfWeek()
     * Returns: Integer (1-7, ISO 8601)
     * Note: Contains a placeholder argument at position 0 that gets replaced
     * by the actual column when used with column.function(builder, alias, 0).
     */
    public static SqlFunction.Builder toDayOfFWeek() {
        return SqlFunction.Builder.create()
                .setName(TO_DAY_OF_WEEK)
                .setReturnType(CommonDataType.INTEGER)
                .setMaxArguments(NumericalConstant.ONE)
                .addArgument(SimpleArgument.of(new StringValue("%s")));
    }

    /**
     * Array join function: Flattens an array into individual elements.
     * ClickHouse equivalent: arrayJoin(array)
     * Maps: Expands array elements into separate rows.
     * Returns: Text representation of the array elements.
     */
    public static SqlFunction.Builder arrayJoin() {
        return SqlFunction.Builder.create().setName(ARRAY_JOIN).setReturnType(CommonDataType.TEXT).setMaxArguments(NumericalConstant.ONE);
    }



    /**
     * This method creates a builder for the ClickHouse now function that returns the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with now('UTC') and DATETIME return type
     */
    private static SqlFunction.Builder nowUTC() {
        return SqlFunction.Builder.create().setName(NOW).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArguments(new StringValue(UTC));
    }

    /**
     * This method creates a builder for the ClickHouse now64 function that returns the current UTC timestamp with microsecond precision
     *
     * @return A SqlFunction.Builder configured with now64(6, 'UTC') and DATETIME return type
     */
    private static SqlFunction.Builder now64UTC() {
        return SqlFunction.Builder.create().setName(NOW_64).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArguments(new NumberValue(6), new StringValue(UTC));
    }

    /**
     * This method creates a builder for the subtractDays function that subtracts 1 day from the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with subtractDays(now('UTC'), 1) and DATETIME return type
     */
    private static SqlFunction.Builder subtractDays() {
        return SqlFunction.Builder.create().setName(SUBTRACT_DAYS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(nowUTC().build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }

    /**
     * This method creates a builder for the subtractWeeks function that subtracts 1 week from the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with subtractWeeks(now('UTC'), 1) and DATETIME return type
     */
    private static SqlFunction.Builder subtractWeeks() {
        return SqlFunction.Builder.create().setName(SUBTRACT_WEEKS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(nowUTC().build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }

    /**
     * This method creates a builder for the subtractMonths function that subtracts 1 month from the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with subtractMonths(now('UTC'), 1) and DATETIME return type
     */
    private static SqlFunction.Builder subtractMonths() {
        return SqlFunction.Builder.create().setName(SUBTRACT_MONTHS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(nowUTC().build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }

    /**
     * This method creates a builder for the subtractQuarters function that subtracts 1 quarter from the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with subtractQuarters(now('UTC'), 1) and DATETIME return type
     */
    private static SqlFunction.Builder subtractQuarters() {
        return SqlFunction.Builder.create().setName(SUBTRACT_QUARTERS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(nowUTC().build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }

    /**
     * This method creates a builder for the subtractYears function that subtracts 1 year from the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with subtractYears(now('UTC'), 1) and DATETIME return type
     */
    private static SqlFunction.Builder subtractYears() {
        return SqlFunction.Builder.create().setName(SUBTRACT_YEARS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(nowUTC().build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }

    /**
     * This method creates a builder for the addDays function that adds 1 day to the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with addDays(now('UTC'), 1) and DATETIME return type
     */
    private static SqlFunction.Builder addDays() {
        return SqlFunction.Builder.create().setName(ADD_DAYS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(nowUTC().build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }

    /**
     * This method creates a builder for the addWeeks function that adds 1 week to the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with addWeeks(now('UTC'), 1) and DATETIME return type
     */
    private static SqlFunction.Builder addWeeks() {
        return SqlFunction.Builder.create().setName(ADD_WEEKS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(nowUTC().build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }

    /**
     * This method creates a builder for the addMonths function that adds 1 month to the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with addMonths(now('UTC'), 1) and DATETIME return type
     */
    private static SqlFunction.Builder addMonths() {
        return SqlFunction.Builder.create().setName(ADD_MONTHS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(nowUTC().build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }

    /**
     * This method creates a builder for the addQuarters function that adds 1 quarter to the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with addQuarters(now('UTC'), 1) and DATETIME return type
     */
    private static SqlFunction.Builder addQuarters() {
        return SqlFunction.Builder.create().setName(ADD_QUARTERS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(nowUTC().build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }

    /**
     * This method creates a builder for the addYears function that adds 1 year to the current UTC timestamp
     *
     * @return A SqlFunction.Builder configured with addYears(now('UTC'), 1) and DATETIME return type
     */
    private static SqlFunction.Builder addYears() {
        return SqlFunction.Builder.create().setName(ADD_YEARS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(nowUTC().build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }

    /**
     * This method creates a builder for the since today function that returns the start of the current day in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfDay(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder sinceToday() {
        return SqlFunction.Builder.create().setName(TO_START_OF_DAY).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE)
                .addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the since yesterday function that returns the start of the previous day in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfDay(subtractDays(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder sinceYesterday() {
        return SqlFunction.Builder.create().setName(TO_START_OF_DAY).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(subtractDays().build()));
    }

    /**
     * This method creates a builder for the since this week function that returns the start of the current week in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfWeek(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder sinceThisWeek() {
        return SqlFunction.Builder.create().setName(TO_START_OF_WEEK).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the since last week function that returns the start of the previous week in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfWeek(subtractWeeks(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder sinceLastWeek() {
        return SqlFunction.Builder.create().setName(TO_START_OF_WEEK).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(subtractWeeks().build()));
    }

    /**
     * This method creates a builder for the since this month function that returns the start of the current month in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfMonth(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder sinceThisMonth() {
        return SqlFunction.Builder.create().setName(TO_START_OF_MONTH).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the since last month function that returns the start of the previous month in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfMonth(subtractMonths(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder sinceLastMonth() {
        return SqlFunction.Builder.create().setName(TO_START_OF_MONTH).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(subtractMonths().build()));
    }

    /**
     * This method creates a builder for the since this quarter function that returns the start of the current quarter in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfQuarter(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder sinceThisQuarter() {
        return SqlFunction.Builder.create().setName(TO_START_OF_QUARTER).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the since last quarter function that returns the start of the previous quarter in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfQuarter(subtractQuarters(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder sinceLastQuarter() {
        return SqlFunction.Builder.create().setName(TO_START_OF_QUARTER).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(subtractQuarters().build()));
    }

    /**
     * This method creates a builder for the since this year function that returns the start of the current year in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfYear(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder sinceThisYear() {
        return SqlFunction.Builder.create().setName(TO_START_OF_YEAR).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the since last year function that returns the start of the previous year in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfYear(subtractYears(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder sinceLastYear() {
        return SqlFunction.Builder.create().setName(TO_START_OF_YEAR).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(subtractYears().build()));
    }

    /**
     * This method creates a builder for the after today function that returns the start of the next day in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfDay(addDays(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder afterToday() {
        return SqlFunction.Builder.create().setName(TO_START_OF_DAY).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(addDays().build()));
    }

    /**
     * This method creates a builder for the after yesterday function that returns the start of the current day in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfDay(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder afterYesterday() {
        return SqlFunction.Builder.create().setName(TO_START_OF_DAY).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the after this week function that returns the start of the next week in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfWeek(addWeeks(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder afterThisWeek() {
        return SqlFunction.Builder.create().setName(TO_START_OF_WEEK).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(addWeeks().build()));
    }

    /**
     * This method creates a builder for the after last week function that returns the start of the current week in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfWeek(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder afterLastWeek() {
        return SqlFunction.Builder.create().setName(TO_START_OF_WEEK).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the after this month function that returns the start of the next month in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfMonth(addMonths(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder afterThisMonth() {
        return SqlFunction.Builder.create().setName(TO_START_OF_MONTH).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(addMonths().build()));
    }

    /**
     * This method creates a builder for the after last month function that returns the start of the current month in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfMonth(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder afterLastMonth() {
        return SqlFunction.Builder.create().setName(TO_START_OF_MONTH).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the after this quarter function that returns the start of the next quarter in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfQuarter(addQuarters(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder afterThisQuarter() {
        return SqlFunction.Builder.create().setName(TO_START_OF_QUARTER).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(addQuarters().build()));
    }

    /**
     * This method creates a builder for the after last quarter function that returns the start of the current quarter in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfQuarter(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder afterLastQuarter() {
        return SqlFunction.Builder.create().setName(TO_START_OF_QUARTER).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the after this year function that returns the start of the next year in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfYear(addYears(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder afterThisYear() {
        return SqlFunction.Builder.create().setName(TO_START_OF_YEAR).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(addYears().build()));
    }

    /**
     * This method creates a builder for the after last year function that returns the start of the current year in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfYear(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder afterLastYear() {
        return SqlFunction.Builder.create().setName(TO_START_OF_YEAR).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    // -------------------------------------------------------------------------
    // PREVIOUS RELATIVE — since* (start of previous period, same as sinceLast*)
    // -------------------------------------------------------------------------

    /**
     * Returns the start of yesterday (start of the previous relative day).
     * SQL: toStartOfDay(subtractDays(now('UTC'), 1))
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder sincePreviousRelativeToday() {
        return sinceYesterday();
    }

    /**
     * Returns the start of last week (start of the previous relative week).
     * SQL: toStartOfWeek(subtractWeeks(now('UTC'), 1))
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder sincePreviousRelativeThisWeek() {
        return sinceLastWeek();
    }

    /**
     * Returns the start of last month (start of the previous relative month).
     * SQL: toStartOfMonth(subtractMonths(now('UTC'), 1))
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder sincePreviousRelativeThisMonth() {
        return sinceLastMonth();
    }

    /**
     * Returns the start of last quarter (start of the previous relative quarter).
     * SQL: toStartOfQuarter(subtractQuarters(now('UTC'), 1))
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder sincePreviousRelativeThisQuarter() {
        return sinceLastQuarter();
    }

    /**
     * Returns the start of last year (start of the previous relative year).
     * SQL: toStartOfYear(subtractYears(now('UTC'), 1))
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder sincePreviousRelativeThisYear() {
        return sinceLastYear();
    }

    // -------------------------------------------------------------------------
    // PREVIOUS RELATIVE — after* (same elapsed moment in the previous period)
    // -------------------------------------------------------------------------

    /**
     * Returns the same moment yesterday (current time minus 1 day).
     * SQL: subtractDays(now('UTC'), 1)
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder afterPreviousRelativeToday() {
        return subtractDays();
    }

    /**
     * Returns the same moment last week (current time minus 1 week).
     * SQL: subtractWeeks(now('UTC'), 1)
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder afterPreviousRelativeThisWeek() {
        return subtractWeeks();
    }

    /**
     * Returns the same moment last month (current time minus 1 month).
     * SQL: subtractMonths(now('UTC'), 1)
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder afterPreviousRelativeThisMonth() {
        return subtractMonths();
    }

    /**
     * Returns the same moment last quarter (current time minus 1 quarter).
     * SQL: subtractQuarters(now('UTC'), 1)
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder afterPreviousRelativeThisQuarter() {
        return subtractQuarters();
    }

    /**
     * Returns the same moment last year (current time minus 1 year).
     * SQL: subtractYears(now('UTC'), 1)
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder afterPreviousRelativeThisYear() {
        return subtractYears();
    }

    // -------------------------------------------------------------------------
    // SAME DAY PREVIOUS YEAR — since* (start of the equivalent period one year ago)
    // -------------------------------------------------------------------------

    /**
     * Returns the start of the same day one year ago.
     * SQL: toStartOfDay(subtractYears(now('UTC'), 1))
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder sinceSameDayPreviousYearToday() {
        return SqlFunction.Builder.create().setName(TO_START_OF_DAY).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE)
                .addArgument(FunctionArgument.create(subtractYears().build()));
    }

    /**
     * Returns the start of the same week one year ago.
     * SQL: toStartOfWeek(subtractYears(now('UTC'), 1))
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder sinceSameDayPreviousYearThisWeek() {
        return SqlFunction.Builder.create().setName(TO_START_OF_WEEK).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE)
                .addArgument(FunctionArgument.create(subtractYears().build()));
    }

    /**
     * Returns the start of the same month one year ago.
     * SQL: toStartOfMonth(subtractYears(now('UTC'), 1))
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder sinceSameDayPreviousYearThisMonth() {
        return SqlFunction.Builder.create().setName(TO_START_OF_MONTH).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE)
                .addArgument(FunctionArgument.create(subtractYears().build()));
    }

    /**
     * Returns the start of the same quarter one year ago.
     * SQL: toStartOfQuarter(subtractYears(now('UTC'), 1))
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder sinceSameDayPreviousYearThisQuarter() {
        return SqlFunction.Builder.create().setName(TO_START_OF_QUARTER).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE)
                .addArgument(FunctionArgument.create(subtractYears().build()));
    }

    // -------------------------------------------------------------------------
    // SAME DAY PREVIOUS YEAR — after* (upper bound of the equivalent period one year ago)
    // -------------------------------------------------------------------------

    /**
     * Returns the end of the same day one year ago (i.e. start of the next day one year ago).
     * SQL: toStartOfDay(addDays(subtractYears(now('UTC'), 1), 1))
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder afterSameDayPreviousYearToday() {
        final SqlFunction subtractYearsExpr = subtractYears().build();
        final SqlFunction addDayExpr = SqlFunction.Builder.create()
                .setName(ADD_DAYS).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO)
                .addArgument(FunctionArgument.create(subtractYearsExpr))
                .addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)))
                .build();
        return SqlFunction.Builder.create().setName(TO_START_OF_DAY).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE)
                .addArgument(FunctionArgument.create(addDayExpr));
    }

    /**
     * Returns the same relative moment within the equivalent week one year ago.
     * SQL: subtractYears(now('UTC'), 1)
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder afterSameDayPreviousYearThisWeek() {
        return subtractYears();
    }

    /**
     * Returns the same relative moment within the equivalent month one year ago.
     * SQL: subtractYears(now('UTC'), 1)
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder afterSameDayPreviousYearThisMonth() {
        return subtractYears();
    }

    /**
     * Returns the same relative moment within the equivalent quarter one year ago.
     * SQL: subtractYears(now('UTC'), 1)
     *
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder afterSameDayPreviousYearThisQuarter() {
        return subtractYears();
    }

    /**
     * This method creates a builder for the before today function that returns the start of the current day in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfDay(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder beforeToday() {
        return SqlFunction.Builder.create().setName(TO_START_OF_DAY).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the before yesterday function that returns the start of the previous day in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfDay(subtractDays(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder beforeYesterday() {
        return SqlFunction.Builder.create().setName(TO_START_OF_DAY).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(subtractDays().build()));
    }

    /**
     * This method creates a builder for the before this week function that returns the start of the current week in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfWeek(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder beforeThisWeek() {
        return SqlFunction.Builder.create().setName(TO_START_OF_WEEK).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the before last week function that returns the start of the previous week in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfWeek(subtractWeeks(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder beforeLastWeek() {
        return SqlFunction.Builder.create().setName(TO_START_OF_WEEK).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(subtractWeeks().build()));
    }

    /**
     * This method creates a builder for the before this month function that returns the start of the current month in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfMonth(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder beforeThisMonth() {
        return SqlFunction.Builder.create().setName(TO_START_OF_MONTH).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the before last month function that returns the start of the previous month in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfMonth(subtractMonths(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder beforeLastMonth() {
        return SqlFunction.Builder.create().setName(TO_START_OF_MONTH).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(subtractMonths().build()));
    }

    /**
     * This method creates a builder for the before this quarter function that returns the start of the current quarter in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfQuarter(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder beforeThisQuarter() {
        return SqlFunction.Builder.create().setName(TO_START_OF_QUARTER).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the before last quarter function that returns the start of the previous quarter in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfQuarter(subtractQuarters(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder beforeLastQuarter() {
        return SqlFunction.Builder.create().setName(TO_START_OF_QUARTER).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(subtractQuarters().build()));
    }

    /**
     * This method creates a builder for the before this year function that returns the start of the current year in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfYear(now('UTC')) and DATETIME return type
     */
    public static SqlFunction.Builder beforeThisYear() {
        return SqlFunction.Builder.create().setName(TO_START_OF_YEAR).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(nowUTC().build()));
    }

    /**
     * This method creates a builder for the before last year function that returns the start of the previous year in UTC
     *
     * @return A SqlFunction.Builder configured with toStartOfYear(subtractYears(now('UTC'), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder beforeLastYear() {
        return SqlFunction.Builder.create().setName(TO_START_OF_YEAR).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(subtractYears().build()));
    }

    /**
     * This method creates a builder that wraps a function with toTimeZone to convert the result to UTC
     *
     * @param function The inner SqlFunction.Builder to wrap with timezone conversion
     * @return A SqlFunction.Builder configured with toTimeZone(function, 'UTC') and DATETIME return type
     */
    private static SqlFunction.Builder toTimeZoneUTC(final SqlFunction.Builder function) {
        return SqlFunction.Builder.create().setName(TO_TIME_ZONE).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(function.build())).addArgument(SimpleArgument.of(new StringValue(UTC)));
    }

    /**
     * This method creates a builder that wraps a function with toTimeZone to convert the result to the specified timezone
     *
     * @param function     The inner SqlFunction.Builder to wrap with timezone conversion
     * @param timeZoneName The target timezone name for the conversion
     * @return A SqlFunction.Builder configured with toTimeZone(function, timeZoneName) and DATETIME return type
     */
    private static SqlFunction.Builder toTimeZone(final SqlFunction.Builder function, final String timeZoneName) {
        return SqlFunction.Builder.create().setName(TO_TIME_ZONE).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArgument(FunctionArgument.create(function.build())).addArgument(SimpleArgument.of(new StringValue(timeZoneName)));
    }

    public static SqlFunction.Builder toTimeZone(final Column column, final String timeZoneName) {
        return SqlFunction.Builder.create().setName(TO_TIME_ZONE).setReturnType(OlapDataType.DATETIME).setMaxArguments(NumericalConstant.TWO).addArguments(column).addArgument(SimpleArgument.of(new StringValue(timeZoneName)));
    }

    public static SqlFunction.Builder toDateTime(final Column column) {
        return SqlFunction.Builder.create().setName(TO_DATE_TIME).setReturnType(CommonDataType.TEXT).setMaxArguments(NumericalConstant.ONE).addArguments(column);
    }


    /**
     * This method creates a builder for the in the last seconds upper bound function
     *
     * @return A SqlFunction.Builder configured with current Unix timestamp in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder inTheLastSecondsUpper() {
        return nowUTC();
    }

    /**
     * This method creates a builder for the in the last minutes upper bound function
     *
     * @return A SqlFunction.Builder configured with current Unix timestamp in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder inTheLastMinutesUpper() {
        return nowUTC();
    }

    /**
     * This method creates a builder for the in the last hours upper bound function
     *
     * @return A SqlFunction.Builder configured with current Unix timestamp in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder inTheLastHoursUpper() {
        return nowUTC();
    }

    /**
     * This method creates a builder for the in the last days upper bound function
     *
     * @return A SqlFunction.Builder configured with the start of tomorrow in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder inTheLastDaysUpper() {
        return nowUTC();
    }

    /**
     * Builds the timezone-aware upper-bound expression for weeks/months/years used by IN THE LAST.
     *
     * <p>Pattern: {@code toTimeZone( toDateTime64(toStartOfXxx(toTimeZone(now64(6,'UTC'), tz)), 6, tz) - INTERVAL 1 MICROSECOND, 'UTC')}</p>
     *
     * @param startOfFn the ClickHouse start-of-period function name (e.g. "toStartOfWeek")
     * @param timeZone  the timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    private static SqlFunction.Builder timeIntervalUpper(final String startOfFn, final String timeZone) {
        final SqlFunction innerTimeZoned = toTimeZone(now64UTC(), timeZone).build();
        final SqlFunction startOfPeriod = SqlFunction.Builder.create()
                .setName(startOfFn)
                .setReturnType(OlapDataType.DATETIME)
                .setMaxArguments(NumericalConstant.ONE)
                .addArgument(FunctionArgument.create(innerTimeZoned))
                .build();
        final SqlFunction toDateTime64MinusMicro = SqlFunction.Builder.create()
                .setName(String.format("%s(%s, 6, '%s') - INTERVAL 1 MICROSECOND",
                        TO_DATE_TIME_64, startOfPeriod.toString(column -> column.getColumnName()), timeZone))
                .setReturnType(OlapDataType.DATETIME)
                .setMaxArguments(0)
                .build();
        return SqlFunction.Builder.create()
                .setName(TO_TIME_ZONE)
                .setReturnType(OlapDataType.DATETIME)
                .setMaxArguments(NumericalConstant.TWO)
                .addArgument(FunctionArgument.create(toDateTime64MinusMicro))
                .addArguments(new StringValue(UTC));
    }

    /**
     * This method creates a builder for the in the last weeks upper bound function.
     * SQL: {@code toTimeZone( toDateTime64(toStartOfWeek(toTimeZone(now64(6, 'UTC'), '%s')), 6, '%s') - INTERVAL 1 MICROSECOND, 'UTC')}
     * Matches: BuiltInFunctions.IN_THE_LAST_WEEKS_UPPER
     *
     * @param timeZone The timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder inTheLastWeeksUpper(final String timeZone) {
        return timeIntervalUpper(TO_START_OF_WEEK, timeZone);
    }

    /**
     * This method creates a builder for the in the last months upper bound function.
     * SQL: {@code toTimeZone( toDateTime64(toStartOfMonth(toTimeZone(now64(6, 'UTC'), '%s')), 6, '%s') - INTERVAL 1 MICROSECOND, 'UTC')}
     * Matches: BuiltInFunctions.IN_THE_LAST_MONTHS_UPPER
     *
     * @param timeZone The timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder inTheLastMonthsUpper(final String timeZone) {
        return timeIntervalUpper(TO_START_OF_MONTH, timeZone);
    }

    /**
     * This method creates a builder for the in the last years upper bound function.
     * SQL: {@code toTimeZone( toDateTime64(toStartOfYear(toTimeZone(now64(6, 'UTC'), '%s')), 6, '%s') - INTERVAL 1 MICROSECOND, 'UTC')}
     * Matches: BuiltInFunctions.IN_THE_LAST_YEARS_UPPER
     *
     * @param timeZone The timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder inTheLastYearsUpper(final String timeZone) {
        return timeIntervalUpper(TO_START_OF_YEAR, timeZone);
    }

    /**
     * This method creates a builder for the in the last seconds lower bound function
     *
     * @param value The number of seconds to subtract from the current timestamp, must not be null
     * @return A SqlFunction.Builder configured with the lower bound timestamp in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder inTheLastSecondsLower(final long value) {
        return SqlFunction.Builder.create().setName(String.format("%s - INTERVAL %d %s", nowUTC().build().toString(Column::getColumnName), value, INTERVAL_SECOND)).setReturnType(OlapDataType.DATETIME).setMaxArguments(0);
    }

    /**
     * This method creates a builder for the in the last minutes lower bound function
     *
     * @param value The number of minutes to subtract from the current timestamp, must not be null
     * @return A SqlFunction.Builder configured with the lower bound timestamp in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder inTheLastMinutesLower(final long value) {
        return SqlFunction.Builder.create().setName(String.format("%s - INTERVAL %d %s", nowUTC().build().toString(Column::getColumnName), value, INTERVAL_MINUTE)).setReturnType(OlapDataType.DATETIME).setMaxArguments(0);
    }

    /**
     * This method creates a builder for the in the last hours lower bound function
     *
     * @param value The number of hours to subtract from the current timestamp, must not be null
     * @return A SqlFunction.Builder configured with the lower bound timestamp in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder inTheLastHoursLower(final long value) {
        return SqlFunction.Builder.create().setName(String.format("%s - INTERVAL %d %s", nowUTC().build().toString(Column::getColumnName), value, INTERVAL_HOUR)).setReturnType(OlapDataType.DATETIME).setMaxArguments(0);
    }

    /**
     * This method creates a builder for the in the last days lower bound function
     *
     * @param value The number of days to subtract from the current timestamp, must not be null
     * @return A SqlFunction.Builder configured with the lower bound timestamp in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder inTheLastDaysLower(final long value) {
        return SqlFunction.Builder.create().setName(String.format("%s - INTERVAL %d %s", nowUTC().build().toString(Column::getColumnName), value, INTERVAL_DAY)).setReturnType(OlapDataType.DATETIME).setMaxArguments(0);
    }

    /**
     * Builds the timezone-aware lower-bound expression for weeks/months/years used by IN THE LAST and NOT IN THE LAST.
     *
     * <p>Pattern: {@code toTimeZone( toDateTime64( toStartOfXxx( toTimeZone(now64(6,'UTC'), tz) ) - INTERVAL N UNIT, 6, tz ), 'UTC' )}</p>
     *
     * <p>Composition (inside-out):</p>
     * <ol>
     *   <li>{@code now64(6, 'UTC')} — {@link #now64UTC()}</li>
     *   <li>{@code toTimeZone(now64(...), tz)} — {@link #toTimeZone(SqlFunction.Builder, String)}</li>
     *   <li>{@code toStartOfXxx(...) - INTERVAL N UNIT} — setName with format since SQL arithmetic can't be nested</li>
     *   <li>{@code toDateTime64(..., 6, tz)} — wrapping function</li>
     *   <li>{@code toTimeZone(..., 'UTC')} — {@link #toTimeZoneUTC(SqlFunction.Builder)}</li>
     * </ol>
     *
     * @param startOfFn the ClickHouse start-of-period function name (e.g. "toStartOfWeek")
     * @param value     the number of units to subtract
     * @param unit      the SQL interval unit (e.g. "WEEK", "MONTH", "YEAR")
     * @param timeZone  the timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    private static SqlFunction.Builder tzIntervalLower(final String startOfFn, final long value, final String unit, final String timeZone) {
        final SqlFunction innerTimeZoned = toTimeZone(now64UTC(), timeZone).build();
        final SqlFunction startOfPeriod = SqlFunction.Builder.create()
                .setName(startOfFn)
                .setReturnType(OlapDataType.DATETIME)
                .setMaxArguments(NumericalConstant.ONE)
                .addArgument(FunctionArgument.create(innerTimeZoned))
                .build();
        final SqlFunction toDateTime64 = SqlFunction.Builder.create()
                .setName(String.format("%s(%s - INTERVAL %d %s, 6, '%s')",
                        TO_DATE_TIME_64, startOfPeriod.toString(column -> column.getColumnName()), value, unit, timeZone))
                .setReturnType(OlapDataType.DATETIME)
                .setMaxArguments(0)
                .build();
        return SqlFunction.Builder.create()
                .setName(TO_TIME_ZONE)
                .setReturnType(OlapDataType.DATETIME)
                .setMaxArguments(NumericalConstant.TWO)
                .addArgument(FunctionArgument.create(toDateTime64))
                .addArguments(new StringValue(UTC));
    }

    /**
     * This method creates a builder for the in the last weeks lower bound function.
     * SQL: {@code toTimeZone( toDateTime64(toStartOfWeek(toTimeZone(now64(6, 'UTC'), '%s')) - INTERVAL %d WEEK, 6, '%s'), 'UTC')}
     * Matches: BuiltInFunctions.IN_THE_LAST_WEEKS_LOWER
     *
     * @param value    The number of weeks to subtract from the current timestamp
     * @param timeZone The timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder inTheLastWeeksLower(final long value, final String timeZone) {
        return tzIntervalLower(TO_START_OF_WEEK, value, INTERVAL_WEEK, timeZone);
    }

    /**
     * This method creates a builder for the in the last months lower bound function.
     * SQL: {@code toTimeZone( toDateTime64(toStartOfMonth(toTimeZone(now64(6, 'UTC'), '%s')) - INTERVAL %d MONTH, 6, '%s'), 'UTC')}
     * Matches: BuiltInFunctions.IN_THE_LAST_MONTHS_LOWER
     *
     * @param value    The number of months to subtract from the current timestamp
     * @param timeZone The timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder inTheLastMonthsLower(final long value, final String timeZone) {
        return tzIntervalLower(TO_START_OF_MONTH, value, INTERVAL_MONTH, timeZone);
    }

    /**
     * This method creates a builder for the in the last years lower bound function.
     * SQL: {@code toTimeZone( toDateTime64(toStartOfYear(toTimeZone(now64(6, 'UTC'), '%s')) - INTERVAL %d YEAR, 6, '%s'), 'UTC')}
     * Matches: BuiltInFunctions.IN_THE_LAST_YEARS_LOWER
     *
     * @param value    The number of years to subtract from the current timestamp
     * @param timeZone The timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder inTheLastYearsLower(final long value, final String timeZone) {
        return tzIntervalLower(TO_START_OF_YEAR, value, INTERVAL_YEAR, timeZone);
    }

    /**
     * This method creates a builder for the not in the last seconds function that returns the time threshold
     *
     * @param value The number of seconds to subtract from the current timestamp, must not be null
     * @return A SqlFunction.Builder configured with the time threshold in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder notInTheLastSeconds(final long value) {
        return SqlFunction.Builder.create().setName(String.format("%s - INTERVAL %d %s", nowUTC().build().toString(Column::getColumnName), value, INTERVAL_SECOND)).setReturnType(OlapDataType.DATETIME).setMaxArguments(0);
    }

    /**
     * This method creates a builder for the not in the last minutes function that returns the time threshold
     *
     * @param value The number of minutes to subtract from the current timestamp, must not be null
     * @return A SqlFunction.Builder configured with the time threshold in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder notInTheLastMinutes(final long value) {
        return SqlFunction.Builder.create().setName(String.format("%s - INTERVAL %d %s", nowUTC().build().toString(Column::getColumnName), value, INTERVAL_MINUTE)).setReturnType(OlapDataType.DATETIME).setMaxArguments(0);
    }

    /**
     * This method creates a builder for the not in the last hours function that returns the time threshold
     *
     * @param value The number of hours to subtract from the current timestamp, must not be null
     * @return A SqlFunction.Builder configured with the time threshold in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder notInTheLastHours(final long value) {
        return SqlFunction.Builder.create().setName(String.format("%s - INTERVAL %d %s", nowUTC().build().toString(Column::getColumnName), value, INTERVAL_HOUR)).setReturnType(OlapDataType.DATETIME).setMaxArguments(0);
    }

    /**
     * This method creates a builder for the not in the last days function that returns the time threshold
     *
     * @param value The number of days to subtract from the current timestamp, must not be null
     * @return A SqlFunction.Builder configured with the time threshold in milliseconds and DATETIME return type
     */
    public static SqlFunction.Builder notInTheLastDays(final long value) {
        return SqlFunction.Builder.create().setName(String.format("%s - INTERVAL %d %s", nowUTC().build().toString(Column::getColumnName), value, INTERVAL_DAY)).setReturnType(OlapDataType.DATETIME).setMaxArguments(0);
    }

    /**
     * This method creates a builder for the not in the last weeks function that returns the time threshold.
     * SQL: {@code toTimeZone( toDateTime64(toStartOfWeek(toTimeZone(now64(6, 'UTC'), '%s')) - INTERVAL %d WEEK, 6, '%s'), 'UTC')}
     * Matches: BuiltInFunctions.NOT_IN_THE_LAST_WEEKS
     *
     * @param value    The number of weeks to subtract from the current timestamp
     * @param timeZone The timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder notInTheLastWeeks(final long value, final String timeZone) {
        return tzIntervalLower(TO_START_OF_WEEK, value, INTERVAL_WEEK, timeZone);
    }

    /**
     * This method creates a builder for the not in the last months function that returns the time threshold.
     * SQL: {@code toTimeZone( toDateTime64(toStartOfMonth(toTimeZone(now64(6, 'UTC'), '%s')) - INTERVAL %d MONTH, 6, '%s'), 'UTC')}
     * Matches: BuiltInFunctions.NOT_IN_THE_LAST_MONTHS
     *
     * @param value    The number of months to subtract from the current timestamp
     * @param timeZone The timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder notInTheLastMonths(final long value, final String timeZone) {
        return tzIntervalLower(TO_START_OF_MONTH, value, INTERVAL_MONTH, timeZone);
    }

    /**
     * This method creates a builder for the not in the last years function that returns the time threshold.
     * SQL: {@code toTimeZone( toDateTime64(toStartOfYear(toTimeZone(now64(6, 'UTC'), '%s')) - INTERVAL %d YEAR, 6, '%s'), 'UTC')}
     * Matches: BuiltInFunctions.NOT_IN_THE_LAST_YEARS
     *
     * @param value    The number of years to subtract from the current timestamp
     * @param timeZone The timezone ID
     * @return A SqlFunction.Builder configured with DATETIME return type
     */
    public static SqlFunction.Builder notInTheLastYears(final long value, final String timeZone) {
        return tzIntervalLower(TO_START_OF_YEAR, value, INTERVAL_YEAR, timeZone);
    }

    /**
     * This method creates a builder for the from Unix timestamp 64 milliseconds function
     *
     * @param value The Unix timestamp in milliseconds, must not be null
     * @return A SqlFunction.Builder configured with the fromUnixTimestamp64Milli function and DATETIME return type
     */
    public static SqlFunction.Builder fromUnixTimestamp64Milli(final long value) {
        return SqlFunction.Builder.create().setName(String.format("%s(%d)", FROM_UNIX_TIMESTAMP_64_MILLI, value)).setReturnType(OlapDataType.DATETIME).setMaxArguments(0);
    }

    /**
     * This method creates a builder for the fromUnixTimestamp64Milli function with timezone conversion
     *
     * @param value    The Unix timestamp in milliseconds
     * @param timeZone The target timezone ID for the conversion
     * @return A SqlFunction.Builder configured with fromUnixTimestamp64Milli(value, timezone) and DATETIME return type
     */
    public static SqlFunction.Builder fromUnixTimestamp64MilliWithTimezone(final long value, final String timeZone) {
        return SqlFunction.Builder.create().setName(FROM_UNIX_TIMESTAMP_64_MILLI).setMaxArguments(NumericalConstant.TWO).addArguments(new NumberValue(value), new StringValue(timeZone)).setReturnType(OlapDataType.DATETIME);
    }

    /**
     * This method creates a builder for a custom after filter using fromUnixTimestamp64Milli with addDays
     *
     * @param value The Unix timestamp in milliseconds
     * @return A SqlFunction.Builder configured with toStartOfDay(addDays(fromUnixTimestamp64Milli(value), 1)) and DATETIME return type
     */
    public static SqlFunction.Builder afterCustom(final long value) {
        final SqlFunction fromUnix = fromUnixTimestamp64Milli(value).build();
        final SqlFunction addDay = SqlFunction.Builder.create()
                .setName(ADD_DAYS)
                .setReturnType(OlapDataType.DATETIME)
                .setMaxArguments(NumericalConstant.TWO)
                .addArgument(FunctionArgument.create(fromUnix))
                .addArguments(new NumberValue(NumericalConstant.ONE))
                .build();
        return SqlFunction.Builder.create()
                .setName(TO_START_OF_DAY)
                .setReturnType(OlapDataType.DATETIME)
                .setMaxArguments(NumericalConstant.ONE)
                .addArgument(FunctionArgument.create(addDay));
    }

    /**
     * This method creates a builder that wraps a function with toDate to extract the date portion
     *
     * @param function The inner SqlFunction.Builder to wrap with date extraction
     * @return A SqlFunction.Builder configured with toDate(function) and DATE return type
     */
    private static SqlFunction.Builder toDate(final SqlFunction.Builder function) {
        return SqlFunction.Builder.create().setName(TO_DATE).setReturnType(OlapDataType.DATE).setMaxArguments(NumericalConstant.ONE).addArgument(FunctionArgument.create(function.build()));
    }

    /**
     * This method creates a builder that subtracts a time interval from a function expression
     *
     * @param function     The inner SqlFunction.Builder representing the base expression
     * @param timeInterval The number of interval units to subtract
     * @param intervalName The name of the interval unit (e.g. "DAY", "WEEK", "MONTH", "QUARTER", "YEAR")
     * @return A SqlFunction.Builder configured with function - INTERVAL timeInterval intervalName and DATE return type
     */
    private static SqlFunction.Builder toSubtractInterval(final SqlFunction.Builder function, final int timeInterval, final String intervalName) {
        return SqlFunction.Builder.create().setName(String.format("%s - INTERVAL %d %s", function.build().toString(Column::getColumnName), timeInterval, intervalName)).setReturnType(OlapDataType.DATE).setMaxArguments(0);
    }

    /**
     * This method creates a builder for the end of today function that returns the date of the current day in UTC
     *
     * @return A SqlFunction.Builder configured with toDate(toStartOfDay(now('UTC'))) and DATE return type
     */
    public static SqlFunction.Builder endOfToday() {
        return toDate(sinceToday());
    }

    /**
     * This method creates a builder for the end of yesterday function that returns the date of the previous day in UTC
     *
     * @return A SqlFunction.Builder configured with toDate(toStartOfDay(subtractDays(now('UTC'), 1))) and DATE return type
     */
    public static SqlFunction.Builder endOfYesterday() {
        return toDate(sinceYesterday());
    }

    /**
     * This method creates a builder for the end of this week function that returns the last date of the current week in UTC
     *
     * @return A SqlFunction.Builder configured with toDate(toStartOfWeek(addWeeks(now('UTC'), 1)) - INTERVAL 1 DAY) and DATE return type
     */
    public static SqlFunction.Builder endOfThisWeek() {
        return toDate(toSubtractInterval(afterThisWeek(), 1, INTERVAL_WEEK));
    }

    /**
     * This method creates a builder for the end of last week function that returns the last date of the previous week in UTC
     *
     * @return A SqlFunction.Builder configured with toDate(toStartOfWeek(now('UTC')) - INTERVAL 1 DAY) and DATE return type
     */
    public static SqlFunction.Builder endOfLastWeek() {
        return toDate(toSubtractInterval(sinceThisWeek(), 1, INTERVAL_WEEK));
    }

    /**
     * This method creates a builder for the end of this month function that returns the last date of the current month in UTC
     *
     * @return A SqlFunction.Builder configured with toDate(toStartOfMonth(addMonths(now('UTC'), 1)) - INTERVAL 1 DAY) and DATE return type
     */
    public static SqlFunction.Builder endOfThisMonth() {
        return toDate(toSubtractInterval(afterThisMonth(), 1, INTERVAL_MONTH));
    }

    /**
     * This method creates a builder for the end of last month function that returns the last date of the previous month in UTC
     *
     * @return A SqlFunction.Builder configured with toDate(toStartOfMonth(now('UTC')) - INTERVAL 1 DAY) and DATE return type
     */
    public static SqlFunction.Builder endOfLastMonth() {
        return toDate(toSubtractInterval(sinceThisMonth(), 1, INTERVAL_MONTH));
    }

    /**
     * This method creates a builder for the end of this quarter function that returns the last date of the current quarter in UTC
     *
     * @return A SqlFunction.Builder configured with toDate(toStartOfQuarter(addQuarters(now('UTC'), 1)) - INTERVAL 1 DAY) and DATE return type
     */
    public static SqlFunction.Builder endOfThisQuarter() {
        return toDate(toSubtractInterval(afterThisQuarter(), 1, INTERVAL_QUARTER));
    }

    /**
     * This method creates a builder for the end of last quarter function that returns the last date of the previous quarter in UTC
     *
     * @return A SqlFunction.Builder configured with toDate(toStartOfQuarter(now('UTC')) - INTERVAL 1 DAY) and DATE return type
     */
    public static SqlFunction.Builder endOfLastQuarter() {
        return toDate(toSubtractInterval(sinceThisQuarter(), 1, INTERVAL_QUARTER));
    }

    /**
     * This method creates a builder for the end of this year function that returns the last date of the current year in UTC
     *
     * @return A SqlFunction.Builder configured with toDate(toStartOfYear(addYears(now('UTC'), 1)) - INTERVAL 1 DAY) and DATE return type
     */
    public static SqlFunction.Builder endOfThisYear() {
        return toDate(toSubtractInterval(afterThisYear(), 1, INTERVAL_YEAR));
    }

    /**
     * This method creates a builder for the end of last year function that returns the last date of the previous year in UTC
     *
     * @return A SqlFunction.Builder configured with toDate(toStartOfYear(now('UTC')) - INTERVAL 1 DAY) and DATE return type
     */
    public static SqlFunction.Builder endOfLastYear() {
        return toDate(toSubtractInterval(sinceThisYear(), 1, INTERVAL_YEAR));
    }
    
    /**
     * This method creates a builder for the cityHash64 ClickHouse function that computes a 64-bit hash.
     * cityHash64 is variadic — it accepts an unbounded number of arguments.
     *
     * @return A SqlFunction.Builder configured with the cityHash64 function name and UNSIGNED_BIGINT return type
     */
    private static SqlFunction.Builder cityHash64() {
        return SqlFunction.Builder.create().setName(CITY_HASH_64).setReturnType(OlapDataType.UNSIGNED_BIGINT).setUnboundedArguments();
    }

    /**
     * This method creates a builder for the toStartOfQuarter ClickHouse function that returns the start date of the quarter
     *
     * @return A SqlFunction.Builder configured with the toStartOfQuarter function name and DATE return type
     */
    private static SqlFunction.Builder toStartOfQuarter() {
        return SqlFunction.Builder.create().setName(TO_START_OF_QUARTER).setReturnType(OlapDataType.DATE);
    }

    /**
     * Creates a {@link SqlFunction.Builder} for the ClickHouse {@code subtractSeconds} function
     * that subtracts one second from the given date/time expression.
     *
     * @param functionBuilder the date/time expression
     * @return a {@link SqlFunction.Builder} configured with
     *         {@code subtractSeconds(<expression>, 1)} and a DATE return type
     */
    public static SqlFunction.Builder subtractSeconds(final SqlFunction.Builder functionBuilder) {
        return SqlFunction.Builder.create().setName(SUBTRACT_SECONDS).setReturnType(OlapDataType.DATE).setMaxArguments(NumericalConstant.TWO)
                .addArgument(FunctionArgument.create(functionBuilder.build())).addArgument(SimpleArgument.of(new NumberValue(NumericalConstant.ONE)));
    }
}

