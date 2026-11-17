package com.tlc.cdc.core.models;

import com.tlc.commons.json.JsonObject;

/**
 * Represents an CDC record.
 *
 * @param transactionId The unique identifier of the source transaction.
 * @param payload       The JSON representation of the CDC event.
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public record CdcEventRecord(Long transactionId, JsonObject payload) {
}
