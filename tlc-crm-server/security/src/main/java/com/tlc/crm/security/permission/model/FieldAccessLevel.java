package com.tlc.crm.security.permission.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.security.internal.status.PermissionErrorCodes;

/**
 * Represents the different access levels that can be assigned to a field
 * within the permission model.
 * <p>
 * Each access level defines how a user or role can interact with a field —
 * whether they can edit it, view it as read-only, or hide it completely.
 * </p>
 *
 * <ul>
 *   <li><b>EDITABLE</b> – Field is visible and can be modified.</li>
 *   <li><b>READ_ONLY</b> – Field is visible but cannot be modified.</li>
 *   <li><b>HIDDEN</b> – Field is not visible to the user.</li>
 * </ul>
 *
 * @author  Sathish
 * @version 1.0
 */
public enum FieldAccessLevel {

    EDITABLE((short) 1),
    READ_ONLY((short) 2),
    HIDDEN((short) 3),
    ;

    private final short id;

    FieldAccessLevel(short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public static FieldAccessLevel get(final short id) {
        return switch (id) {
            case 1 -> EDITABLE;
            case 2 -> READ_ONLY;
            case 3 -> HIDDEN;
            default -> throw ErrorCode.get(PermissionErrorCodes.FIELD_ACCESS_LEVEL_NOT_FOUND);
        };
    }
}
