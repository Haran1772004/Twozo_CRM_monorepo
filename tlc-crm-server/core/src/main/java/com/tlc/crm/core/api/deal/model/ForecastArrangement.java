package com.tlc.crm.core.api.deal.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;

/**
 * An enum contains the various {@link ForecastArrangement} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum ForecastArrangement implements ForecastFieldHolder {

    OPEN_DEAL_FIRST("Open deals first", (short) 1),
    WON_DEAL_FIRST("Won deals first", (short) 2);

    private final String name;
    private final short id;

    ForecastArrangement(final String name, final short id) {
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
     * Fetches the forecast for the given id
     * </p>
     *
     * @param id Represents the forecast arrangement id
     * @return The {@link ForecastArrangement}
     */
    public static ForecastArrangement getForecastArrangementById(final short id) {
        return switch (id) {
            case 1 -> OPEN_DEAL_FIRST;
            case 2 -> WON_DEAL_FIRST;
            default -> throw ErrorCode.get(DealErrorCodes.INVALID_FORECAST_ARRANGEMENT);
        };
    }
}
