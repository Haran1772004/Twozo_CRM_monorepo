package com.tlc.crm.workflow.api.model.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * <p>
 * A model that represents the {@link WorkflowStatus}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum WorkflowStatus {

    ANY_STATUS((short) -1),
    ACTIVE((short) 1),
    INACTIVE((short) 2);

    private final short id;

    WorkflowStatus(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the status of the automation filter
     * </p>
     *
     * @param id Refers the status is
     * @return The {@link WorkflowStatus}
     */
    public static WorkflowStatus get(final short id) {
        return switch (id) {
            case -1 -> ANY_STATUS;
            case 1 -> ACTIVE;
            case 2 -> INACTIVE;
            default -> throw ErrorCode.get(WorkflowErrorCodes.INVALID_STATUS, "i18n.workflow.filter.status.invalid");
        };
    }
}
