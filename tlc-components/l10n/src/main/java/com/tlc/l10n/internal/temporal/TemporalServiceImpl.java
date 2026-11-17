package com.tlc.l10n.internal.temporal;

import com.tlc.commons.util.StringUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.l10n.temporal.model.ISOTemporalFormat;
import com.tlc.l10n.temporal.model.PeriodOffset;
import com.tlc.l10n.temporal.model.TimezoneInfo;
import com.tlc.l10n.temporal.TemporalPatternType;
import com.tlc.l10n.temporal.interval.DateInterval;
import com.tlc.l10n.temporal.interval.Interval;
import com.tlc.l10n.temporal.service.TemporalService;

import java.text.DateFormat;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Definitions of services provided by the {@link TemporalService} interface.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public class TemporalServiceImpl implements TemporalService {

    private static final Map<TemporalPatternType, DateTimeFormatter> FORMATTERS;
    private static final String TIMEZONE_INDICATOR_REGEX = "(Z|[+-]\\d{2}:\\d{2}|[A-Z]{2,3})$";
    private static final String DOT_REGEX = "\\.";
    private static final int MINUTES_IN_AN_HOUR = 60;
    private static final String FEW_SECONDS_PAST = "a few seconds ago";
    private static final String FEW_SECONDS_FUTURE = "in a few seconds";
    private static final int MAX_VALID_DAY = 31;
    private static final int MAX_VALID_MONTH = 12;
    private static final String SLASH_DATE_FORMAT_REGEX = "(\\d{2}/\\d{2}/\\d{4})|(\\d{2}/\\d{2}/\\d{2})|(\\d{4}/\\d{2}/\\d{2})";

    private static final long SECONDS_IN_A_DAY = TimeUnit.DAYS.toSeconds(NumericalConstant.ONE);
    private static final long SECONDS_IN_AN_HOUR = TimeUnit.HOURS.toSeconds(NumericalConstant.ONE);
    private static final long SECONDS_IN_A_MINUTE = TimeUnit.MINUTES.toSeconds(NumericalConstant.ONE);

    private static final String EMPTY_SPACE = StringUtil.Delimiter.EMPTY_SPACE.getDelimiter();
    private static final char PLURAL_SUFFIX = 's';

    private static final String DAY = "day";
    private static final String HOUR = "hour";
    private static final String MINUTE = "minute";
    private static final String SECOND = "second";

    private final Instant instant;

    static {
        FORMATTERS = new HashMap<>();
        final TemporalPatternType[] temporalPatternTypes = TemporalPatternType.values();

        for (final TemporalPatternType temporalPatternType : temporalPatternTypes) {
            final TemporalPattern[] patterns = TemporalPattern.getPatterns(temporalPatternType);
            final DateTimeFormatterBuilder dateTimeFormatterBuilder = new DateTimeFormatterBuilder().parseCaseInsensitive();

            for (final TemporalPattern pattern : patterns) {
                dateTimeFormatterBuilder.appendPattern(String.format("[%s]", pattern.get()));
            }
            FORMATTERS.put(temporalPatternType, dateTimeFormatterBuilder.toFormatter());
        }
    }

    public TemporalServiceImpl() {
        this.instant = Instant.now();
    }

    public TemporalServiceImpl(final String date, final TimezoneInfo timezoneInfo, final TemporalPatternType temporalPatternType) {
        this.instant = constructInstant(date, timezoneInfo, temporalPatternType);
    }

    public TemporalServiceImpl(final long date) {
        this.instant = Instant.ofEpochMilli(date);
    }

    public TemporalServiceImpl(final TimezoneInfo timezoneInfo) {
        this.instant = ZonedDateTime.now(timezoneInfo.getTimeZone().toZoneId()).toInstant();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getUtcDateTime() {
        return instant.toEpochMilli();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalDate(final TimezoneInfo timezoneInfo) {
        return getLocalDate(timezoneInfo, Locale.US);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalDate(final TimezoneInfo timezoneInfo, final Locale locale) {
        final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.DEFAULT, locale);

        dateFormat.setTimeZone(timezoneInfo.getTimeZone());
        return getFormattedDate(dateFormat);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalTime(final TimezoneInfo timezoneInfo) {
        return getLocalTime(timezoneInfo, Locale.US);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalTime(final TimezoneInfo timezoneInfo, final Locale locale) {
        final DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.DEFAULT, locale);

        dateFormat.setTimeZone(timezoneInfo.getTimeZone());
        return getFormattedDate(dateFormat);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalDateTime(final TimezoneInfo timezoneInfo) {
        return getLocalDateTime(timezoneInfo, Locale.US);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLocalDateTime(final TimezoneInfo timezoneInfo, final Locale locale) {
        final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT, locale);

        dateFormat.setTimeZone(timezoneInfo.getTimeZone());
        return getFormattedDate(dateFormat);
    }

    /**
     * <p>
     * Fetches the milliseconds of UTC to local.
     * </p>
     *
     * @param formatter the {@link DateFormat} object for format the date.
     * @return {@link String} value of formatted local date.
     */
    private String getFormattedDate(final DateFormat formatter) {
        return formatter.format(instant.toEpochMilli());
    }

    /**
     * <p>
     * Constructs the {@link Instant} using input date and timezoneInfo.
     * </p>
     *
     * @param date         Input date from the user.
     * @param timezoneInfo {@link TimezoneInfo} of the user.
     * @param temporalPatternType  Type of the date pattern.
     * @return Constructed {@link Instant}.
     */
    private Instant constructInstant(final String date, final TimezoneInfo timezoneInfo, final TemporalPatternType temporalPatternType) {
        final DateTimeFormatter formatter = FORMATTERS.get(temporalPatternType);
        final ZoneId zoneId = timezoneInfo.getTimeZone().toZoneId();

        return switch (temporalPatternType) {
            case DAY_INCLUSIVE_DATE_TIME, REGULAR_DATE_TIME ->
                    ZonedDateTime.of(LocalDateTime.parse(date, formatter), zoneId).toInstant();
            case DATE -> {
                if (date.matches(SLASH_DATE_FORMAT_REGEX)) {
                    yield parseSlashDate(date).atStartOfDay(zoneId).toInstant();
                } else {
                    yield LocalDate.parse(date, formatter).atStartOfDay(zoneId).toInstant();
                }
            }
            case INTERNATIONAL_TIME, MERIDIAN_TIME ->
                    ZonedDateTime.of(LocalTime.parse(date, formatter).atDate(LocalDate.now()), zoneId).toInstant();
            default -> instant;
        };
    }

    /**
     * <p>
     * Parses a slash-separated date string into a {@link LocalDate} using the appropriate format.
     * </p>
     *
     * @param date Input date from the user.
     * @return Parsed {@link LocalDate}.
     */
    private LocalDate parseSlashDate(final String date) {
        final String[] dateSegments = date.split("/");
        final int firstSegment = Integer.parseInt(dateSegments[0]);
        final int secondSegment = Integer.parseInt(dateSegments[1]);

        final int firstSegmentLength = dateSegments[0].length();
        final int thirdSegmentLength = dateSegments[2].length();

        final boolean isFourDigitYearFirst = 4 == firstSegmentLength;
        final boolean isFourDigitYearLast = 4 == thirdSegmentLength;
        final boolean isTwoDigitDate = 2 == firstSegmentLength;

        final String pattern;

        if (isFourDigitYearFirst) {
            pattern = TemporalPattern.DatePattern.REVERSED_DATE_1.get();
        } else if (MAX_VALID_DAY < firstSegment) {
            pattern = TemporalPattern.DatePattern.YEAR_MONTH_DAY.get();
        } else if (MAX_VALID_MONTH >= firstSegment && MAX_VALID_MONTH < secondSegment) {
            pattern = TemporalPattern.DatePattern.MONTH_DAY_YEAR.get();
        } else {

            if (isFourDigitYearLast) {
                pattern = TemporalPattern.DatePattern.DATE_1.get();
            } else {
                pattern = isTwoDigitDate ? TemporalPattern.DatePattern.DAY_MONTH_YEAR_WITHOUT_LEADING_ZEROS2.get()
                        : TemporalPattern.DatePattern.DAY_MONTH_YEAR_WITHOUT_LEADING_ZEROS1.get();
            }
        }

        return LocalDate.parse(date, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * <p>
     * Converts Coordinated Universal Time to local time
     * </p>
     *
     * @param utcTime      Represents the Co-ordinated Universal Time
     * @param timezoneInfo Represents the {@link TimezoneInfo} of the user.
     * @return the local time
     */
    @Override
    public Long convertUtcToLocalTime(final Long utcTime, final TimezoneInfo timezoneInfo) {
        return new java.util.Date(utcTime + TimeZone.getTimeZone(timezoneInfo.getTimeZone().getID()).getOffset(utcTime)).getTime();
    }

    /**
     * <p>
     * Gets the start time of the current day using timezoneInfo.
     * </p>
     *
     * @param timezoneInfo Represents the {@link TimezoneInfo} of the user.
     * @return start time of the day.
     */
    @Override
    public Long getStartTimeOfDay(final TimezoneInfo timezoneInfo) {
        final ZoneId zoneId = timezoneInfo.getTimeZone().toZoneId();

        return getLocalDate(zoneId).atStartOfDay(zoneId).toInstant().toEpochMilli();
    }

    /**
     * <p>
     * Gets the end time of the current day using timezoneInfo.
     * </p>
     *
     * @param timezoneInfo Represents the {@link TimezoneInfo} of the user.
     * @return end time of the day.
     */
    @Override
    public Long getEndTimeOfDay(final TimezoneInfo timezoneInfo) {
        final ZoneId zoneId = timezoneInfo.getTimeZone().toZoneId();

        return getLocalDate(zoneId).atTime(LocalTime.MAX).atZone(zoneId).toInstant().toEpochMilli();
    }

    /**
     * Converts a timestamp to a relative time string.
     * <p>
     * This method converts the provided timestamp into a human-readable relative time
     * string, such as "a few seconds ago" or "in 3 days". The relative time is based
     * on the current time in the specified timezone.
     * </p>
     *
     * @param timezoneInfo Contains the time zone information to convert the timestamp
     *                     into the desired time zone.
     * @return A string representing the relative time, e.g., "5 minutes ago" or
     *         "in 2 days".
     */
    @Override
    public String convertTimeStampToRelativeTime(final TimezoneInfo timezoneInfo) {
        final ZoneId zoneId = timezoneInfo.getTimeZone().toZoneId();
        final ZonedDateTime targetTime = instant.atZone(zoneId);

        return getRelativeTime(targetTime, ZonedDateTime.now(zoneId));
    }

    /**
     * <p>The calculation uses the following rules based on the time difference:</p>
     * <ul>
     *   <li>Less than a minute: "a few seconds ago" / "in a few seconds"</li>
     *   <li>Less than an hour: "X minutes ago" / "in X minutes"</li>
     *   <li>Less than a day: "X hours ago" / "in X hours"</li>
     *   <li>Less than a week: "X days ago" / "in X days"</li>
     *   <li>Less than a month: "X weeks ago" / "in X weeks"</li>
     *   <li>Less than a year: "X months ago" / "in X months"</li>
     *   <li>Otherwise: "X years ago" / "in X years"</li>
     * </ul>
     *
     * @param targetTime the target time
     * @param now        the current time
     * @return a formatted relative time string
     */
    private String getRelativeTime(final ZonedDateTime targetTime, final ZonedDateTime now) {
        final boolean isFuture = targetTime.isAfter(now);
        final Duration duration = isFuture ? Duration.between(now, targetTime) : Duration.between(targetTime, now);
        final long seconds = duration.toSeconds();
        final long days = duration.toDays();
        final long minutes = duration.toMinutes();
        final long hours = duration.toHours();
        final String relativeTime;

        if (seconds < NumericalConstant.SIXTY) {
            relativeTime = formatRelativeTime(seconds, "second", isFuture);
        } else if (minutes < NumericalConstant.SIXTY) {
            relativeTime = formatRelativeTime(minutes, "minute", isFuture);
        } else if (hours < NumericalConstant.TWENTY_FOUR) {
            relativeTime = formatRelativeTime(hours, "hour", isFuture);
        } else if (days < NumericalConstant.SEVEN) {
            relativeTime = formatRelativeTime(days, "day", isFuture);
        } else if (days < NumericalConstant.THIRTY) {
            final long weeks = days / NumericalConstant.SEVEN;

            relativeTime = formatRelativeTime(weeks, "week", isFuture);
        } else if (days < NumericalConstant.THREE_HUNDRED_SIXTY_FIVE) {
            final long months = days / NumericalConstant.THIRTY;

            relativeTime = formatRelativeTime(months, "month", isFuture);
        } else {

            relativeTime = formatRelativeTime(days / NumericalConstant.THREE_HUNDRED_SIXTY_FIVE, "year", isFuture);
        }

        return relativeTime;
    }

    /**
     * Formats a relative time string with the given unit and value.
     * <p>
     * This method generates a relative time string, such as "1 day ago", "in 2 months",
     * depending on the provided value and unit (minute, hour, day, etc.).
     * </p>
     *
     * @param value     the time amount (e.g., 5 for 5 minutes, 1 for 1 hour)
     * @param unit      the time unit (e.g., "second", "minute", "hour", "day", etc.)
     * @param isFuture  whether the time is in the future (true for future, false for past)
     * @return a formatted relative time string, such as "in 2 days" or "a day ago"
     */
    private String formatRelativeTime(final long value, final String unit, final boolean isFuture) {
        if (Objects.equals(unit, "second")) {
            return isFuture ? FEW_SECONDS_FUTURE : FEW_SECONDS_PAST;
        } else if (value == NumericalConstant.ONE) {
            return isFuture ? String.format("in a %s", unit) : String.format("a %s ago", unit);
        } else {
            final String pluralizedUnit = unit + "s";

            return isFuture ? String.format("in %d %s", value, pluralizedUnit) : String.format("%d %s ago", value, pluralizedUnit);
        }
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param interval     Represents the start date and end date in {@link Interval}.
     * @param dateInterval Represents the {@link DateInterval} that holds various time intervals
     * @return the date interval value
     */
    @Override
    public int getInterval(final Interval<Long, Long> interval, final DateInterval dateInterval) {
        final LocalDateTime startDateTime = convertUtcToLocalDateTime(interval.start());
        final LocalDateTime endDateTime = convertUtcToLocalDateTime(interval.end());

        return Math.abs((int) switch (dateInterval) {
            case YEARS -> ChronoUnit.YEARS.between(startDateTime, endDateTime);
            case QUARTERS -> ChronoUnit.MONTHS.between(startDateTime, endDateTime) / 3;
            case MONTHS -> ChronoUnit.MONTHS.between(startDateTime, endDateTime);
            case WEEKS -> ChronoUnit.WEEKS.between(startDateTime, endDateTime);
            case DAYS -> ChronoUnit.DAYS.between(startDateTime, endDateTime);
            case HOURS -> ChronoUnit.HOURS.between(startDateTime, endDateTime);
            case MINUTES -> ChronoUnit.MINUTES.between(startDateTime, endDateTime);
            case SECONDS -> ChronoUnit.SECONDS.between(startDateTime, endDateTime);
        });
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param date          Represent the date and time in Co-ordinated Universal time format
     * @param intervalValue Represent the interval value to get the previous date
     * @param dateInterval  Represents the {@link DateInterval} that holds various time intervals
     * @return the date in UTC timestamp
     */
    @Override
    public Long getPreviousDate(final Long date, final int intervalValue, final DateInterval dateInterval) {
        final LocalDateTime localDateTime = convertUtcToLocalDateTime(date);

        return convertLocalDateTimeToUtc(switch (dateInterval) {
            case YEARS -> localDateTime.minusYears(intervalValue);
            case QUARTERS -> localDateTime.minusMonths(intervalValue * 3L);
            case MONTHS -> localDateTime.minusMonths(intervalValue);
            case WEEKS -> localDateTime.minusWeeks(intervalValue);
            case DAYS -> localDateTime.minusDays(intervalValue);
            case HOURS -> localDateTime.minusHours(intervalValue);
            case MINUTES -> localDateTime.minusMinutes(intervalValue);
            case SECONDS -> localDateTime.minusSeconds(intervalValue);
        });
    }

    /**
     * {@inheritDoc}
     *
     * @param duration     Represents the duration time
     * @param dateInterval Represents the {@link DateInterval} that holds various time intervals
     * @return
     */
    @Override
    public Long plus(final int duration, final DateInterval dateInterval) {
        final Instant instant = switch (dateInterval) {
            case YEARS -> this.instant.atZone(TimezoneInfo.UTC.getTimeZone().toZoneId()).plusYears(duration).toInstant();
            case MONTHS -> this.instant.atZone(TimezoneInfo.UTC.getTimeZone().toZoneId()).plusMonths(duration).toInstant();
            case WEEKS -> this.instant.atZone(TimezoneInfo.UTC.getTimeZone().toZoneId()).plusWeeks(duration).toInstant();
            case DAYS -> this.instant.plus(duration, ChronoUnit.DAYS);
            case HOURS -> this.instant.plus(duration, ChronoUnit.HOURS);
            case MINUTES -> this.instant.plus(duration, ChronoUnit.MINUTES);
            case SECONDS -> this.instant.plus(duration, ChronoUnit.SECONDS);
            default -> this.instant.plus(duration, ChronoUnit.MILLIS);
        };

        return instant.toEpochMilli();
    }

    /**
     * {@inheritDoc}
     *
     * @param timezoneInfo        Represents the {@link TimezoneInfo}
     * @param temporalPatternType Represents the pattern used to format the date
     * @return A formatted string representation of the given date
     */
    @Override
    public String convertDateToString(final TimezoneInfo timezoneInfo, final TemporalPatternType temporalPatternType) {
        final DateTimeFormatter formatter = FORMATTERS.get(temporalPatternType);

        return formatter.format(instant.atZone(timezoneInfo.getTimeZone().toZoneId()));
    }

    /**
     * <p>
     * Converts the time stamp to Coordinated Universal Time(UTC).
     * </p>
     *
     * @param timeStamp    Represents the time stamp.
     * @param timezoneInfo Represents {@link TimezoneInfo}
     * @return A UTC time stamp.
     */
    public Long convertToUtc(final Long timeStamp, final TimezoneInfo timezoneInfo) {
        final String timezone = timezoneInfo.getTimeZone().getID();
        final LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeStamp), ZoneId.of(timezone));
        final ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.of(timezone));
        final ZonedDateTime utcZonedDateTime = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC"));

        return utcZonedDateTime.toInstant().toEpochMilli();
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p>
     *
     * @param timestamp       Represents the timestamp string to be formatted. It should be in a valid date-time format.
     * @param excludeTimezone Indicates whether the timezone indicator (e.g., 'Z' or offsets) should be excluded from the timestamp or not.
     * @param fractionalCount The number of fractional seconds digits to which the timestamp should be normalized.
     *                        If the fractional part has more digits than specified, it will be truncated; if fewer, it will be padded with zeros.
     * @return the formatted timeStamp
     */
    @Override
    public String formatTimestamp(final String timestamp, final boolean excludeTimezone, final int fractionalCount) {
        final String formattedTimestamp = excludeTimezone ? timestamp.replaceAll(TIMEZONE_INDICATOR_REGEX, "") : timestamp;

        return normalizeFractionalSeconds(formattedTimestamp, fractionalCount);
    }

    /**
     * <p>
     * Converts a LocalDateTime object into a Coordinated Universal Time
     * </p>
     *
     * @param localDateTime Represents the {@link LocalDateTime}
     * @return the UTC timestamp
     */
    private Long convertLocalDateTimeToUtc(final LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /**
     * <p>
     * Converts the Coordinated Universal Time to a LocalDateTime for calculating date intervals in email date filters
     * </p>
     *
     * @param date Represent the Coordinated Universal Time
     * @return the {@link LocalDateTime}
     */
    private LocalDateTime convertUtcToLocalDateTime(final Long date) {
        return Instant.ofEpochMilli(date).atZone(ZoneId.of(TimezoneInfo.UTC.name())).toLocalDateTime();
    }

    /**
     * <p>
     * Gets the local date using zone id.
     * </p>
     *
     * @param zoneId Represents the {@link ZoneId} of the user.
     * @return {@link LocalDate} value.
     */
    private LocalDate getLocalDate(final ZoneId zoneId) {
        return instant.atZone(zoneId).toLocalDate();
    }

    /**
     * <p>
     * Normalizes the fractional seconds in the given timestamp to the specified number of digits.
     * Adjusts the fractional part of the timestamp to ensure it has exactly the number of digits
     * specified by the `fractionalDigits` parameter. If the fractional part has more digits than specified,
     * it will be truncated. If it has fewer digits, it will be padded with trailing zeros. If the timestamp
     * does not contain a fractional part, it will be returned as-is.
     * </p>
     *
     * Supported formats:
     *    - ISO 8601 format: "YYYY-MM-DDTHH:MM:SS.sssZ"
     *    - Timestamp with fractional seconds: "YYYY-MM-DD HH:MM:SS.sss"
     *    - Timestamp without fractional seconds: "YYYY-MM-DD HH:MM:SS"
     *
     * @param timestamp        The timestamp string to be normalized. It should be in a valid date-time format
     *                         with or without a fractional part.
     * @param fractionalDigits The number of fractional seconds digits to which the timestamp should be normalized.
     *                         If the fractional part has more digits than this value, it will be truncated; if fewer,
     *                         it will be padded with zeros.
     * @return The timestamp with the fractional seconds normalized to the specified number of digits.
     */
    private String normalizeFractionalSeconds(final String timestamp, final int fractionalDigits) {
        final String normalizedTimestamp;

        if (timestamp.contains(".")) {
            final String[] parts = timestamp.split(DOT_REGEX);
            final String formatSpecifier = String.format("%%-%ds", fractionalDigits);
            String fractionalPart = parts[1];

            fractionalPart = String.format(formatSpecifier, fractionalPart).replace(' ', '0');
            normalizedTimestamp = String.format("%s.%s", parts[0], fractionalPart);
        } else {
            normalizedTimestamp = timestamp;
        }

        return normalizedTimestamp;
    }

    /**
     * {@inheritDoc}
     *
     * @param timezoneInfo Represents the {@link TimezoneInfo} of the user
     * @return {@link Long} of end time of the month in UTC milliseconds
     */
    @Override
    public Long getEndTimeOfMonth(final TimezoneInfo timezoneInfo) {
        return instant.atZone(timezoneInfo.getTimeZone().toZoneId()).
                with(TemporalAdjusters.lastDayOfMonth()).with(LocalTime.MAX).toInstant().toEpochMilli();
    }

    /**
     * {@inheritDoc}
     *
     * @param endTime      Represents the end time
     * @param timezoneInfo Represents the {@link TimezoneInfo}
     * @return {@link Collection} dates in UTC milliseconds
     */
    @Override
    public Collection<Long> fetchDatesUpToEndTime(final Long endTime, final TimezoneInfo timezoneInfo) {
        final ZoneId zoneId = timezoneInfo.getTimeZone().toZoneId();

        return getLocalDate(zoneId, instant.toEpochMilli()).datesUntil(getLocalDate(zoneId, endTime).plusDays(1)).map(date ->
                date.atStartOfDay(zoneId).toInstant().toEpochMilli()).toList();
    }

    /**
     * {@inheritDoc }
     *
     * @param periodOffset The relative offset of the period (e.g. CURRENT, NEXT, PREVIOUS)
     * @param dateInterval The granularity of the period (MONTHS, QUARTERS, YEARS)
     * @return An {@link Interval} containing the start (inclusive) and end (inclusive) timestamps of the resolved period
     */
    @Override
    public Interval<Long, Long> getPeriodInterval(final PeriodOffset periodOffset, final DateInterval dateInterval) {
        final LocalDateTime localDateTime = convertUtcToLocalDateTime(instant.toEpochMilli());
        final int periodOffsetValue = periodOffset.getValue();
        final LocalDateTime periodStart = switch (dateInterval) {

            case YEARS -> localDateTime.withDayOfYear(NumericalConstant.ONE).with(LocalTime.MIN).plusYears(periodOffsetValue);
            case QUARTERS -> {
                final  int quarter = (localDateTime.getMonthValue() - NumericalConstant.ONE) / NumericalConstant.THREE;
                final  int quarterStartMonth = quarter * NumericalConstant.THREE + NumericalConstant.ONE;

                yield localDateTime.withMonth(quarterStartMonth).withDayOfMonth(NumericalConstant.ONE)
                        .with(LocalTime.MIN).plusMonths(periodOffsetValue* 3L);
            }
            case MONTHS -> localDateTime.withDayOfMonth(NumericalConstant.ONE).with(LocalTime.MIN).plusMonths(periodOffsetValue);
            case WEEKS -> localDateTime.with(DayOfWeek.MONDAY).with(LocalTime.MIN).plusWeeks(periodOffsetValue);
            case DAYS -> localDateTime.with(LocalTime.MIN).plusDays(periodOffsetValue);
            case HOURS -> localDateTime.withMinute(NumericalConstant.ZERO).withSecond(NumericalConstant.ZERO).withNano(NumericalConstant.ZERO).plusHours(periodOffsetValue);
            case MINUTES -> localDateTime.withSecond(NumericalConstant.ZERO).withNano(NumericalConstant.ZERO).plusMinutes(periodOffsetValue);
            case SECONDS -> localDateTime.withNano(NumericalConstant.ZERO).plusSeconds(periodOffsetValue);
        };

        final LocalDateTime periodEnd = switch (dateInterval) {
            case YEARS -> periodStart.plusYears(NumericalConstant.ONE);
            case QUARTERS -> periodStart.plusMonths(NumericalConstant.THREE);
            case MONTHS -> periodStart.plusMonths(NumericalConstant.ONE);
            case WEEKS -> periodStart.plusWeeks(NumericalConstant.ONE);
            case DAYS -> periodStart.plusDays(NumericalConstant.ONE);
            case HOURS -> periodStart.plusHours(NumericalConstant.ONE);
            case MINUTES -> periodStart.plusMinutes(NumericalConstant.ONE);
            case SECONDS -> periodStart.plusSeconds(NumericalConstant.ONE);
        };
        return new Interval<>(convertLocalDateTimeToUtc(periodStart), convertLocalDateTimeToUtc(periodEnd.minusNanos(1)));
    }

    /**
     * {@inheritDoc}
     *
     * @param periodOffset The relative offset of the year (e.g. CURRENT, NEXT)
     * @return A list of four {@link Interval} objects representing Q1 through Q4
     */
    @Override
    public Collection<Interval<Long, Long>> getQuarterIntervals(final PeriodOffset periodOffset) {
        final Interval<Long, Long> yearInterval = getPeriodInterval(periodOffset, DateInterval.YEARS);
        final LocalDateTime yearStart = convertUtcToLocalDateTime(yearInterval.start());
        final Collection<Interval<Long, Long>> quarters = new ArrayList<>(NumericalConstant.FOUR);

        for (int index = NumericalConstant.ZERO; index < NumericalConstant.FOUR; index++) {
            final LocalDateTime start = yearStart.plusMonths(index * 3L);
            final LocalDateTime end = start.plusMonths(3L).minusNanos(1);

            quarters.add(new Interval<>(convertLocalDateTimeToUtc(start), convertLocalDateTimeToUtc(end)));
        }
        return quarters;
    }

    /**
     * {@inheritDoc}
     *
     * @param interval     The start and end time of the period in UTC epoch milliseconds
     * @param dateInterval The granularity of the period (MONTHS, QUARTERS, YEARS)
     * @param timezoneInfo The timezone used to resolve calendar boundaries
     * @param locale       The locale used for localized month names and formatting
     * @return A formatted, human-readable period label
     */
    @Override
    public String formatPeriodInterval(final Interval<Long, Long> interval, final DateInterval dateInterval,
                                       final TimezoneInfo timezoneInfo, final Locale locale) {
        final ZoneId zoneId = timezoneInfo.getTimeZone().toZoneId();
        final LocalDate startDate = Instant.ofEpochMilli(interval.start()).atZone(zoneId).toLocalDate();
        final LocalDate endDate = Instant.ofEpochMilli(interval.end()).atZone(zoneId).toLocalDate();

        return switch (dateInterval) {
            case MONTHS -> startDate.format(DateTimeFormatter.ofPattern("MMM yyyy"));
            case QUARTERS -> {
                final Month startMonth = startDate.getMonth();
                final Month endMonth = endDate.getMonth();
                final StringBuilder quarter = new StringBuilder();

                quarter.append(startMonth.getDisplayName(TextStyle.SHORT, locale));
                quarter.append(" - ");
                quarter.append(endMonth.getDisplayName(TextStyle.SHORT, locale));
                quarter.append(" ");
                quarter.append(startDate.getYear());

                yield quarter.toString();
            }
            case YEARS -> String.valueOf(startDate.getYear());
            default -> String.valueOf(startDate.getDayOfWeek());
        };
    }

    /**
     * <p>
     * Gets the local date using zone id and given time
     * </p>
     *
     * @param zoneId Represents the {@link ZoneId} of the user
     * @param time   Represents the given time
     * @return {@link LocalDate} value
     */
    private LocalDate getLocalDate(final ZoneId zoneId, final Long time) {
        return Instant.ofEpochMilli(time).atZone(zoneId).toLocalDate();
    }

    /**
     * {@inheritDoc}
     *
     * @param timestamp Represents the timestamp
     * @return the minutes of the day
     */
    public int getMinutesOfTheDay(final long timestamp) {
        return getMinutesOfTheDay(timestamp, TimezoneInfo.UTC);
    }

    /**
     * {@inheritDoc}
     *
     * @param timestamp    Represents the timestamp
     * @param timezoneInfo Represents the {@link TimezoneInfo}
     * @return the minutes of the day
     */
    public int getMinutesOfTheDay(final long timestamp, final TimezoneInfo timezoneInfo) {
        final ZoneId zoneId = ZoneId.of(Objects.requireNonNull(timezoneInfo, "Timezone should not be null").getTimeZone().getID());
        final LocalDateTime localDateTime = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDateTime();

        return (MINUTES_IN_AN_HOUR * localDateTime.getHour()) + localDateTime.getMinute();
    }

    /**
     * {@inheritDoc}
     *
     * @param timezoneInfo Represents the {@link TimezoneInfo} object containing the timezone to be used for the conversion
     * @return representation of the zoned date in the specified timezone
     */
    @Override
    public LocalDate getZonedDate(final TimezoneInfo timezoneInfo) {
        return instant.atZone(timezoneInfo.getTimeZone().toZoneId()).toLocalDate();
    }

    /**
     * {@inheritDoc}
     *
     * @param offsetString The ISO offset date-time string to parse.
     * @return The epoch time in milliseconds corresponding to the parsed date-time.
     */
    @Override
    public Long getISOOffsetTimeStamp(final String offsetString) {
        return OffsetDateTime.parse(offsetString).toInstant().toEpochMilli();
    }

    /**
     * {@inheritDoc}
     *
     * @param timestamp    The epoch timestamp in milliseconds to convert.
     * @param timezoneInfo The {@code TimezoneInfo}.
     * @param format       Refers the ISO temporal format.
     * @return The ISO 8601 offset date-time string representation of the timestamp.
     */
    @Override
    public String getISOOffsetDateTimeString(final Long timestamp, final TimezoneInfo timezoneInfo, final ISOTemporalFormat format) {
        final DateTimeFormatter dateTimeFormatter = switch (format) {
            case ISO_OFFSET_TIME -> DateTimeFormatter.ISO_OFFSET_TIME;
            case ISO_OFFSET_DATE -> DateTimeFormatter.ISO_OFFSET_DATE;
            default -> DateTimeFormatter.ISO_OFFSET_DATE_TIME;
        };

        return dateTimeFormatter.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.of(timezoneInfo.getTimeZone().getID())));
    }

    /**
     * {@inheritDoc}
     *
     * @param seconds the total duration in seconds
     * @return A formatted string representing the duration in seconds, minutes, hours, or days
     */
    @Override
    public String formatDuration(final long seconds) {
        if (seconds < 60) {
            return String.format("%d %s", seconds, seconds == 1 ? "Sec" : "Secs");
        }
        final double minutes = seconds / 60.0;

        if (minutes < 60) {
            return String.format("%.2f %s", minutes, minutes == 1.0 ? "Min" : "Mins");
        }
        final double hours = minutes / 60.0;

        if (hours < 24) {
            return String.format("%.2f %s", hours, hours == 1.0 ? "Hr" : "Hrs");
        }
        final double days = hours / 24.0;

        return String.format("%.2f %s", days, days == 1.0 ? "Day" : "Days");
    }

    /**
     * <p>
     * Formats the given time in milliseconds to a human-readable string.
     * Returns null if the time is zero or negative.
     * </p>
     *
     * @param milliSeconds time in milliseconds
     * @return A formatted string representing the time duration
     * (e.g., "1 day 2 hours 30 minutes"), or null if the time is zero or negative
     */
    public String toHumanReadableFormat(final long milliSeconds) {
        return formatSeconds(TimeUnit.MILLISECONDS.toSeconds(milliSeconds));
    }

    /**
     * {@inheritDoc}
     *
     * @param timezoneInfo      The timezone information containing the UTC timestamp to be converted
     * @param dateTimeFormatter The formatter used to format the date/time into a specific pattern
     *                          (e.g., yyyy-MM-dd or yyyy-MM-dd HH:mm:ss)
     */
    @Override
    public String formatUtcToDate(final TimezoneInfo timezoneInfo, final DateTimeFormatter dateTimeFormatter) {
        return instant.atZone(timezoneInfo.getTimeZone().toZoneId()).toLocalDate().format(dateTimeFormatter);
    }

    /**
     * {@inheritDoc}
     *
     * @param timezoneInfo      The timezone information containing the UTC timestamp to be converted
     * @param dateTimeFormatter The formatter used to format the date/time into a specific pattern
     *                          (e.g., yyyy-MM-dd or yyyy-MM-dd HH:mm:ss)
     */
    @Override
    public String formatUtcToDateTime(final TimezoneInfo timezoneInfo, final DateTimeFormatter dateTimeFormatter) {
        return instant.atZone(timezoneInfo.getTimeZone().toZoneId()).toLocalDateTime().format(dateTimeFormatter);
    }

    /**
     * <p>
     * Formats the given total seconds to a human-readable string.
     * Returns null if the time is zero or negative.
     * </p>
     *
     * @param totalSeconds The total time in seconds
     * @return A formatted string representing the time duration (e.g., "1 day 2 hours 30 minutes"),
     * or null if the time is zero or negative
     */
    private String formatSeconds(final long totalSeconds) {
        if (totalSeconds <= NumericalConstant.ZERO) {
            return null;
        }

        final long dayRemainder = totalSeconds % SECONDS_IN_A_DAY;
        final long hourRemainder = dayRemainder % SECONDS_IN_AN_HOUR;
        final StringBuilder formattedTimeSpent = new StringBuilder();

        appendDurationPart(formattedTimeSpent,
                totalSeconds / SECONDS_IN_A_DAY, DAY);
        appendDurationPart(formattedTimeSpent,
                dayRemainder / SECONDS_IN_AN_HOUR, HOUR);
        appendDurationPart(formattedTimeSpent,
                hourRemainder / SECONDS_IN_A_MINUTE, MINUTE);
        appendDurationPart(formattedTimeSpent,
                hourRemainder % SECONDS_IN_A_MINUTE, SECOND);

        return formattedTimeSpent.toString();
    }

    /**
     * <p>
     * Appends a duration part (e.g., days, hours, minutes, seconds) to the formatted time string.
     * </p>
     *
     * @param formattedTime The StringBuilder to which the duration part will be appended
     * @param value         The value of the duration part (e.g., number of days, hours)
     * @param unit          The unit of the duration part (e.g., "day", "hour")
     */
    private void appendDurationPart(final StringBuilder formattedTime,
                                    final long value, final String unit) {
        if (value <= NumericalConstant.ZERO) {
            return;
        }

        if (!formattedTime.isEmpty()) {
            formattedTime.append(EMPTY_SPACE);
        }
        formattedTime.append(value).append(EMPTY_SPACE).append(unit);

        if (value > NumericalConstant.ONE) {
            formattedTime.append(PLURAL_SUFFIX);
        }
    }
}
