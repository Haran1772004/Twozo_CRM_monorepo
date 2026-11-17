package com.tlc.crm.core.api.deal.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.crm.core.internal.deal.status.DealErrorCodes;

/**
 * An enums contains the various {@link ForecastStage} data
 *
 * @author Kishore J
 * @version 1.0
 */
public enum ForecastStage implements ForecastFieldHolder {

    THREE_COLUMNS("3 columns", 3, (short) 1),
    FOUR_COLUMNS("4 columns", 4, (short) 2),
    FIVE_COLUMNS("5 columns", 5, (short) 3);

    private final String name;
    private final int value;
    private final short id;

    ForecastStage(final String name, final int value, final short id) {
        this.name = name;
        this.value = value;
        this.id = id;
    }

    /**
     * <p>
     * Fetches the forecast stage value
     * </p>
     *
     * @return The forecast stage value
     */
    public int getValue() {
        return value;
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
     * Fetches the forecast columns for the given id
     * </p>
     *
     * @param id Represents the forecast stage id
     * @return The {@link ForecastStage}
     */
    public static ForecastStage getForecastStageById(final short id) {
        return switch (id) {
            case 1 -> THREE_COLUMNS;
            case 2 -> FOUR_COLUMNS;
            case 3 -> FIVE_COLUMNS;
            default -> throw ErrorCode.get(DealErrorCodes.INVALID_FORECAST_STAGE);
        };
    }
}
