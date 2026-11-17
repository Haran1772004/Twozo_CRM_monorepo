package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Represents wait event context types.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum EventContextType {

    TRIGGER((short) 1),
    ACTION((short) 2),
    ;

    private final short id;

    EventContextType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public static EventContextType get(final short id) {
        return switch (id) {
            case 1 -> TRIGGER;
            case 2 -> ACTION;
            default ->
                    throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_EVENT_CONTEXT, "i18n.workflow.invalid.event.context");
        };
    }

    public static EventContextType fromName(final String name) {
        return EventContextType.valueOf(name.trim().toUpperCase());
    }
}
