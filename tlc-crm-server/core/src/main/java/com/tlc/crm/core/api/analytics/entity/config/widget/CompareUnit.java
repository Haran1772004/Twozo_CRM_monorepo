package com.tlc.crm.core.api.analytics.entity.config.widget;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.commons.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the types of compare unit supported
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum CompareUnit
{
    PREVIOUS_PERIOD( 1, "i18n.crm.analytics.kpi.compare.previousPeriod" ),
    PREVIOUS_PERIOD_RELATIVE( 2, "i18n.crm.analytics.kpi.compare.previousPeriodRelative" ),
    SAME_PERIOD_LAST_YEAR( 3, "i18n.crm.analytics.kpi.compare.samePeriodLastYear" ),
    CUSTOM( 4, "i18n.crm.analytics.kpi.compare.custom" );

    private final int typeCode;
    private final String i18nKey;

    /**
     * Retrieves the {@link CompareUnit} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link CompareUnit} enum associated with the provided code.
     */
    public static CompareUnit getCompareUnit( final int typeCode )
    {
        switch( typeCode )
        {
            case 1 ->
            {
                return PREVIOUS_PERIOD;
            }
            case 2 ->
            {
                return PREVIOUS_PERIOD_RELATIVE;
            }
            case 3 ->
            {
                return SAME_PERIOD_LAST_YEAR;
            }
            case 4 ->
            {
                return CUSTOM;
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_COMPARE_UNIT,
                                    "i18n.analytics.invalid.compare.unit" );
        }
    }

    /**
     * Retrieves the {@link CompareUnit} name corresponding to a given type code
     *
     * @param typeCode The type code to look up
     * @return The {@link CompareUnit} name associated with the provided code
     */
    public static String getCompareUnitName( final int typeCode )
    {
        switch ( typeCode )
        {
            case 1 ->
            {
                return PREVIOUS_PERIOD.i18nKey;
            }
            case 2 ->
            {
                return PREVIOUS_PERIOD_RELATIVE.i18nKey;
            }
            case 3 ->
            {
                return SAME_PERIOD_LAST_YEAR.i18nKey;
            }
            case 4 ->
            {
                return CUSTOM.i18nKey;
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_COMPARE_UNIT,
                                    "i18n.analytics.invalid.compare.unit" );
        }
    }
}
