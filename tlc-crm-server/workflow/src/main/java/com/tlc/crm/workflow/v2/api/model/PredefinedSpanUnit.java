package com.tlc.crm.workflow.v2.api.model;

import com.tlc.commons.code.ErrorCode;

import com.tlc.crm.workflow.v2.internal.status.WorkflowV2ErrorCodes;

/**
 * Predefined span units for workflow nodes.
 *
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public enum PredefinedSpanUnit {

    ONE_DAY("1 day", (short) 1, true, true),
    THREE_DAYS("3 days", (short) 2, true, true),
    FIVE_DAYS("5 days", (short) 3, true, true),
    ONE_WEEK("1 week", (short) 4, true, true),
    NEXT_MONDAY("Next Monday", (short) 5, true, false),
    NEXT_TUESDAY("Next Tuesday", (short) 6, true, false),
    NEXT_WEDNESDAY("Next Wednesday", (short) 7, true, false),
    NEXT_THURSDAY("Next Thursday", (short) 8, true, false),
    NEXT_FRIDAY("Next Friday", (short) 9, true, false),
    NEXT_SATURDAY("Next Saturday", (short) 10, true, false),
    NEXT_SUNDAY("Next Sunday", (short) 11, true, false),
    ONE_HOUR("1 hour", (short) 12, false, true),
    EIGHT_HOURS("8 hours", (short) 13, false, true),
    TWELVE_HOURS("12 hours", (short) 14, false, true),
    FIVE_MINUTES("5 minutes", (short) 15, false, true),
    FIFTEEN_MINUTES("15 minutes", (short) 16, false, true),
    THIRTY_MINUTES("30 minutes", (short) 17, false, true),
    ;

    private final short id;
    private final String name;
    private final boolean hasDueTime;
    private final boolean hasSkipWeekends;

    PredefinedSpanUnit(final String name, final short id, final boolean hasDueTime, final boolean hasSkipWeekends) {
        this.name = name;
        this.id = id;
        this.hasDueTime = hasDueTime;
        this.hasSkipWeekends = hasSkipWeekends;
    }

    public short getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean hasDueTime() {
        return hasDueTime;
    }

    public boolean hasSkipWeekends() {
        return hasSkipWeekends;
    }

    public static PredefinedSpanUnit get(final short id) {
        return switch (id) {
            case 1 -> ONE_DAY;
            case 2 -> THREE_DAYS;
            case 3 -> FIVE_DAYS;
            case 4 -> ONE_WEEK;
            case 5 -> NEXT_MONDAY;
            case 6 -> NEXT_TUESDAY;
            case 7 -> NEXT_WEDNESDAY;
            case 8 -> NEXT_THURSDAY;
            case 9 -> NEXT_FRIDAY;
            case 10 -> NEXT_SATURDAY;
            case 11 -> NEXT_SUNDAY;
            case 12 -> ONE_HOUR;
            case 13 -> EIGHT_HOURS;
            case 14 -> TWELVE_HOURS;
            case 15 -> FIVE_MINUTES;
            case 16 -> FIFTEEN_MINUTES;
            case 17 -> THIRTY_MINUTES;
            default -> throw ErrorCode.get(WorkflowV2ErrorCodes.INVALID_SPAN_UNIT, "i18n.workflow.invalid.span.unit");
        };
    }
}
