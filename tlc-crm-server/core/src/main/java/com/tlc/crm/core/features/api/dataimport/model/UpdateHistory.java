package com.tlc.crm.core.features.api.dataimport.model;

import com.tlc.commons.json.JsonObject;

/**
 * Holds both the previous and current data of a specific record from a source
 *
 * @param previousState Represents the data of the record before the update
 * @param currentState  Represents the most recent data of the record to be updated
 * @author sundar
 * @version 1.0
 */
public record UpdateHistory(JsonObject previousState, JsonObject currentState) {
}
