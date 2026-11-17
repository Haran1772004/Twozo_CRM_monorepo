package com.tlc.crm.core.segment.models;

/**
 * <p>
 * Simple model representing a segment recompute event payload.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class SegmentRecomputeEvent {

    private final long orgId;
    private final long segmentId;
    private final long enqueuedAt;

    /**
     * <p>
     * Constructs a {@link SegmentRecomputeEvent} with the given details.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param segmentId  Represents the segment id.
     * @param enqueuedAt Represents the epoch millisecond timestamp when the event was enqueued.
     */
    public SegmentRecomputeEvent(final long orgId, final long segmentId, final long enqueuedAt) {
        this.orgId = orgId;
        this.segmentId = segmentId;
        this.enqueuedAt = enqueuedAt;
    }

    /**
     * <p>
     * Gets the organization id.
     * </p>
     *
     * @return The organization id.
     */
    public long getOrgId() {
        return orgId;
    }

    /**
     * <p>
     * Gets the segment id.
     * </p>
     *
     * @return The segment id.
     */
    public long getSegmentId() {
        return segmentId;
    }

    /**
     * <p>
     * Gets the epoch millisecond timestamp when the event was enqueued.
     * </p>
     *
     * @return The enqueued-at timestamp.
     */
    public long getEnqueuedAt() {
        return enqueuedAt;
    }
}