package com.tlc.crm.core.api.analytics.entity.config.fields.bucket;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the chart bucket unit types
 * </p>
 *
 * @author James
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum BucketUnit
{
    NONE( 1, "i18n.crm.analytics.config.units.none" ),
    THOUSAND( 2, "i18n.crm.analytics.config.units.thousand" ),
    MILLION( 3, "i18n.crm.analytics.config.units.million" ),
    BILLION( 4, "i18n.crm.analytics.config.units.billion" ),
    TRILLION( 5, "i18n.crm.analytics.config.units.trillion" );

    private final int typeCode;

    private final String i18nKey;

    /**
     * Retrieves the {@link BucketUnit} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link BucketUnit} enum associated with the provided code.
     */
    public static String getUnitName( final int typeCode )
    {
        return switch (typeCode) {
            case 1 -> NONE.getI18nKey();
            case 2 -> THOUSAND.getI18nKey();
            case 3 -> MILLION.getI18nKey();
            case 4 -> BILLION.getI18nKey();
            case 5 -> TRILLION.getI18nKey();
            default -> throw ErrorCode.get( AnalyticsErrorCodes.INVALID_COMPARE_UNIT,
                    "i18n.analytics.invalid.compare.unit" );
        };
    }
}
