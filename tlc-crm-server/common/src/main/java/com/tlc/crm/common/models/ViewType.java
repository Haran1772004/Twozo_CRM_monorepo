package com.tlc.crm.common.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.common.internal.resource.CommonErrorCodes;

/**
 * <p>
 * Represents the view type
 * </p>
 *
 * @author Maharaja S
 * @version 1.0
 */
public enum ViewType {

    LIST((short) 1),
    GRID((short) 2),
    ;

    private final short id;

    ViewType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link ViewType} by its id.
     * </p>
     *
     * @param id Represents the view id.
     * @return The {@link ViewType}.
     */
    public static ViewType get(final short id) {
        return switch (id) {
            case 1 -> LIST;
            case 2 -> GRID;
            default -> throw ErrorCode.get(CommonErrorCodes.INVALID_VIEW_TYPE, "i18n.common.invalid.view.type");
        };
    }
}