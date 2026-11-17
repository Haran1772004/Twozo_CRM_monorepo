package com.tlc.crm.core.api.analytics.entity.config.fields.filter;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.commons.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the types of time period supported
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum TimePeriod
{
    SECONDS( 1, "i18n.crm.analytics.filter.time.period.seconds" ),
    MINUTES( 2, "i18n.crm.analytics.filter.time.period.minutes" ),
    HOURS( 3, "i18n.crm.analytics.filter.time.period.hours" ),
    DAYS( 4, "i18n.crm.analytics.filter.time.period.days" ),
    WEEKS( 5, "i18n.crm.analytics.filter.time.period.weeks" ),
    MONTHS( 6, "i18n.crm.analytics.filter.time.period.months" ),
    YEARS( 7, "i18n.crm.analytics.filter.time.period.years" ),
    TODAY( 8, "i18n.crm.analytics.filter.time.period.today"),
    THIS_WEEK( 9, "i18n.crm.analytics.filter.time.period.thisWeek" ),
    THIS_MONTH( 10, "i18n.crm.analytics.filter.time.period.thisMonth" ),
    THIS_QUARTER( 11, "i18n.crm.analytics.filter.time.period.thisQuarter" ),
    THIS_YEAR( 12, "i18n.crm.analytics.filter.time.period.thisYear" ),
    CUSTOM( 13, "i18n.crm.analytics.filter.time.period.custom" );

    private final int typeCode;
    private final String i18nKey;

    /**
     * Retrieves the {@link TimePeriod} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link TimePeriod} enum associated with the provided code.
     */
    public static TimePeriod getTimePeriod( final int typeCode )
    {
        switch( typeCode )
        {
            case 1 ->
            {
                return SECONDS;
            }
            case 2 ->
            {
                return MINUTES;
            }
            case 3 ->
            {
                return HOURS;
            }
            case 4 ->
            {
                return DAYS;
            }
            case 5 ->
            {
                return WEEKS;
            }
            case 6 ->
            {
                return MONTHS;
            }
            case 7 ->
            {
                return YEARS;
            }
            case 8 ->
            {
                return TODAY;
            }
            case 9 ->
            {
                return THIS_WEEK;
            }
            case 10 ->
            {
                return THIS_MONTH;
            }
            case 11 ->
            {
                return THIS_QUARTER;
            }
            case 12 ->
            {
                return THIS_YEAR;
            }
            case 13 ->
            {
                return CUSTOM;
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_TIME_PERIOD,
                                    "i18n.analytics.invalid.report.type" );
        }
    }

    /**
     * Retrieves the {@link TimePeriod} enum corresponding to a given type code
     *
     * @param typeCode The type code to look up
     * @return The {@link TimePeriod} name associated with the provided code
     */
    public static String getTimePeriodName( final int typeCode )
    {
        switch( typeCode )
        {
            case 1 ->
            {
                return SECONDS.getI18nKey();
            }
            case 2 ->
            {
                return MINUTES.getI18nKey();
            }
            case 3 ->
            {
                return HOURS.getI18nKey();
            }
            case 4 ->
            {
                return DAYS.getI18nKey();
            }
            case 5 ->
            {
                return WEEKS.getI18nKey();
            }
            case 6 ->
            {
                return MONTHS.getI18nKey();
            }
            case 7 ->
            {
                return YEARS.getI18nKey();
            }
            case 8 ->
            {
                return TODAY.getI18nKey();
            }
            case 9 ->
            {
                return THIS_WEEK.getI18nKey();
            }
            case 10 ->
            {
                return THIS_MONTH.getI18nKey();
            }
            case 11 ->
            {
                return THIS_QUARTER.getI18nKey();
            }
            case 12 ->
            {
                return THIS_YEAR.getI18nKey();
            }
            case 13 ->
            {
                return CUSTOM.getI18nKey();
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_TIME_PERIOD,
                                    "i18n.analytics.invalid.time.period" );
        }
    }
}