package com.tlc.crm.core.api.deal.model;

import java.math.BigDecimal;

/**
 * A record representing a summary of deal forecast data.
 *
 * @param totalDeals          The total number of deals included in the summary.
 * @param totalOpenDealsValue The aggregated monetary value of all deals that are currently in an 'open' state.
 * @param totalWonDealsValue  The aggregated monetary value of all deals that have been marked as 'won'.
 * @author ThalaimalaiPandiyanT
 * @version 2.0
 */
public record ForecastSummary(Long totalDeals, BigDecimal totalOpenDealsValue, BigDecimal totalWonDealsValue) {
}
