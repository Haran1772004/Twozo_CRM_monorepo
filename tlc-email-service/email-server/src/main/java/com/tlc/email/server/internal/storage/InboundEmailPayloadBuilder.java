package com.tlc.email.server.internal.storage;

import com.tlc.attachment.storage.model.ObjectSummary;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.email.server.fields.InboundEmailReceiverFields;

/**
 * Builds Kafka payloads for inbound email events.
 *
 * @author sundar
 * @version 1.0
 */
public final class InboundEmailPayloadBuilder {

    private InboundEmailPayloadBuilder() {
    }

    public static String buildPayload(final String bucketName, final String objectId, final long timestamp) {
        final JsonObject data = Json.object();
        data.put(InboundEmailReceiverFields.BUCKET_NAME, bucketName);
        data.put(InboundEmailReceiverFields.OBJECT_ID, objectId);
        data.put(InboundEmailReceiverFields.TIMESTAMP, timestamp);

        final JsonObject payload = Json.object();
        payload.put(InboundEmailReceiverFields.DATA, data);
        return payload.toString();
    }

    public static String buildPayload(final String bucketName, final ObjectSummary objectSummary) {
        return buildPayload(bucketName, objectSummary.key(), objectSummary.lastModified());
    }
}
