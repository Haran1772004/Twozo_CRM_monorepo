package com.tlc.crm.core.notification.email.list.api.filter.model;

import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;

import java.util.Collection;

/**
 * <p>
 * Represents a collection of synced email ids used for filtering.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class SyncEmailFilterOption implements FilterOption {

    private Collection<Long> syncedEmailIds;

    /**
     * Initializes a new {@link SyncEmailFilterOption} with the given collection of synced email ids.
     *
     * @param syncedEmailIds a collection of {@link Long} objects
     */
    public SyncEmailFilterOption(final Collection<Long> syncedEmailIds) {
        this.syncedEmailIds = syncedEmailIds;
    }

    /**
     * Returns the collection of synced email ids.
     *
     * @return a {@link Collection} of {@link Long} objects
     */
    public Collection<Long> getSyncedEmailIds() {
        return syncedEmailIds;
    }

    /**
     * Sets the collection of synced email ids.
     *
     * @param syncedEmailIds a {@link Collection} of {@link Long} objects to set
     */
    public void setSyncedEmailIds(final Collection<Long> syncedEmailIds) {
        this.syncedEmailIds = syncedEmailIds;
    }
}