package com.tlc.analytics.config.model.view.share;

import com.tlc.analytics.config.internal.ConfigErrorCodes;

import com.tlc.commons.code.ErrorCode;

import lombok.Getter;

/**
 * Enum representing different types of permissions for sharing a report.
 *
 * @author Aswath
 * @version 1.0
 */
@Getter
public enum PermissionType
{
    /**
     * Permission to view the report
     */
    CAN_VIEW( 1 ),

    /**
     * Permission to edit the report
     */
    CAN_EDIT( 2 ),

    /**
     * Permission to manage the report (e.g., delete, share)
     */
    CAN_MANAGE( 3 );

    private final int typeCode;

    /**
     * Constructor to initialize the type code.
     *
     * @param typeCode The numeric code associated with the {@link PermissionType}.
     */
    PermissionType( int typeCode )
    {
        this.typeCode = typeCode;
    }

    /**
     * Gets the {@link PermissionType} based on the provided type code.
     *
     * @param typeCode The type code to match against {@link PermissionType}.
     * @return The PermissionType corresponding to the provided type code.
     * @throws IllegalArgumentException If the provided type code does not match any {@link PermissionType}.
     */
    public static PermissionType getType( final int typeCode )
    {
        switch( typeCode )
        {
            case 1 ->
            {
                return CAN_VIEW;
            }
            case 2 ->
            {
                return CAN_EDIT;
            }
            case 3 ->
            {
                return CAN_MANAGE;
            }
            default -> throw ErrorCode.get( ConfigErrorCodes.INVALID_PERMISSION_TYPE,
                                            "i18n.analytics.invalid.permission.type");
        }
    }
}
