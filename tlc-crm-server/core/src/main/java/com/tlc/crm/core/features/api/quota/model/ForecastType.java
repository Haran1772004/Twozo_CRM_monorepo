package com.tlc.crm.core.features.api.quota.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.status.CoreErrorCodes;

/**
 * Represents the type of forecast calculation based on whether won deals are included or excluded
 *
 * @author kishore J
 * @version 1.0
 */
public enum ForecastType {

    WON((short) 1, "i18n.quota.forecast.won.deal.placeholder", true),
    WITHOUT_WON((short) 2, "i18n.quota.forecast.without.won.placeholder", false);

    private final short id;
    private final String displayName;
    private final boolean isDefault;

    ForecastType(final short id, final String displayName, final boolean isDefault) {
        this.id = id;
        this.displayName = displayName;
        this.isDefault = isDefault;
    }

    public short getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isWon() {
        return this == WON;
    }

    /**
     * <p>
     * Fetches the {@link ForecastType}
     * </p>
     *
     * @param id Represents the forecast type id
     * @return The {@link ForecastType}
     */
    public static ForecastType getById(final short id) {
        return switch (id) {
            case 1 -> WON;
            case 2 -> WITHOUT_WON;
            default -> throw ErrorCode.get(CoreErrorCodes.FORECAST_WON_TYPE_NOT_FOUND);
        };
    }
}
