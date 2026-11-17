package com.tlc.crm.zapier.internal.event;

import com.tlc.commons.json.JsonObject;

import com.tlc.crm.zapier.api.models.ZapEventWebhook;

import java.util.Collection;

/**
 * A data record representing a Zapier event request.
 * It encapsulates the target webhooks and the corresponding payloads to be sent.
 *
 * @param zapEventWebhooks The collection of webhook endpoints to which the event payloads should be sent.
 * @param payloads         The collection of {@link JsonObject} payloads representing the event data.
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public record ZapEventRequest(Collection<ZapEventWebhook> zapEventWebhooks, Collection<JsonObject> payloads) {
}
