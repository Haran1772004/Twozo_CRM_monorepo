package com.tlc.crm.workflow.api.model.filter;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.workflow.internal.status.WorkflowErrorCodes;

/**
 * <p>
 * A model that represents the {@link TriggerType}
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public enum TriggerType {

    ALL_TRIGGERS((short) -1),
    ACTIVITY_CREATED((short) 1),
    ACTIVITY_EDITED((short) 2),
    ACTIVITY_DELETED((short) 3),
    CONTACT_CREATED((short) 4),
    CONTACT_EDITED((short) 5),
    CONTACT_DELETED((short) 6),
    DEAL_CREATED((short) 7),
    DEAL_EDITED((short) 8),
    DEAL_DELETED((short) 9),
    COMPANY_CREATED((short) 10),
    COMPANY_EDITED((short) 11),
    COMPANY_DELETED((short) 12),
    ;

    private final short id;

    TriggerType(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link TriggerType} for the given id
     * </p>
     *
     * @param id Refers the {@link TriggerType} id
     * @return The {@link TriggerType}
     */
    public static TriggerType get(final short id) {
        return switch (id) {
            case -1 -> ALL_TRIGGERS;
            case 1 -> ACTIVITY_CREATED;
            case 2 -> ACTIVITY_EDITED;
            case 3 -> ACTIVITY_DELETED;
            case 4 -> CONTACT_CREATED;
            case 5 -> CONTACT_EDITED;
            case 6 -> CONTACT_DELETED;
            case 7 -> DEAL_CREATED;
            case 8 -> DEAL_EDITED;
            case 9 -> DEAL_DELETED;
            case 10 -> COMPANY_CREATED;
            case 11 -> COMPANY_EDITED;
            case 12 -> COMPANY_DELETED;
            default ->
                    throw ErrorCode.get(WorkflowErrorCodes.INVALID_TRIGGER_TYPE, "i18n.workflow.filter.triggerType.invalid");
        };
    }
}
