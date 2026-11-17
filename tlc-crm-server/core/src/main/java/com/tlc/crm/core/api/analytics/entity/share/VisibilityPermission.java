package com.tlc.crm.core.api.analytics.entity.share;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.core.internal.analytics.status.AnalyticsErrorCodes;

import lombok.Getter;

/**
 * <p>
 * A enum that represents the visibility access data.
 * </p>
 *
 * @author Venkatesh Nambiyar
 * @version 1.0
 */
@Getter
public enum VisibilityPermission
{
    CAN_VIEW( ( short ) 1 ),
    CAN_EDIT( ( short ) 2 ),
    CAN_MANAGE( ( short ) 3 ),
    OWNER( ( short ) 4 ),
    ;

    private final short id;

    VisibilityPermission( short id )
    {
        this.id = id;
    }

    /**
     * <p>
     * Gets the {@link VisibilityPermission}
     * </p>
     *
     * @param id Represents the visibility permission id
     * @return The {@link VisibilityPermission}
     */
    public static VisibilityPermission get( final short id )
    {
        return switch( id )
        {
            case 1 -> CAN_VIEW;
            case 2 -> CAN_EDIT;
            case 3 -> CAN_MANAGE;
            case 4 -> OWNER;
            default -> throw ErrorCode.get( AnalyticsErrorCodes.INVALID_DASHBOARD_VISIBILITY_PERMISSION,
                                    "i18n.crm.analytics.visibility.permission.invalid" );
        };
    }
}
