package com.tlc.crm.core.notification.email.timeline.impl;

import com.tlc.crm.core.notification.email.timeline.EmailTimelineManager;

import com.tlc.crm.entity.timeline.model.EntityTimeline;
import com.tlc.crm.entity.timeline.model.EntityTimelines;
import com.tlc.crm.entity.timeline.model.Timeline;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Implements the timeline to engage email timeline related activities.
 * </p>
 *
 * @author Ajay
 * @version 1.0
 */
public final class EmailTimelineProvider {

    private final EmailTimelineManager emailTimelineManager;

    public EmailTimelineProvider() {
        emailTimelineManager = EmailTimelineManager.getInstance();
    }

    private static class Instance {
        private static final EmailTimelineProvider INSTANCE = new EmailTimelineProvider();
    }

    /**
     * <p>
     * Gets the {@link EmailTimelineProvider} Instance
     * </p>
     *
     * @return {@link EmailTimelineProvider}
     */
    public static EmailTimelineProvider getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * <p>
     * Gets {@link Collection} of {@link Timeline}.
     * </p>
     *
     * @param orgId Represents the organization's id.
     * @param ids   Holds {@link Collection} of timeline ids.
     * @return {@link Collection} of {@link EntityTimeline}.
     */
    public Map<Long, EntityTimelines> get(final long orgId, final Collection<Long> ids) {
        return emailTimelineManager.get(orgId, ids);
    }
}
