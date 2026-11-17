package com.tlc.crm.workflow.api.model.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * <p>
 * A model that represents the {@link Type}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum Type {

    WORKFLOW_AUTOMATION((short) 1),
    WORKFLOW_HISTORY((short) 2),
    WORKFLOW_AUTOMATION_V2((short) 3),
    WORKFLOW_HISTORY_V2((short) 4),
    ;

    private final short id;

    Type(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the type of the automation filter
     * </p>
     *
     * @param id Refers the type id of the filter
     * @return The {@link Type}
     */
    public static Type get(final short id) {
        return switch (id) {
            case 1 -> WORKFLOW_AUTOMATION;
            case 2 -> WORKFLOW_HISTORY;
            case 3 -> WORKFLOW_AUTOMATION_V2;
            case 4 -> WORKFLOW_HISTORY_V2;
            default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_FILTER_TYPE, "i18n.workflow.filter.type.invalid");
        };
    }
}
