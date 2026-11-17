package com.tlc.cdc.core.models;

import com.tlc.commons.json.JsonObject;

/**
 * Represents a transaction-aggregated CDC (Change Data Capture) event
 * passed to downstream handlers for processing.
 *
 * @param id   The unique identifier associated with the aggregated CDC event.
 * @param data The aggregated or enriched JSON payload.
 * @author Thalaimalai Pandiyan T
 */
public record AggregatedCdcEventRequest(String id, JsonObject data) {
}
