package com.tlc.crm.entity.timeline.model;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Holds the {@link EntityTimelines} details
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public class EntityTimelines {

    final Map<EntityTimelineType, EntityTimeline> entityTimelines;

    public EntityTimelines() {
        this.entityTimelines = new HashMap<>();
    }

    /**
     * <p>
     * Sets the given entity timeline
     * </p>
     *
     * @param entityTimeline Represents the {@link EntityTimeline}
     */
    public void set(final EntityTimeline entityTimeline) {
        entityTimelines.put(entityTimeline.getType(), entityTimeline);
    }

    /**
     * <p>
     * Gets the entity timeline based on the type
     * </p>
     *
     * @param entityTimelineType Represents the {@link EntityTimeline}
     * @return the {@link EntityTimeline}
     */
    public <T extends EntityTimeline> T get(final EntityTimelineType entityTimelineType) {
        return (T) entityTimelines.get(entityTimelineType);
    }
}
