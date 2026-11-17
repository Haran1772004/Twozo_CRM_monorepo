package com.tlc.crm.core.internal.async.bulk.processor.model;

import com.tlc.commons.json.JsonObject;

/**
 * <p>
 * Holds an asynchronous action request
 * </p>
 *
 * @param orgId       Represents an org id
 * @param userId      Represents an user id
 * @param modelId     Represents an model id
 * @param requestJson The {@link JsonObject} containing request details
 */
public record AsyncActionRequest(long orgId, long userId, long modelId, JsonObject requestJson) {
}
