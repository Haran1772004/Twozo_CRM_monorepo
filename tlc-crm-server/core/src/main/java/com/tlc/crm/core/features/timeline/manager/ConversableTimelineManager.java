package com.tlc.crm.core.features.timeline.manager;

import com.tlc.crm.entity.timeline.model.EntityTimeline;
import com.tlc.crm.entity.timeline.model.TimelineManager;

import java.util.Collection;
import java.util.Map;

/**
 * Contract for managing conversable timelines
 *
 * @author kishore J
 * @version 1.0
 */
public interface ConversableTimelineManager extends TimelineManager {

    /**
     * <p>
     * Fetches {@link EntityTimeline} for the given organization id and timeline ids
     * </p>
     *
     * @param orgId       Represents an organization id
     * @param timelineIds Which contains the collection of timeline ids
     * @return A {@link Map} of timeline ID to its corresponding {@link EntityTimeline}
     */
    Map<Long, EntityTimeline> get(final long orgId, final Collection<Long> timelineIds);
}
