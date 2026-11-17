package com.tlc.crm.entity.fieldaudit.models;

/**
 * Supported date intervals for filtering field audit records.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
public enum DateIntervals {

    TODAY((short) 1, "i18n.field.audit.date.interval.today"),
    YESTERDAY((short) 2, "i18n.field.audit.date.interval.yesterday"),
    LAST_7_DAYS((short) 3, "i18n.field.audit.date.interval.last.7.days"),
    LAST_30_DAYS((short) 4, "i18n.field.audit.date.interval.last.30.days"),
    CUSTOM((short) 5, "i18n.field.audit.date.interval.custom.period"),
    ;

    private final short id;
    private final String displayName;

    DateIntervals(final short id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public short getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }
}
