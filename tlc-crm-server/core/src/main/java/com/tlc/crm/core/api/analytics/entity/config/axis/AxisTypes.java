package com.tlc.crm.core.api.analytics.entity.config.axis;

import com.tlc.analytics.config.internal.ConfigErrorCodes;
import com.tlc.commons.code.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the chart axis types
 * </p>
 *
 * @author James
 * @version 1.0
 */
@Getter
@RequiredArgsConstructor
public enum AxisTypes
{
    X_ONLY( 1 ),
    Y_ONLY( 2 ),
    X_Y( 3 ),
    NONE( 4 );

    private final int typeCode;

    /**
     * Retrieves the {@link AxisTypes} enum corresponding to a given type code.
     *
     * @param typeCode The type code to look up.
     * @return The {@link AxisTypes} enum associated with the provided code.
     */
    public static AxisTypes getAxisTypes( final int typeCode )
    {
        return switch(typeCode)
        {
            case 1 -> X_ONLY;
            case 2 -> Y_ONLY;
            case 3 -> X_Y;
            case 4 -> NONE;
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_AXIS_TYPE,
                                            "i18n.analytics.invalid.axis.type" );
        };
    }
}
