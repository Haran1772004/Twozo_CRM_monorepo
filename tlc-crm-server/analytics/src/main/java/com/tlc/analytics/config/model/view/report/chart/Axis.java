package com.tlc.analytics.config.model.view.report.chart;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the chart axis types
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@RequiredArgsConstructor()
@Getter
public enum Axis
{
    X_AXIS( 1, "xAxis" ),
    Y_AXIS( 2, "yAxis" ),
    COLOR_AXIS( 3, "colorAxis" ),
    GROUP( 4, "group" ),
    TEXT_AXIS( 5, "textAxis" ),
    SIZE_AXIS( 6, "sizeAxis" );

    private final int type;
    private final String displayName;

    public static Axis getType( final int typeCode )
    {
        switch( typeCode )
        {
            case 1 -> {
                return X_AXIS;
            }
            case 2 -> {
                return Y_AXIS;
            }
            case 3 -> {
                return COLOR_AXIS;
            }
            case 4 -> {
                return GROUP;
            }
            case 5 -> {
                return TEXT_AXIS;
            }
            case 6 -> {
                return SIZE_AXIS;
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_AXIS_TYPE,
                                            "i18n.analytics.invalid.axis.type" );
        }
    }

    /**
     * <p>
     * Gets the {@link Axis} details based on the axis type code
     * </p>
     *
     * @param typeCode Represents the axis type code
     * @return The {@link Axis}
     */
    public static Axis getAxis( final int typeCode )
    {
        return switch (typeCode) {
            case 1 -> X_AXIS;
            case 2 -> Y_AXIS;
            case 3 -> COLOR_AXIS;
            case 4 -> GROUP;
            case 5 -> TEXT_AXIS;
            case 6 -> SIZE_AXIS;
            default -> throw ErrorCode.get(ConfigErrorCodes.INVALID_AXIS_TYPE,
                    "i18n.analytics.invalid.axis.type");
        };
    }
}
