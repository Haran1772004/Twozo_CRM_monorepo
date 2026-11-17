package com.tlc.crm.core.features.api.quota.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

/**
 * Represents the type of quota time period supported by the system
 *
 * @author kishore J
 * @version 1.0
 */
public enum QuotaTimePeriod {

    WEEKLY("i18n.quota.time.period.weekly", (short) 1),
    MONTHLY("i18n.quota.time.period.monthly", (short) 2),
    QUARTERLY("i18n.quota.time.period.quarterly", (short) 3),
    YEARLY("i18n.quota.time.period.yearly", (short) 4);

    private final short id;
    private final String displayName;

    QuotaTimePeriod(final String displayName, final short id) {
        this.displayName = displayName;
        this.id = id;
    }

    public short getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * <p>
     * Gets the {@link QuotaTimePeriod} by ID
     * </p>
     *
     * @param id Represents an id
     * @return The {@link QuotaTimePeriod}
     */
    public static QuotaTimePeriod getById(final short id) {
        return switch (id) {
            case 1 -> QuotaTimePeriod.WEEKLY;
            case 2 -> QuotaTimePeriod.MONTHLY;
            case 3 -> QuotaTimePeriod.QUARTERLY;
            case 4 -> QuotaTimePeriod.YEARLY;
            default -> throw ErrorCode.get(CoreErrorCodes.INVALID_QUOTA_TIME_PERIOD);
        };
    }
}
