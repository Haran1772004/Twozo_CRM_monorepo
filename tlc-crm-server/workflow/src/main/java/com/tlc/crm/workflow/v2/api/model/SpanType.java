package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Represents whether a delay/wait span uses a predefined preset or custom duration.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum SpanType {

    PREDEFINED((short) 1),
    CUSTOM((short) 2),
    ;

    private final short id;

    SpanType(final short id) {
        this.id = id;
    }

    public static SpanType get(final short id) {
        return switch (id) {
            case 1 -> PREDEFINED;
            case 2 -> CUSTOM;
            default ->
                    throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_SPAN_CONFIGURATION, "i18n.workflow.invalid.span.type");
        };
    }

    public short getId() {
        return id;
    }
}
