package com.tlc.crm.core.segment.manager;

import com.tlc.crm.core.segment.manager.impl.SegmentContactManagerImpl;

import com.tlc.crm.common.models.LimitConfig;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Provides the segment to contact mapping related DB actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface SegmentContactManager {

    static SegmentContactManager getInstance() {
        return SegmentContactManagerImpl.getInstance();
    }

    /**
     * <p>
     * Upserts a page of contact ids for the given segment and sets {@code LAST_RECALCULATED_AT}
     * to the supplied timestamp. Intended to be called per-page during recalculation.
     * </p>
     *
     * @param orgId        Represents the organization id.
     * @param segmentId    Represents the segment id.
     * @param contactIds   Represents the contact ids to upsert.
     * @param runStartedAt Represents the recalculation run start timestamp.
     */
    void upsertContactsWithLastRecalculatedAt(final Long orgId, final Long segmentId,
                                              final Collection<Long> contactIds, final long runStartedAt);

    /**
     * <p>
     * Deletes the stale mappings for the segment where {@code LAST_RECALCULATED_AT} is less than
     * the given run start timestamp, or is {@code null}.
     * </p>
     *
     * @param orgId        Represents the organization id.
     * @param segmentId    Represents the segment id.
     * @param runStartedAt Represents the recalculation run start timestamp.
     */
    void deleteStaleMappings(final Long orgId, final Long segmentId, final long runStartedAt);

    /**
     * <p>
     * Gets contact counts for the given segment ids in a single query.
     * </p>
     *
     * @param orgId      Represents the organization id.
     * @param segmentIds Represents the segment ids.
     * @return A map of segment id to contact count.
     */
    Map<Long, Long> getContactCountMap(final Long orgId, final Collection<Long> segmentIds);

    /**
     * <p>
     * Gets contact ids mapped to the segment, optionally excluding ids and applying a limit.
     * </p>
     *
     * @param orgId              organization id
     * @param segmentId          segment id
     * @param excludedContactIds contact ids to exclude
     * @param limitConfig        pagination limit, or {@code null} for all matching ids
     * @return contact ids in the segment
     */
    Collection<Long> getContactIds(final Long orgId, final Long segmentId, final Collection<Long> excludedContactIds,
                                   final LimitConfig limitConfig);
}