package com.tlc.crm.core.segment.converter;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.service.Codec;
import com.tlc.crm.core.segment.models.SegmentRecomputeEvent;

import java.util.Objects;

/**
 * <p>
 * Codec for {@link SegmentRecomputeEvent} that uses the project {@link Json}
 * utility for compact encoding and decoding.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentRecomputeCodec implements Codec<SegmentRecomputeEvent> {

    private static final String ORG_ID = "orgId";
    private static final String SEGMENT_ID = "segmentId";
    private static final String ENQUEUED_AT = "enqueuedAt";

    private SegmentRecomputeCodec() {
    }

    /**
     * <p>
     * Holds the singleton instance of {@link SegmentRecomputeCodec}.
     * </p>
     */
    private static final class InstanceHolder {

        private static final SegmentRecomputeCodec INSTANCE = new SegmentRecomputeCodec();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link SegmentRecomputeCodec}.
     * </p>
     *
     * @return The {@link SegmentRecomputeCodec} instance.
     */
    public static SegmentRecomputeCodec getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * <p>
     * Encodes the given event into its compact JSON byte representation.
     * </p>
     *
     * @param segmentRecomputeEvent Represents the event to encode.
     * @return The encoded bytes, or {@code null} when the given value is {@code null}.
     */
    @Override
    public byte[] encode(final SegmentRecomputeEvent segmentRecomputeEvent) {
        if (Objects.isNull(segmentRecomputeEvent)) {
            return null;
        }
        final JsonObject jsonObject = Json.object()
                .put(ORG_ID, segmentRecomputeEvent.getOrgId())
                .put(SEGMENT_ID, segmentRecomputeEvent.getSegmentId())
                .put(ENQUEUED_AT, segmentRecomputeEvent.getEnqueuedAt());

        return jsonObject.getBytes();
    }

    /**
     * <p>
     * Decodes the given bytes back into a {@link SegmentRecomputeEvent}.
     * </p>
     *
     * @param data Represents the bytes to decode.
     * @return The decoded {@link SegmentRecomputeEvent}, or {@code null} when the given data is {@code null}.
     */
    @Override
    public SegmentRecomputeEvent decode(final byte[] data) {
        if (Objects.isNull(data)) {
            return null;
        }
        final JsonObject jsonObject = Json.object(data);

        return new SegmentRecomputeEvent(jsonObject.getLong(ORG_ID), jsonObject.getLong(SEGMENT_ID), jsonObject.getLong(ENQUEUED_AT));
    }
}