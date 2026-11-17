package com.tlc.analytics.config.model.view.report.kpi;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the {@link KPIComparatorType} details
 * </p>
 *
 * @author pandiarajkumarG
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum KPIComparatorType
{
    PREVIOUS( 1, "i18n.analytics.widget.comp.type.previous" ),
    PREVIOUS_RELATIVE( 2, "i18n.analytics.widget.comp.type.previous.relative" ),
    SAME_DAY_PREVIOUS_YEAR( 3, "i18n.analytics.widget.comp.type.same.day.previous.year" ),
    CUSTOM( 4, "i18n.analytics.widget.comp.type.custom" );

    private final int typeCode;
    private final String i18key;

    /**
     * <p>
     * Gets the {@link KPIComparatorType} based on the type code
     * </p>
     *
     * @param typeCode Represents the type code
     * @return The {@link KPIComparatorType}
     */
    public static @NonNull KPIComparatorType getComparatorType(final int typeCode) {
        return switch (typeCode) {
            case 1 -> PREVIOUS;
            case 2 -> PREVIOUS_RELATIVE;
            case 3 -> SAME_DAY_PREVIOUS_YEAR;
            case 4 -> CUSTOM;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_WIDGET_COMPARE_TYPE,
                    "i18n.analytics.invalid.widget.compare.type");
        };
    }
}
