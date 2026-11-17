package com.tlc.crm.core.features.api.quota.model;

/**
 * <p>
 * Represents the start and end boundaries of a quota, as epoch timestamps.
 * </p>
 *
 * @param startTime the inclusive start of the quota window
 * @param endTime   the inclusive end of the quota window
 * @author Ramesh G
 * @version 1.0
 */
public record QuotaPeriod(long startTime, long endTime) {
}