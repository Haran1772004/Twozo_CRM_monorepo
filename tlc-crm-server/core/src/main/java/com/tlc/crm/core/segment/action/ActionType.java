package com.tlc.crm.core.segment.action;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.crm.entity.internal.status.SegmentErrorCodes;

/**
 * <p>
 * Represents the action types for segment partial updates.
 * </p>
 *
 * @author Mohammed Asif J.
 * @version 1.0
 */
public enum ActionType {

    ADD_TO_FOLDER((short) NumericalConstant.ONE),
    STATUS_CHANGE((short) NumericalConstant.TWO),
    ;

    private final short id;

    ActionType(final short id) {
        this.id = id;
    }

    /**
     * <p>
     * Gets the action type id.
     * </p>
     *
     * @return The action type id.
     */
    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link ActionType} by its id.
     * </p>
     *
     * @param id Represents the action type id.
     * @return The matching {@link ActionType}.
     */
    public static ActionType get(final short id) {
        return switch (id) {
            case NumericalConstant.ONE -> ADD_TO_FOLDER;
            case NumericalConstant.TWO -> STATUS_CHANGE;
            default -> throw ErrorCode.get(SegmentErrorCodes.UNSUPPORTED_ACTION, "i18n.segment.unsupported.action");
        };
    }
}