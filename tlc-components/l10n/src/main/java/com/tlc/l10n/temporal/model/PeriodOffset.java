package com.tlc.l10n.temporal.model;

/**
 * <p>
 * Holds the period offset values
 * </p>
 *
 * @author kishore J
 * @version 1.0
 */
public enum PeriodOffset {

    PREVIOUS(-1),
    CURRENT(0),
    NEXT(1);


    private final int value;

    PeriodOffset(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}