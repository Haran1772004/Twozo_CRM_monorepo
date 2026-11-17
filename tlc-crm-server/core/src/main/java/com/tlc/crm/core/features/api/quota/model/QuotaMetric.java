package com.tlc.crm.core.features.api.quota.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

/**
 * Represents the type of quota metrics supported by the system
 *
 * @author kishore J
 * @version 1.0
 */
public enum QuotaMetric {

    DEAL_VALUE((short) 1, "i18n.quota.deal.value.placeholder"),
    DEAL_COUNT((short) 2, "i18n.quota.deal.count.placeholder"),
    ;

    private final short id;
    private final String displayName;

    QuotaMetric(final short id, final String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link QuotaMetric} by id
     * </p>
     *
     * @param id Represents the quota metric id
     * @return The {@link QuotaMetric}
     */
    public static QuotaMetric getById(final short id) {
        return switch (id) {
            case 1 -> QuotaMetric.DEAL_VALUE;
            case 2 -> QuotaMetric.DEAL_COUNT;
            default -> throw ErrorCode.get(CoreErrorCodes.INVALID_QUOTA_METRIC);
        };
    }

    /**
     * Checks whether this metric represents a deal value quota.
     *
     * @return true if this metric is {@link #DEAL_VALUE}, otherwise false
     */
    public boolean isDealValue() {
        return this == DEAL_VALUE;
    }
}
