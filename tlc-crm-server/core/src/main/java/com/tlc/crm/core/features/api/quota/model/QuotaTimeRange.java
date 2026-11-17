package com.tlc.crm.core.features.api.quota.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

/**
 * Represents the type of quota time range supported by the system
 *
 * @author kishore J
 * @version 1.0
 */
public enum QuotaTimeRange {

    THIS_WEEK((short) 1),
    NEXT_WEEK((short) 2),
    ALL_WEEK_THIS_MONTH((short) 3),
    ALL_WEEK_THIS_QUARTER((short) 4),
    ALL_WEEK_THIS_YEAR((short) 5),
    WEEK_WITH_CUSTOM_RANGE((short) 6),
    THIS_MONTH((short) 7),
    NEXT_MONTH((short) 8),
    ALL_MONTH_THIS_QUARTER((short) 9),
    ALL_MONTH_THIS_YEAR((short) 10),
    MONTH_WITH_CUSTOM_RANGE((short) 11),
    THIS_QUARTER((short) 12),
    NEXT_QUARTER((short) 13),
    ALL_QUARTER_THIS_YEAR((short) 14),
    QUARTER_WITH_CUSTOM_RANGE((short) 15),
    THIS_YEAR((short) 16),
    NEXT_YEAR((short) 17),
    YEAR_WITH_CUSTOM_RANGE((short) 18),
    ;

    private final short id;

    QuotaTimeRange(final short id) {
        this.id = id;
    }

    public short getId() {
        return id;
    }

    /**
     * <p>
     * Gets the {@link QuotaTimeRange} by ID
     * </p>
     *
     * @param id Represents the id of the {@link QuotaTimeRange}
     * @return The {@link QuotaTimeRange}
     */
    public static QuotaTimeRange getById(final short id) {
        return switch (id) {
            case 1 -> QuotaTimeRange.THIS_WEEK;
            case 2 -> QuotaTimeRange.NEXT_WEEK;
            case 3 -> QuotaTimeRange.ALL_WEEK_THIS_MONTH;
            case 4 -> QuotaTimeRange.ALL_WEEK_THIS_QUARTER;
            case 5 -> QuotaTimeRange.ALL_WEEK_THIS_YEAR;
            case 6 -> QuotaTimeRange.WEEK_WITH_CUSTOM_RANGE;
            case 7 -> QuotaTimeRange.THIS_MONTH;
            case 8 -> QuotaTimeRange.NEXT_MONTH;
            case 9 -> QuotaTimeRange.ALL_MONTH_THIS_QUARTER;
            case 10 -> QuotaTimeRange.ALL_MONTH_THIS_YEAR;
            case 11 -> QuotaTimeRange.MONTH_WITH_CUSTOM_RANGE;
            case 12 -> QuotaTimeRange.THIS_QUARTER;
            case 13 -> QuotaTimeRange.NEXT_QUARTER;
            case 14 -> QuotaTimeRange.ALL_QUARTER_THIS_YEAR;
            case 15 -> QuotaTimeRange.QUARTER_WITH_CUSTOM_RANGE;
            case 16 -> QuotaTimeRange.THIS_YEAR;
            case 17 -> QuotaTimeRange.NEXT_YEAR;
            case 18 -> QuotaTimeRange.YEAR_WITH_CUSTOM_RANGE;
            default -> throw ErrorCode.get(CoreErrorCodes.INVALID_QUOTA_TIME_RANGE);
        };
    }
}
