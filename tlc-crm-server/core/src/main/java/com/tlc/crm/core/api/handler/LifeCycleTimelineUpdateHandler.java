package com.tlc.crm.core.api.handler;

import com.tlc.crm.core.api.contact.model.Contact;

import java.util.Collection;

/**
 * <p>
 * Handles updates to contact lifecycle timelines based on various lifecycle events
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public interface LifeCycleTimelineUpdateHandler {

    /**
     * <p>
     * Gets the {@link LifeCycleTimelineUpdateHandler} instance
     * </p>
     *
     * @return The {@link LifeCycleTimelineUpdateHandler} instance
     */
    static LifeCycleTimelineUpdateHandler getInstance() {
        return LifeCycleTimelineUpdateHandlerImpl.getInstance();
    }

    /**
     * <p>
     * Updates the lifecycle timeline for contacts based on a deal creation
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param contacts Which contain a {@link Collection} of {@link Contact}
     */
    void updateLifeCycleOnDealWon(final long orgId, final Collection<Contact> contacts);

    /**
     * <p>
     * Updates the lifecycle timeline for contacts based on a deal won
     * </p>
     *
     * @param orgId    Represents an organization id
     * @param contacts Which contain a {@link Collection} of {@link Contact}
     */
    void updateLifeCycleOnDealCreation(final long orgId, final Collection<Contact> contacts);
}
