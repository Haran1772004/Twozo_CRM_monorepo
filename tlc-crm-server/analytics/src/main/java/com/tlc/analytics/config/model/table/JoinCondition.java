package com.tlc.analytics.config.model.table;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;

/**
 * <p>
 * Represents the join condition types
 * </p>
 *
 * @author James
 * @version 1.0
 */
@Getter
public enum JoinCondition
{
    EQUAL( 1 );

    private final int conditionCode;

    JoinCondition( int condition )
    {
        this.conditionCode = condition;
    }

    public static JoinCondition getJoinTypeByCode( final long conditionCode )
    {
        if ( (int) conditionCode == 1 )
        {
            return EQUAL;
        }
        throw ErrorCode.get( ConfigErrorCodes.INVALID_JOIN_CONDITION,
                     "i18n.analytics.invalid.data.type" );
    }
}
