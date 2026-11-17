package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Represents the time unit for a custom delay/wait span.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum CustomSpanUnit {

    MINUTES("minutes", (short) 1),
    HOURS("hours", (short) 2),
    DAYS("days", (short) 3),
    WEEKS("weeks", (short) 4),
    ;

    private final String aliasName;
    private final short id;

    CustomSpanUnit(final String aliasName, final short id) {
        this.aliasName = aliasName;
        this.id = id;
    }

    public String getAliasName() {
        return aliasName;
    }

    public short getId() {
        return id;
    }

    public static CustomSpanUnit get(final short id) {
        return switch (id) {
            case 1 -> MINUTES;
            case 2 -> HOURS;
            case 3 -> DAYS;
            case 4 -> WEEKS;
            default -> throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_SPAN_UNIT, "i18n.workflow.invalid.span.unit");
        };
    }
}
