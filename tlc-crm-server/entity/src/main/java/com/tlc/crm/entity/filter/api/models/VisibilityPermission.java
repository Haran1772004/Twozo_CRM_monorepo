package com.tlc.crm.entity.filter.api.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;

/**
 * <p>
 * A enum that represents the visibility access data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum VisibilityPermission {

    CAN_VIEW((short) 1, false, false),
    CAN_VIEW_AND_EDIT((short) 2, true, true),
    ;

    private final short id;
    private final boolean isEditable;
    private final boolean isDeletable;

    VisibilityPermission(final short id, final boolean isEditable, final boolean isDeletable) {
        this.id = id;
        this.isEditable = isEditable;
        this.isDeletable = isDeletable;
    }

    /**
     * <p>
     * Gets the visibility permission id
     * </p>
     *
     * @return The visibility permission id
     */
    public short getId() {
        return id;
    }

    /**
     * <p>
     * Checks if the visibility permission is editable or not
     * </p>
     *
     * @return If visibility permission is editable it will return true, otherwise it will return false
     */
    public boolean isEditable() {
        return isEditable;
    }

    /**
     * <p>
     * Checks if the visibility permission is deletable or not
     * </p>
     *
     * @return If visibility permission is deletable it will return true, otherwise it will return false
     */
    public boolean isDeletable() {
        return isDeletable;
    }

    /**
     * <p>
     * Gets the {@link VisibilityPermission}
     * </p>
     *
     * @param id Represents the visibility permission id
     * @return The {@link VisibilityPermission}
     */
    public static VisibilityPermission get(final short id) {
        return switch (id) {
            case 1 -> CAN_VIEW;
            case 2 -> CAN_VIEW_AND_EDIT;
            default ->
                    throw ErrorCode.get(EntityErrorCodes.INVALID_FILTER_VISIBILITY_PERMISSION, "i18n.filter.visibility.permission.invalid");
        };
    }
}
