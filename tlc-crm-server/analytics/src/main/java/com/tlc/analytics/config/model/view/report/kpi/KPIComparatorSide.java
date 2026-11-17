package com.tlc.analytics.config.model.view.report.kpi;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Enumeration for specifying comparator side.
 */
@Getter
@RequiredArgsConstructor
public enum KPIComparatorSide
{
    RIGHT(1 ),
    LEFT( 2 );

    private final int typeCode;

    public static @NonNull KPIComparatorSide getComparatorSide( final int typeCode )
    {
        switch (typeCode )
        {
            case 1 ->
            {
                return RIGHT;
            }
            case 2 ->
            {
                return LEFT;
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_WIDGET_COMPARE_SIDE,
                                            "i18n.analytics.invalid.widget.compare.side" );
        }
    }

}
