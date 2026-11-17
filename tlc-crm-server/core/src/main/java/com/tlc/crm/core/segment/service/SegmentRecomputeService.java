package com.tlc.crm.core.segment.service;

import com.tlc.crm.core.segment.service.impl.SegmentRecomputeServiceImpl;

/**
 * <p>
 * Defines the contract for recomputing a segment's contact membership and
 * publishing recompute events.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SegmentRecomputeService {

    /**
     * <p>
     * Gets the {@link SegmentRecomputeService} instance.
     * </p>
     *
     * @return {@link SegmentRecomputeService}
     */
    static SegmentRecomputeService getInstance() {
        return SegmentRecomputeServiceImpl.getInstance();
    }

    /**
     * <p>
     * Recomputes the given segment's matching contacts and reconciles the
     * result against the currently stored membership, then stamps the
     * segment's last-calculated timestamp.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param segmentId Represents the segment id to recalculate.
     */
    void recompute(final long orgId, final long segmentId);

    /**
     * <p>
     * Publishes a segment recompute event for the given segment. The event is
     * silently skipped with a warning log when no producer is available for
     * the topic, and any failure during encoding or publishing is caught and
     * logged rather than propagated.
     * </p>
     *
     * @param orgId     Represents the organization id.
     * @param segmentId Represents the segment id.
     */
    void publish(final long orgId, final long segmentId);
}