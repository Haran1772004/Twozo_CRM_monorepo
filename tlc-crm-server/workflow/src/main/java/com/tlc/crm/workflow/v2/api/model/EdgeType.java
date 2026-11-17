package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Represents graph edge types in workflow V2.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum EdgeType {

    NEXT((short) 1),
    FALLBACK((short) 2),
    ;

    private final short id;

    EdgeType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public static EdgeType get(final short id) {
        return switch (id) {
            case 1 -> NEXT;
            case 2 -> FALLBACK;
            default -> throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_EDGE_TYPE, "i18n.workflow.invalid.edge.type");
        };
    }
}
