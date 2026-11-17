package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Schedule kinds persisted in {@code OMWFLEXECUTIONSCHEDULEV2}.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum ScheduleType {

    DELAY((short) 1),
    WAIT((short) 2),
    ;

    private final short id;

    ScheduleType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * Returns the schedule type for the given ID.
     *
     * @param id The ID.
     * @return The schedule type.
     */
    public static ScheduleType get(final short id) {
        return switch (id) {
            case 1 -> DELAY;
            case 2 -> WAIT;
            default ->
                    throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_SCHEDULE_TYPE, "i18n.workflow.invalid.schedule.type");
        };
    }
}
