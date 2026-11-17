package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Represents who can trigger a workflow V2 automation.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum TriggerScope {

    SELECTED_USERS((short) 1),
    EVERYONE((short) 2),
    ;

    private final short id;

    TriggerScope(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public static TriggerScope get(final short id) {
        return switch (id) {
            case 1 -> SELECTED_USERS;
            case 2 -> EVERYONE;
            default ->
                    throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_TRIGGER_SCOPE, "i18n.workflow.invalid.trigger.scope");
        };
    }
}
