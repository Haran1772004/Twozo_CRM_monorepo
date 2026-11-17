package com.tlc.crm.core.api.analytics.entity.config.widget;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the types of kpi card supported period
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum KPIPeriod
{
    TODAY( 1, "i18n.crm.analytics.kpi.period.today"),
    THIS_WEEK( 2, "i18n.crm.analytics.kpi.period.thisWeek" ),
    THIS_MONTH( 3, "i18n.crm.analytics.kpi.period.thisMonth" ),
    THIS_QUARTER( 4, "i18n.crm.analytics.kpi.period.thisQuarter" ),
    THIS_YEAR( 5, "i18n.crm.analytics.kpi.period.thisYear" ),
    CUSTOM( 6, "i18n.crm.analytics.kpi.period.custom" );

    private final int typeCode;
    private final String i18nKey;

    /**
     * Retrieves the {@link KPIPeriod} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link KPIPeriod} enum associated with the provided code.
     */
    public static KPIPeriod getTimePeriod( final int typeCode )
    {
        switch( typeCode )
        {
            case 1 ->
            {
                return TODAY;
            }
            case 2 ->
            {
                return THIS_WEEK;
            }
            case 3 ->
            {
                return THIS_MONTH;
            }
            case 4 ->
            {
                return THIS_QUARTER;
            }
            case 5 ->
            {
                return THIS_YEAR;
            }
            case 6 ->
            {
                return CUSTOM;
            }
            default ->
            {
                return null;
            }
        }
    }

    /**
     * Retrieves the {@link KPIPeriod} enum corresponding to a given type code
     *
     * @param typeCode The type code to look up
     * @return The {@link KPIPeriod} name associated with the provided code
     */
    public static String getKPIPeriodName( final int typeCode )
    {
        switch( typeCode )
        {
            case 1 ->
            {
                return TODAY.getI18nKey();
            }
            case 2 ->
            {
                return THIS_WEEK.getI18nKey();
            }
            case 3 ->
            {
                return THIS_MONTH.getI18nKey();
            }
            case 4 ->
            {
                return THIS_QUARTER.getI18nKey();
            }
            case 5 ->
            {
                return THIS_YEAR.getI18nKey();
            }
            case 6 ->
            {
                return CUSTOM.getI18nKey();
            }
            default ->
            {
                return null;
            }
        }
    }
}
