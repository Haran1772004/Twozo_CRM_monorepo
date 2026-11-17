package com.tlc.email.server.fields;

/**
 * Represents the fields related to inbound email receiver.
 *
 * @author sundar
 * @version 1.0
 */
public final class InboundEmailReceiverFields {

    private InboundEmailReceiverFields() {
    }

    public static final String TYPE = "type";
    public static final String DATA = "data";
    public static final String BUCKET_NAME = "bucketName";
    public static final String OBJECT_ID = "objectId";
    public static final String TIMESTAMP = "timestamp";
    public static final String PUBLISH_INBOUND = "publishInbound";

    public static final String STATE = "state";
    public static final String STATE_SEND_REQUESTED = "send_requested";
}
