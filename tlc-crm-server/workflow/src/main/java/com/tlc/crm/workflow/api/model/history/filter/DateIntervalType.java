package com.tlc.crm.workflow.api.model.history.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * <p>
 * A model that represents the {@link DateIntervalType}
 * </p>
 *
 * @author karthick.v
 * @version 1.0
 */
public enum DateIntervalType {

    ALL((short) -1),
    TODAY((short) 1),
    YESTERDAY((short) 2),
    THIS_WEEK((short) 3),
    LAST_WEEK((short) 4),
    BETWEEN((short) 5),
    ;

    private final short id;

    DateIntervalType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the date interval type for the given id
     * </p>
     *
     * @param id Refers the id of the date interval type
     * @return The {@link DateIntervalType}
     */
    public static DateIntervalType get(final short id) {
        return switch (id) {
            case -1 -> ALL;
            case 1 -> TODAY;
            case 2 -> YESTERDAY;
            case 3 -> THIS_WEEK;
            case 4 -> LAST_WEEK;
            case 5 -> BETWEEN;
            default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_DATA_INTERVAL_TYPE, "i18n.workflow.filter.dateIntervalType.invalid");
        };
    }
}
