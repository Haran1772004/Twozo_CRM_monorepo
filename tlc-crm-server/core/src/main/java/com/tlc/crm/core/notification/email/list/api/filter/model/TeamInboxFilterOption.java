package com.tlc.crm.core.notification.email.list.api.filter.model;

import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.list.api.filter.FilterOption;

import java.util.Collection;

/**
 * <p>
 * Represents a collection of team inboxes used for filtering.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class TeamInboxFilterOption implements FilterOption {

    private Collection<TeamInbox> teamInboxes;

    /**
     * Initializes a new {@link TeamInboxFilterOption} with the given collection of team inboxes.
     *
     * @param teamInboxes a collection of {@link TeamInbox} objects
     */
    public TeamInboxFilterOption(final Collection<TeamInbox> teamInboxes) {
        this.teamInboxes = teamInboxes;
    }

    /**
     * Returns the collection of team inboxes.
     *
     * @return a {@link Collection} of {@link TeamInbox} objects
     */
    public Collection<TeamInbox> getTeamInboxes() {
        return teamInboxes;
    }

    /**
     * Sets the collection of team inboxes.
     *
     * @param teamInboxes a {@link Collection} of {@link TeamInbox} objects to set
     */
    public void setTeamInboxes(final Collection<TeamInbox> teamInboxes) {
        this.teamInboxes = teamInboxes;
    }
}