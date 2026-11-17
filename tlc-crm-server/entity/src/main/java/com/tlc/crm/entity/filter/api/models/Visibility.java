package com.tlc.crm.entity.filter.api.models;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.entity.internal.status.EntityErrorCodes;

/**
 * <p>
 * A enum that represents the visibility data.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 */
public enum Visibility {

    JUST_ME((short) 1),
    EVERY_ONE((short) 2),
    SELECTED((short) 3),
    ;

    private final short id;

    Visibility(final short id) {
        this.id = id;
    }

    /**
     * <p>
     * Gets the visibility id
     * </p>
     *
     * @return The visibility id
     */
    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link Visibility}
     * </p>
     *
     * @param id Represents the visibility id
     * @return The {@link Visibility}
     */
    public static Visibility get(final short id) {
        return switch (id) {
            case 1 -> JUST_ME;
            case 2 -> EVERY_ONE;
            case 3 -> SELECTED;
            default ->
                    throw ErrorCode.get(EntityErrorCodes.INVALID_FILTER_VISIBILITY, "i18n.filter.visibility.invalid");
        };
    }
}
