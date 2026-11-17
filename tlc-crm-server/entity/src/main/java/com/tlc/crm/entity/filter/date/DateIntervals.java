package com.tlc.crm.entity.filter.date;

import com.tlc.commons.code.ErrorCode;

import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.common.iam.api.model.CrmUser;
import com.tlc.crm.common.iam.api.preferences.model.holder.UserPreferenceHolder;
import com.tlc.crm.common.iam.api.preferences.model.impl.UserRegionalPreference;
import com.tlc.crm.common.iam.api.preferences.model.type.UserPreferenceType;

import com.tlc.crm.entity.internal.status.FilterErrorCodes;

import com.tlc.crm.security.iam.LoggedInUserInfo;

import com.tlc.l10n.temporal.interval.Interval;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAdjusters;

import java.util.Objects;

/**
 * <p>
 * Provides the date intervals.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum DateIntervals {

    LAST_QUARTER,
    THIS_QUARTER,
    NEXT_QUARTER,
    LAST_MONTH,
    THIS_MONTH,
    NEXT_MONTH,
    LAST_WEEK,
    THIS_WEEK,
    NEXT_WEEK,
    SIX_MONTHS_AGO,
    FIVE_MONTHS_AGO,
    FOUR_MONTHS_AGO,
    THREE_MONTHS_AGO,
    TWO_MONTHS_AGO,
    ONE_MONTH_AGO,
    THREE_WEEK_AGO,
    TWO_WEEK_AGO,
    ONE_WEEK_AGO,
    BEFORE_YESTERDAY,
    YESTERDAY,
    YESTERDAY_OR_LATER,
    BEFORE_TODAY,
    TODAY,
    TODAY_OR_LATER,
    BEFORE_TOMORROW,
    TOMORROW,
    TOMORROW_OR_LATER,
    IN_ONE_WEEK,
    IN_TWO_WEEKS,
    IN_THREE_WEEKS,
    IN_ONE_MONTH,
    IN_TWO_MONTHS,
    IN_THREE_MONTHS,
    IN_FOUR_MONTHS,
    IN_FIVE_MONTHS,
    IN_SIX_MONTHS,
    TILL_NOW,
    ROTTEN_TIME,
    BETWEEN,
    THIS_YEAR,
    LAST_YEAR
    ;

    /**
     * <p>
     * Gets the {@link Interval} by using {@link DateIntervals}.
     * </p>
     *
     * @return date interval.
     */
    public Interval<Long, Long> getInterval() {
        final ZonedDateTime zonedDateTime = getCurrentTime();

        return switch (this) {
            case LAST_QUARTER -> getLastQuarterInterval();
            case THIS_QUARTER -> getQuarterInterval(zonedDateTime.getYear(), getPresentQuarter());
            case NEXT_QUARTER -> getNextQuarterInterval();
            case LAST_MONTH -> getLastMonthInterval();
            case THIS_MONTH -> fetchInterval(zonedDateTime.getYear(), zonedDateTime.getMonthValue());
            case NEXT_MONTH -> getNextMonthInterval();
            case LAST_WEEK -> {
                final ZonedDateTime startDateOfLastWeak = zonedDateTime.minusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

                yield getInterval(startDateOfLastWeak, startDateOfLastWeak.plusDays(6));
            }
            case THIS_WEEK -> {
                final ZonedDateTime startDateOfThisWeak = zonedDateTime.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

                yield getInterval(startDateOfThisWeak, startDateOfThisWeak.plusDays(6));
            }
            case NEXT_WEEK -> {
                final ZonedDateTime startDateOfNextWeak = zonedDateTime.plusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

                yield getInterval(startDateOfNextWeak, startDateOfNextWeak.plusDays(6));
            }
            case THIS_YEAR -> {
                final ZonedDateTime startDateOfThisYear = zonedDateTime.with( TemporalAdjusters.firstDayOfYear() );

                yield getInterval( startDateOfThisYear, startDateOfThisYear.plusYears( 1 ) );
            }
            case LAST_YEAR -> {
                final ZonedDateTime startDateOfLastYear = zonedDateTime.minusYears( 1 )
                                                                       .with( TemporalAdjusters.firstDayOfYear() );

                yield getInterval( startDateOfLastYear, startDateOfLastYear.plusYears( 1 ) );
            }
            case SIX_MONTHS_AGO -> getMonthsAgoInterval(zonedDateTime, 6);
            case FIVE_MONTHS_AGO -> getMonthsAgoInterval(zonedDateTime, 5);
            case FOUR_MONTHS_AGO -> getMonthsAgoInterval(zonedDateTime, 4);
            case THREE_MONTHS_AGO -> getMonthsAgoInterval(zonedDateTime, 3);
            case TWO_MONTHS_AGO -> getMonthsAgoInterval(zonedDateTime, 2);
            case ONE_MONTH_AGO -> getMonthsAgoInterval(zonedDateTime, 1);
            case THREE_WEEK_AGO -> getWeeksAgoInterval(zonedDateTime, 3);
            case TWO_WEEK_AGO -> getWeeksAgoInterval(zonedDateTime, 2);
            case ONE_WEEK_AGO -> getWeeksAgoInterval(zonedDateTime, 1);
            case BEFORE_YESTERDAY -> getInterval(null, zonedDateTime.minusDays(2));
            case YESTERDAY -> getInterval(zonedDateTime.minusDays(1));
            case YESTERDAY_OR_LATER -> getInterval(zonedDateTime.minusDays(1), null);
            case BEFORE_TODAY -> getInterval(null, zonedDateTime.minusDays(1));
            case TODAY -> getInterval(zonedDateTime);
            case TODAY_OR_LATER -> getInterval(zonedDateTime, null);
            case BEFORE_TOMORROW -> getInterval(null, zonedDateTime);
            case TOMORROW -> getInterval(zonedDateTime.plusDays(1));
            case TOMORROW_OR_LATER -> getInterval(zonedDateTime.plusDays(1), null);
            case IN_ONE_WEEK -> getWithInWeekInterval(zonedDateTime, 1);
            case IN_TWO_WEEKS -> getWithInWeekInterval(zonedDateTime, 2);
            case IN_THREE_WEEKS -> getWithInWeekInterval(zonedDateTime, 3);
            case IN_ONE_MONTH -> getWithInMonthInterval(zonedDateTime, 1);
            case IN_TWO_MONTHS -> getWithInMonthInterval(zonedDateTime, 2);
            case IN_THREE_MONTHS -> getWithInMonthInterval(zonedDateTime, 3);
            case IN_FOUR_MONTHS -> getWithInMonthInterval(zonedDateTime, 4);
            case IN_FIVE_MONTHS -> getWithInMonthInterval(zonedDateTime, 5);
            case IN_SIX_MONTHS -> getWithInMonthInterval(zonedDateTime, 6);
            case TILL_NOW -> new Interval<>(null, zonedDateTime.toInstant().toEpochMilli());
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the {@link Interval} for the start and end {@link ZonedDateTime}.
     * </p>
     *
     * @param start start date of an interval.
     * @param end   end date of an interval.
     * @return {@link Interval}.
     */
    private Interval<Long, Long> getInterval(final ZonedDateTime start, final ZonedDateTime end) {
        return getInterval(start, end, true);
    }

    /**
     * <p>
     * Gets the {@link Interval} for the start and end {@link ZonedDateTime}.
     * </p>
     *
     * @param start start date of an interval.
     * @param end   end date of an interval.
     * @return {@link Interval}.
     */
    private Interval<Long, Long> getInterval(final ZonedDateTime start, final ZonedDateTime end,
                                             final boolean requiresStartOfDayReset) {
        final Long startDateTime = Objects.isNull(start) ? null : requiresStartOfDayReset ? start.withHour(NumericalConstant.ZERO).
                withMinute(NumericalConstant.ZERO).withSecond(NumericalConstant.ZERO).
                withNano(NumericalConstant.ZERO).toInstant().toEpochMilli() : start.toInstant().toEpochMilli();
        final Long endDateTime = Objects.isNull(end) ? null : end.withHour(NumericalConstant.TWENTY_THREE).
                withMinute(NumericalConstant.FIFTY_NINE).withSecond(NumericalConstant.FIFTY_NINE).
                with(ChronoField.MILLI_OF_SECOND, NumericalConstant.NINE_HUNDRED_NINETY_NINE).toInstant().toEpochMilli();

        return new Interval<>(startDateTime, endDateTime);
    }

    /**
     * <p>
     * Gets the {@link Interval} of next month.
     * </p>
     *
     * @return {@link Interval} of next month.
     */
    private Interval<Long, Long> getNextMonthInterval() {
        final ZonedDateTime zonedDateTime = getCurrentTime();
        final int presentYear = zonedDateTime.getYear();
        final int presentMonth = zonedDateTime.getMonthValue();
        final int nextMonth;
        final int year;

        if (presentMonth < NumericalConstant.TWELVE) {
            nextMonth = presentMonth + NumericalConstant.ONE;
            year = presentYear;
        } else {
            nextMonth = NumericalConstant.ONE;
            year = presentYear + NumericalConstant.ONE;
        }

        return fetchInterval(year, nextMonth);
    }

    /**
     * <p>
     * Gets the {@link Interval} of previous month.
     * </p>
     *
     * @return {@link Interval} of previous month.
     */
    private Interval<Long, Long> getLastMonthInterval() {
        final ZonedDateTime zonedDateTime = getCurrentTime();
        final int presentYear = zonedDateTime.getYear();
        final int presentMonth = zonedDateTime.getMonthValue();
        final int lastMonth;
        final int year;

        if (presentMonth > NumericalConstant.ONE) {
            lastMonth = presentMonth - NumericalConstant.ONE;
            year = presentYear;
        } else {
            lastMonth = NumericalConstant.TWELVE;
            year = presentYear - NumericalConstant.ONE;
        }

        return fetchInterval(year, lastMonth);
    }

    /**
     * <p>
     * Gets the {@link Interval} of next quarter.
     * </p>
     *
     * @return {@link Interval} of next quarter.
     */
    private Interval<Long, Long> getNextQuarterInterval() {
        final ZonedDateTime zonedDateTime = getCurrentTime();
        final int presentYear = zonedDateTime.getYear();
        final int calculatedLastQuarter = getPresentQuarter() + NumericalConstant.ONE;
        final int nextQuarter;
        final int year;

        if (calculatedLastQuarter < NumericalConstant.FIVE) {
            nextQuarter = calculatedLastQuarter;
            year = presentYear;
        } else {
            nextQuarter = NumericalConstant.ONE;
            year = presentYear + NumericalConstant.ONE;
        }

        return getQuarterInterval(year, nextQuarter);
    }

    /**
     * <p>
     * Gets the {@link Interval} of last quarter.
     * </p>
     *
     * @return {@link Interval} of last quarter.
     */
    private Interval<Long, Long> getLastQuarterInterval() {
        final ZonedDateTime zonedDateTime = getCurrentTime();
        final int presentYear = zonedDateTime.getYear();
        final int calculatedLastQuarter = getPresentQuarter() - NumericalConstant.ONE;
        final int lastQuarter;
        final int year;

        if (calculatedLastQuarter > NumericalConstant.ZERO) {
            lastQuarter = calculatedLastQuarter;
            year = presentYear;
        } else {
            lastQuarter = NumericalConstant.FOUR;
            year = presentYear - NumericalConstant.ONE;
        }

        return getQuarterInterval(year, lastQuarter);
    }

    /**
     * <p>
     * Gets the {@link Interval} of quarter.
     * </p>
     *
     * @return {@link Interval} of quarter.
     */
    private Interval<Long, Long> getQuarterInterval(final int year, final int quarter) {
        return switch (quarter) {
            case 1 -> fetchInterval(year, Month.JANUARY.getValue(), Month.MARCH.getValue());
            case 2 -> fetchInterval(year, Month.APRIL.getValue(), Month.JUNE.getValue());
            case 3 -> fetchInterval(year, Month.JULY.getValue(), Month.SEPTEMBER.getValue());
            case 4 -> fetchInterval(year, Month.OCTOBER.getValue(), Month.DECEMBER.getValue());

            default -> throw ErrorCode.get(FilterErrorCodes.INVALID_QUARTER);
        };
    }

    /**
     * <p>
     * Fetches the interval values of the given data.
     * </p>
     *
     * @param year  value of year.
     * @param month value of month.
     * @return {@link Interval} value.
     */
    private Interval<Long, Long> fetchInterval(final int year, final int month) {
        return fetchInterval(year, month, month);
    }

    /**
     * <p>
     * Fetches the interval values of the given data.
     * </p>
     *
     * @param year       value of year.
     * @param startMonth value of starting month.
     * @param endMonth   value of ending month.
     * @return {@link Interval} value.
     */
    private Interval<Long, Long> fetchInterval(final int year, final int startMonth, final int endMonth) {
        final ZonedDateTime zonedDateTime = getCurrentTime();
        final ZonedDateTime startTime = zonedDateTime.withYear(year).withMonth(startMonth).withDayOfMonth(NumericalConstant.ONE)
                .withHour(NumericalConstant.ZERO).withMinute(NumericalConstant.ZERO).withSecond(NumericalConstant.ZERO)
                .with(ChronoField.MILLI_OF_SECOND, NumericalConstant.ZERO);
        final long startTimeInMillis = startTime.toInstant().toEpochMilli();

        final ZonedDateTime endTime = zonedDateTime.withYear(year).withMonth(endMonth).withHour(NumericalConstant.TWENTY_THREE).
                withMinute(NumericalConstant.FIFTY_NINE).withSecond(NumericalConstant.FIFTY_NINE).
                with(ChronoField.MILLI_OF_SECOND, NumericalConstant.NINE_HUNDRED_NINETY_NINE).with(TemporalAdjusters.lastDayOfMonth());
        final long endTimeInMillis = endTime.toInstant().toEpochMilli();

        return new Interval<>(startTimeInMillis, endTimeInMillis);
    }

    /**
     * <p>
     * Gets the value of the present quarter.
     * </p>
     *
     * @return present quarter of the year.
     */
    private int getPresentQuarter() {
        final ZonedDateTime now = getCurrentTime();

        return (now.getMonthValue() - 1) / 3 + 1;
    }

    /**
     * <p>
     * Fetches the interval of within the months.
     * </p>
     *
     * @param zonedDateTime {@link ZonedDateTime}.
     * @param noOfMonths    Count of months.
     * @return {@link Interval} of within the months.
     */
    private Interval<Long, Long> getWithInMonthInterval(final ZonedDateTime zonedDateTime, final int noOfMonths) {
        return getInterval(zonedDateTime, zonedDateTime.plusMonths(noOfMonths), false);
    }

    /**
     * <p>
     * Fetches the interval of within the weeks.
     * </p>
     *
     * @param zonedDateTime {@link ZonedDateTime}.
     * @param noOfWeeks     Count of weeks.
     * @return {@link Interval} of within the weeks.
     */
    private Interval<Long, Long> getWithInWeekInterval(final ZonedDateTime zonedDateTime, final int noOfWeeks) {
        return getInterval(zonedDateTime, zonedDateTime.plusWeeks(noOfWeeks), false);
    }

    /**
     * <p>
     * Fetches the interval of weeks ago.
     * </p>
     *
     * @param zonedDateTime {@link ZonedDateTime}.
     * @param noOfWeeks     Count of weeks.
     * @return {@link Interval} of weeks ago.
     */
    private Interval<Long, Long> getWeeksAgoInterval(final ZonedDateTime zonedDateTime, final int noOfWeeks) {
        return getInterval(null, zonedDateTime.minusWeeks(noOfWeeks));
    }

    /**
     * <p>
     * Fetches the interval of months ago.
     * </p>
     *
     * @param zonedDateTime {@link ZonedDateTime}.
     * @param noOfMonths    Count of months.
     * @return {@link Interval} of months ago.
     */
    private Interval<Long, Long> getMonthsAgoInterval(final ZonedDateTime zonedDateTime, final int noOfMonths) {
        return getInterval(null, zonedDateTime.minusMonths(noOfMonths));
    }

    /**
     * <p>
     * Constructs the pair of value.
     * </p>
     *
     * @param day represents the value of day.
     * @return {@link Interval}.
     */
    private Interval<Long, Long> getInterval(final ZonedDateTime day) {
        return getInterval(day, day);
    }

    /**
     * <p>
     * Fetches the current time as {@link ZonedDateTime}.
     * </p>
     *
     * @return {@link ZonedDateTime}.
     */
    private ZonedDateTime getCurrentTime() {
        final CrmUser crmUser = LoggedInUserInfo.getUser();
        final UserPreferenceHolder preferenceHolder = crmUser.getPreferenceHolder();

        if (Objects.isNull(preferenceHolder)) {
            return ZonedDateTime.now();
        }
        final UserRegionalPreference userPreference = preferenceHolder.get(UserPreferenceType.USER_REGIONAL_PREFERENCE);

        return ZonedDateTime.now(userPreference.getTimezoneInfo().getTimeZone().toZoneId());
    }
}
