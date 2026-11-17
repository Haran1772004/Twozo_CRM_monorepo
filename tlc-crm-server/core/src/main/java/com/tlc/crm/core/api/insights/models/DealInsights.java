package com.tlc.crm.core.api.insights.models;

/**
 * <p>
 * Represents the deal insights data.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class DealInsights {

    private long openDealsCount;
    private long wonDealsCount;
    private long lostDealsCount;

    /**
     * <p>
     * Gets the open deals count.
     * </p>
     *
     * @return the get open deals count value
     */
    public long getOpenDealsCount() {
        return openDealsCount;
    }

    /**
     * <p>
     * Sets the open deals count.
     * </p>
     *
     * @param openDealsCount the open deals count
     */
    public void setOpenDealsCount(final long openDealsCount) {
        this.openDealsCount = openDealsCount;
    }

    /**
     * <p>
     * Gets the won deals count.
     * </p>
     *
     * @return the get won deals count value
     */
    public long getWonDealsCount() {
        return wonDealsCount;
    }

    /**
     * <p>
     * Sets the won deals count.
     * </p>
     *
     * @param wonDealsCount the won deals count
     */
    public void setWonDealsCount(final long wonDealsCount) {
        this.wonDealsCount = wonDealsCount;
    }

    /**
     * <p>
     * Gets the lost deals count.
     * </p>
     *
     * @return the get lost deals count value
     */
    public long getLostDealsCount() {
        return lostDealsCount;
    }

    /**
     * <p>
     * Sets the lost deals count.
     * </p>
     *
     * @param lostDealsCount the lost deals count
     */
    public void setLostDealsCount(final long lostDealsCount) {
        this.lostDealsCount = lostDealsCount;
    }
}