package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Represents workflow V2 node types.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum NodeType {

    TRIGGER((short) 1),
    INSTANT_CONDITION((short) 2),
    IF_ELSE_CONDITION((short) 3),
    WAIT_FOR_CONDITION((short) 4),
    ACTION((short) 5),
    DELAY((short) 6),
    ;

    private final short id;

    NodeType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public static NodeType get(final short id) {
        return switch (id) {
            case 1 -> TRIGGER;
            case 2 -> INSTANT_CONDITION;
            case 3 -> IF_ELSE_CONDITION;
            case 4 -> WAIT_FOR_CONDITION;
            case 5 -> ACTION;
            case 6 -> DELAY;
            default -> throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_NODE_TYPE, "i18n.workflow.invalid.node.type");
        };
    }

}
