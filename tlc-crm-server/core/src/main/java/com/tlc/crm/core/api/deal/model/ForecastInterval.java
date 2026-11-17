package com.tlc.crm.core.api.deal.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;

/**
 * <p>
 * Represents the different intervals.
 * </p>
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public enum ForecastInterval implements ForecastFieldHolder {

    MONTH("Month", (short) 1),
    WEEK("Week", (short) 2),
    QUARTER("Quarter", (short) 3);

    private final String name;
    private final short id;

    ForecastInterval(final String name, final short id) {
        this.name = name;
        this.id = id;
    }

    /**
     * {@inheritDoc}
     *
     * @return The forecast field id
     */
    @Override
    public short id() {
        return id;
    }

    /**
     * {@inheritDoc}
     *
     * @return The forecast field name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * <p>
     * Fetches the forecast interval for the given id
     * </p>
     *
     * @param id Represents the forecast interval id
     * @return The {@link ForecastInterval}
     */
    public static ForecastInterval getForeCastIntervalById(final short id) {
        return switch (id) {
            case 1 -> MONTH;
            case 2 -> WEEK;
            case 3 -> QUARTER;
            default -> throw ErrorCode.get(DealErrorCodes.INVALID_FORECAST_INTERVAL);
        };
    }
}