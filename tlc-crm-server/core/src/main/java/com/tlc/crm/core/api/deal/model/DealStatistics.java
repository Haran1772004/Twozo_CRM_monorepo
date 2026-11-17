package com.tlc.crm.core.api.deal.model;

import java.util.Objects;

/**
 * Refers the deal counts details for summary.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class DealStatistics {

    private long ZERO_VALUE = 0L;
    private Long openDealsCount;
    private long wonDealsCount;
    private long lostDealsCount;

    public Long getOpenDealsCount() {
        return Objects.isNull(openDealsCount) ? ZERO_VALUE : openDealsCount ;
    }

    public void setOpenDealsCount(final Long openDealsCount) {
        this.openDealsCount = Objects.requireNonNullElse(openDealsCount, ZERO_VALUE);
    }

    public long getClosedDealsCount() {
        return wonDealsCount + lostDealsCount;
    }

    public long getWonDealsCount() {
        return wonDealsCount;
    }

    public void setWonDealsCount(final Long wonDealsCount) {
        this.wonDealsCount = Objects.requireNonNullElse(wonDealsCount, ZERO_VALUE);
    }

    public long getLostDealsCount() {
        return lostDealsCount;
    }

    public void setLostDealsCount(final Long lostDealsCount) {
        this.lostDealsCount = Objects.requireNonNullElse(lostDealsCount, ZERO_VALUE);
    }
}
