package com.tlc.crm.core.notification.email.timeline;

import com.tlc.crm.core.notification.email.timeline.impl.EmailTimelineManagerImpl;

import com.tlc.crm.entity.timeline.model.EntityTimelines;
import com.tlc.crm.entity.timeline.model.Timeline;

import com.tlc.sql.common.api.dml.Table;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Performs email - timeline DB related actions
 * </p>
 *
 * @author Kavinila E
 * @version 1.0
 */
public interface EmailTimelineManager {

    /**
     * <p>
     * Gets the instance of the {@link EmailTimelineManager}
     * </p>
     *
     * @return The {@link EmailTimelineManagerImpl} instance
     */
    static EmailTimelineManager getInstance() {
        return EmailTimelineManagerImpl.getInstance();
    }

    /**
     * <p>
     * Gets {@link Collection} of {@link Timeline}
     * </p>
     *
     * @param orgId Represents an organization's id
     * @param ids   Holds {@link Collection} of ids
     * @return {@link Map} of {@link Long} timeline id  {@link EntityTimelines}
     */
    Map<Long, EntityTimelines> get(final long orgId, final Collection<Long> ids);

    /**
     * <p>
     * Gets the email timeline table
     * </p>
     *
     * @return The {@link Table} of email timeline
     */
    Table getTable();
}
