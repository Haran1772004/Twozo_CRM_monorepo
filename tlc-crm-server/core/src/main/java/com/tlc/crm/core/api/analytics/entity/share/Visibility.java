package com.tlc.crm.core.api.analytics.entity.share;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import lombok.Getter;

/**
 * <p>
 * A enum that represents the dashboard visibility data
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
public enum Visibility
{
    JUST_ME( ( short ) 1),
    SHARED_WITH( ( short ) 2),
    ;

    private final short id;

    Visibility( short id )
    {
        this.id = id;
    }

    /**
     * <p>
     * Gets the {@link com.tlc.crm.entity.filter.api.models.Visibility}
     * </p>
     *
     * @param id Represents the visibility id
     * @return The {@link com.tlc.crm.entity.filter.api.models.Visibility}
     */
    public static Visibility get( final short id )
    {
        return switch( id )
        {
            case 1 -> JUST_ME;
            case 2 -> SHARED_WITH;
            default -> throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY,
                                    "i18n.crm.analytics.visibility.invalid" );
        };
    }
}
