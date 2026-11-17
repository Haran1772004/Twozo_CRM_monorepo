package com.tlc.crm.security.permission.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;

/**
 * Defines the fundamental basis or strategy used to store the data within the request/response data.
 *
 * @author Sathish
 * @version 1.0
 */
public enum AccessKeyBasis {

    /**
     * Indicates that the access key is based on a unique numeric identifier (e.g., Primary Key).
     */
    ID((short) 1),

    /**
     * Indicates that the access key is based on a unique string alias or name.
     */
    ALIAS_NAME((short) 2),
    ;

    private final short id;

    AccessKeyBasis(final short id) {
        this.id = id;
    }

    /**
     * Retrieves the numeric identifier associated with this access basis.
     *
     * @return the short value representing the basis type.
     */
    public short getId() {
        return id;
    }

    /**
     * Resolves the {@link AccessKeyBasis} corresponding to the given numeric ID.
     *
     * @param id the numeric ID to look up.
     * @return the matching {@link AccessKeyBasis} constant.
     * @throws ErrorCode if the provided ID does not map to a valid basis type
     *                   (specifically {@link PermissionErrorCodes#INVALID_TYPE}).
     */
    public static AccessKeyBasis get(final short id) {
        return switch (id) {
            case 1 -> ID;
            case 2 -> ALIAS_NAME;
            default -> throw ErrorCode.get(PermissionErrorCodes.INVALID_TYPE);
        };
    }
}