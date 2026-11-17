package com.tlc.cdc.core.enrichment;

import com.tlc.commons.json.JsonObject;

/**
 * Represents an enriched CDC record produced after transaction-level aggregation
 * or business enrichment processing.
 *
 * @param transactionId   The unique identifier of the source transaction.
 * @param enrichedPayload The enriched JSON representation of the aggregated event.
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public record EnrichedRecord(Long transactionId, JsonObject enrichedPayload) {
}
