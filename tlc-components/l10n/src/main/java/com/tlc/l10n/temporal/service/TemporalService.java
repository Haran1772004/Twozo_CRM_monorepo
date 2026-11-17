package com.tlc.l10n.temporal.service;

import com.tlc.l10n.temporal.model.ISOTemporalFormat;
import com.tlc.l10n.temporal.TemporalPatternType;
import com.tlc.l10n.temporal.model.PeriodOffset;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.l10n.temporal.interval.Interval;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import java.util.Collection;
import java.util.Locale;
import com.tlc.l10n.internal.temporal.TemporalPattern.DatePattern;

/**
 * <p>
 * Declaration of services provided by the {@link TemporalService}.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public interface TemporalService {

    /**
     * <p>
     * Provides the UTC date and time in milliseconds.
     * </p>
     *
     * @return {@link Long} value of UTC DateTime.
     */
    Long getUtcDateTime();

    /**
     * <p>
     * Fetches the local date and time using timezoneInfo.
     * </p>
     *
     * @param timezoneInfo {@link TimezoneInfo} refers to the regional or geographical area.
     * @return {@link String} value of converted local date. (ex: 20-02-2000)
     */
    String getLocalDate(final TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Fetches the local date and time using timezoneInfo.
     * </p>
     *
     * @param timezoneInfo refers to the regional or geographical area.
     * @return {@link String} value of converted local time. (ex: 10:55 AM)
     */
    String getLocalTime(final TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Fetches the local date and time using timezoneInfo.
     * </p>
     *
     * @param timezoneInfo refers to the regional or geographical area.
     * @return {@link String} value of converted local date and time. (ex: 20-02-2000 10:55 AM)
     */
    String getLocalDateTime(final TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Fetches the local date using timezoneInfo, language, country.
     * </p>
     *
     * @param timezoneInfo refers to the regional or geographical area.
     * @param locale       {@link Locale} refers to a set of parameters that define specific regional settings for a software application.
     * @return {@link String} value of converted local date. (ex: 20-02-2000)
     */
    String getLocalDate(final TimezoneInfo timezoneInfo, final Locale locale);

    /**
     * <p>
     * Fetches the local time using timezoneInfo, language, country.
     * </p>
     *
     * @param timezoneInfo refers to the regional or geographical area.
     * @param locale       {@link Locale} refers to a set of parameters that define specific regional settings for a software application.
     * @return {@link String} value of converted local time. (ex: 10:55 AM)
     */
    String getLocalTime(final TimezoneInfo timezoneInfo, final Locale locale);

    /**
     * <p>
     * Fetches the local date and time using timezoneInfo, language, country.
     * </p>
     *
     * @param timezoneInfo refers to the regional or geographical area.
     * @param locale       {@link Locale} refers to a set of parameters that define specific regional settings for a software application.
     * @return {@link String} value of converted local date and time. (ex: 20-02-2000 10:55 AM)
     */
    String getLocalDateTime(final TimezoneInfo timezoneInfo, final Locale locale);

    /**
     * <p>
     * Converts Coordinated Universal Time to local time
     * </p>
     *
     * @param utcTime      Represents the Co-ordinated Universal Time
     * @param timezoneInfo Represents the {@link TimezoneInfo} of the user.
     * @return the local time
     */
    Long convertUtcToLocalTime(final Long utcTime, TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Gets the start time of the current day using timezoneInfo.
     * </p>
     *
     * @param timezoneInfo Represents the {@link TimezoneInfo} of the user.
     * @return start time of the day.
     */
    Long getStartTimeOfDay(final TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Gets the end time of the current day using timezoneInfo.
     * </p>
     *
     * @param timezoneInfo Represents the {@link TimezoneInfo} of the user.
     * @return end time of the day.
     */
    Long getEndTimeOfDay(final TimezoneInfo timezoneInfo);

    /**
     * Converts the timestamp to a human-readable relative time string.
     * <p>
     * This method takes the provided timezone information and converts the
     * associated timestamp to a relative time format, such as
     * "a few seconds ago", "3 hours ago", or "in 2 days", depending on whether
     * the time is in the past or future.
     * </p>
     *
     * @param timezoneInfo the timezone information used to interpret the timestamp
     * @return a formatted relative time string
     */
    String convertTimeStampToRelativeTime(final TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Fetches the date intervals between two intervals
     * </p>
     *
     * @param interval     Represents the start date and end date in {@link Interval}.
     * @param dateInterval Represents the {@link DateInterval} that holds various time intervals
     * @return the date interval value
     */
    int getInterval(final Interval<Long, Long> interval, final DateInterval dateInterval);

    /**
     * <p>
     * Gets the previous date based on the specified interval value and date interval
     * </p>
     *
     * @param date          Represents the date and time in Co-ordinated Universal time format
     * @param intervalValue Represents the interval value to get the previous date
     * @param dateInterval  Represents the {@link DateInterval} that holds various time intervals
     * @return the date in Co-ordinated Universal Time
     */
    Long getPreviousDate(final Long date, final int intervalValue, final DateInterval dateInterval);

    /**
     * <p>
     * Gets the specified duration to the current instant based on the provided date interval
     * </p>
     *
     * @param duration     Represents the duration time
     * @param dateInterval Represents the {@link DateInterval} that holds various time intervals
     * @return The time after adding the duration time
     */
    Long plus(final int duration, final DateInterval dateInterval);

    /**
     * <p>
     * Converts UTC (Coordinated Universal Time) to a formatted string using the specified pattern and timezone.
     * </p>
     *
     * @param timezoneInfo        Represents the {@link TimezoneInfo}
     * @param temporalPatternType Represents the pattern used to format the date
     * @return A formatted string representation of the given date
     */
    String convertDateToString(final TimezoneInfo timezoneInfo, final TemporalPatternType temporalPatternType);

    /**
     * <p>
     * Converts the time stamp to Coordinated Universal Time(UTC).
     * </p>
     *
     * @param timeStamp    Represents the time stamp.
     * @param timezoneInfo Represents {@link TimezoneInfo}
     * @return A UTC time stamp.
     */
    Long convertToUtc(final Long timeStamp, final TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Formats the given timestamp according to the specified {@link DateTimeFormatter}.
     * Optionally removes the timezone indicator and normalizes fractional seconds to the given count of digits.
     * </p>
     *
     * For Example:
     *     Input: "2024-09-17T15:23:45.6789Z", excludeTimezone: false, fractionalCount: 3 -> Output: "2024-09-17T15:23:45.678"
     *     Input: "2024-09-17T15:23:45.67+02:00", excludeTimezone: true, fractionalCount: 2 -> Output: "2024-09-17T15:23:45.67"
     *     Input: "2024-09-17T15:23:45Z", excludeTimezone: false, fractionalCount: 3 -> Output: "2024-09-17T15:23:45.000"
     *     Input: "2024-09-17T15:23:45.987+05:30", excludeTimezone: true, fractionalCount: 1 -> Output: "2024-09-17T15:23:45.9"
     *     Input: "2024-09-17T15:23:45", excludeTimezone: false, fractionalCount: 2 -> Output: "2024-09-17T15:23:45.00"
     *
     * @param timestamp         Represents the timestamp string to be formatted. It should be in a valid date-time format.
     * @param excludeTimezone   Indicates whether the timezone indicator (e.g., 'Z' or offsets) should be excluded from the timestamp or not.
     * @param fractionalCount   The number of fractional seconds digits to which the timestamp should be normalized. If the fractional part has more digits than specified, it will be truncated; if fewer, it will be padded with zeros.
     * @return The formatted timestamp string. It will have fractional seconds normalized to the specified count and optionally have the timezone indicator removed.
     */
    String formatTimestamp(final String timestamp, final boolean excludeTimezone, final int fractionalCount);

    /**
     * <p>
     * Gets the end time of the current month based on the provided {@link TimezoneInfo}.
     * The result is returned as a {@link Long} representing the UTC milliseconds of the last day of the month
     * at 23:59:59.999 (just before the start of the next day).
     * </p>
     *
     * <b> For example, if the current day is 15th January 2024, this method will return the timestamp
     * for 31st January 2024, 23:59:59.999 in UTC </b>
     *
     * @param timezoneInfo Represents the {@link TimezoneInfo} of the user
     * @return {@link Long} representing the end time of the month in UTC milliseconds
     */
    Long getEndTimeOfMonth(final TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Retrieves all dates in UTC milliseconds from the current date up to the specified end time.
     * Generates a list of dates (at the start of each day) between the start date and the provided end time,
     * both in the user's time zone, and converts them into UTC milliseconds.
     * </p>
     *
     * <b>For example, if the current date is 10th January 2024 and the endTime represents 15th January 2024,
     * it will return UTC timestamps corresponding to the start of each day (00:00:00) from 10th to 15th January 2024</b>
     *
     * @param endTime      Represents the end time in UTC milliseconds
     * @param timezoneInfo Represents the {@link TimezoneInfo} containing the user's time zone information
     * @return a {@link Collection} of dates in UTC milliseconds that fall between the given start and end time
     */
    Collection<Long> fetchDatesUpToEndTime(final Long endTime, final TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Gets the total minutes of the day in UTC time zone from the provided timestamp
     * </p>
     *
     * @param timestamp Represents the timestamp
     * @return the minutes of the day
     */
    int getMinutesOfTheDay(final long timestamp);

    /**
     * <p>
     * Gets the total minutes of the day from the provided timestamp and {@link TimezoneInfo}
     * </p>
     *
     * @param timestamp    Represents the timestamp
     * @param timezoneInfo Represents the {@link TimezoneInfo}
     * @return the minutes of the day
     */
    int getMinutesOfTheDay(final long timestamp, final TimezoneInfo timezoneInfo);

    /**
     * <p>
     * Retrieves the formatted local date string of current instant based on the specified timezone and date pattern.
     * Takes a given {@link TimezoneInfo} and {@link DatePattern} to produce a formatted date string.
     * Converts the instant to a zoned date in the provided timezone extracts the local date,
     * and then formats it using the specified date pattern.
     * </p>
     *
     * <p><b>Note:</b> The instant may either represent the current instant or a user-preferred instant.
     * If the user does not provide an instant the current instant will be used.</p>
     *
     * @param timezoneInfo Represents the {@link TimezoneInfo} object containing the timezone to be used for the conversion
     * @return a {@link LocalDate} representation of the zoned date in the specified timezone.
     */
    LocalDate getZonedDate(final TimezoneInfo timezoneInfo);

    /**
     * Parses the given ISO 8601 offset date-time string and returns the time in milliseconds since the epoch.
     *
     * <p>The input should be in a standard ISO 8601 format with a timezone offset,
     * such as {@code "2025-05-15T10:15:30+05:30"}.</p>
     *
     * @param offsetString The ISO offset date-time string to parse.
     * @return The epoch time in milliseconds corresponding to the parsed date-time.
     */
    Long getISOOffsetTimeStamp(final String offsetString);

    /**
     * Converts the given timestamp into an ISO 8601 offset date-time string based on the specified store type.
     *
     * <p>The resulting string will include the date, time, and timezone offset, such as
     * {@code "2025-05-15T10:15:30+05:30"}.</p>
     *
     * @param timestamp    The epoch timestamp in milliseconds to convert.
     * @param timezoneInfo The {@code TimezoneInfo}.
     * @param format       Refers the ISO temporal format.
     * @return The ISO 8601 offset date-time string representation of the timestamp.
     */
    String getISOOffsetDateTimeString(final Long timestamp, final TimezoneInfo timezoneInfo, final ISOTemporalFormat format);

    /**
     * <p>
     * Converts a duration in seconds into a human-readable string with an appropriate unit.
     * </p>
     *
     * <ul>
     *   <li>If the duration is less than 60 seconds, it is displayed in seconds (e.g., "45 Secs").</li>
     *   <li>If the duration is less than 60 minutes, it is displayed in minutes with 2 decimal places (e.g., "3.12 Mins").</li>
     *   <li>If the duration is less than 24 hours, it is displayed in hours with 2 decimal places (e.g., "1.50 Hrs").</li>
     *   <li>If the duration is 24 hours or more, it is displayed in days with 2 decimal places (e.g., "2.00 Days").</li>
     * </ul>
     *
     * @param seconds the total duration in seconds
     * @return A formatted string representing the duration in seconds, minutes, hours, or days
     */
    String formatDuration(final long seconds);

    /**
     * <p>
     * Formats the given total time in milliseconds to a human-readable string.
     * Returns null if the total time is zero or negative.
     * </p>
     *
     * @param milliSeconds The total time in milliseconds
     * @return A formatted string representing the time duration
     * (e.g., "1 day 2 hours 30 minutes"), or null if the time is zero or negative
     */
    String toHumanReadableFormat(final long milliSeconds);

    /**
     * Computes the start and end time (UTC epoch milliseconds) for a given
     * period type and offset relative to the current time
     * <p>
     *
     * @param periodOffset The relative offset of the period (e.g. CURRENT, NEXT, PREVIOUS)
     * @param dateInterval The granularity of the period (MONTHS, QUARTERS, YEARS)
     *
     * @return An {@link Interval} containing the start (inclusive) and end (inclusive) timestamps of the resolved period
     */
    Interval<Long, Long> getPeriodInterval(final PeriodOffset periodOffset, final DateInterval dateInterval);

    /**
     * <p>
     * Formats a given period interval into a human-readable label.
     * </p>
     * <p>
     * <ul>
     *   <li>MONTHS   → {@code "Aug 2025"}</li>
     *   <li>QUARTERS → {@code "Jul - Sep 2025"}</li>
     *   <li>YEARS    → {@code "2025"}</li>
     * </ul>
     * <p>
     *
     * @param interval The start and end time of the period in UTC epoch milliseconds
     * @param dateInterval The granularity of the period (MONTHS, QUARTERS, YEARS)
     * @param timezoneInfo The timezone used to resolve calendar boundaries
     * @param locale The locale used for localized month names and formatting
     *
     * @return A formatted, human-readable period label
     */
    String formatPeriodInterval(final Interval<Long, Long> interval, final DateInterval dateInterval,
                                final TimezoneInfo timezoneInfo, final Locale locale);

    /**
     * <p>
     * Gets all quarter intervals for a specific year period.
     * </p>
     *
     * @param periodOffset The relative offset of the year (e.g. CURRENT, NEXT)
     * @return A list of four {@link Interval} objects representing Q1 through Q4
     */
    Collection<Interval<Long, Long>> getQuarterIntervals(final PeriodOffset periodOffset);

    /**
     * Converts the given UTC timestamp from {@link TimezoneInfo} into a formatted date string
     * based on the provided {@link DateTimeFormatter}.
     *
     * @param timezoneInfo      The timezone information containing the UTC timestamp to be converted
     * @param dateTimeFormatter The formatter used to format the date/time into a specific pattern
     *                          (e.g., yyyy-MM-dd or yyyy-MM-dd HH:mm:ss)
     * @return The formatted date/time string based on the specified formatter
     */
    String formatUtcToDate(final TimezoneInfo timezoneInfo, final DateTimeFormatter dateTimeFormatter);

    /**
     * Converts the given UTC timestamp from {@link TimezoneInfo} into a formatted datetime string
     * based on the provided {@link DateTimeFormatter}.
     *
     * @param timezoneInfo      The timezone information containing the UTC timestamp to be converted
     * @param dateTimeFormatter The formatter used to format the date/time into a specific pattern
     *                          (e.g., yyyy-MM-dd or yyyy-MM-dd HH:mm:ss)
     * @return The formatted date/time string based on the specified formatter
     */
    String formatUtcToDateTime(final TimezoneInfo timezoneInfo, final DateTimeFormatter dateTimeFormatter);
}
