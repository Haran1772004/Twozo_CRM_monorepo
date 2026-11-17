package com.tlc.analytics.config.model.view;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * Represents the chart position types
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
@RequiredArgsConstructor
@Getter
public enum Position
{
    LEFT( 1 ),
    RIGHT( 2 ),
    TOP( 3 ),
    BOTTOM( 4 ),
    TOP_LEFT( 5 ),
    TOP_RIGHT( 6 ),
    BOTTOM_LEFT( 7 ),
    BOTTOM_RIGHT( 8 );

    private final int position;

    public static Position getPosition( final int positionCode )
    {
        switch( positionCode )
        {
            case 1 -> {
                return LEFT;
            }
            case 2 -> {
                return RIGHT;
            }
            case 3 -> {
                return TOP;
            }
            case 4 -> {
                return BOTTOM;
            }
            case 5 -> {
                return TOP_LEFT;
            }
            case 6 -> {
                return TOP_RIGHT;
            }
            case 7 -> {
                return BOTTOM_LEFT;
            }
            case 8 -> {
                return BOTTOM_RIGHT;
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_POSITION_TYPE,
                                            "i18n.analytics.invalid.position" );
        }
    }
}
