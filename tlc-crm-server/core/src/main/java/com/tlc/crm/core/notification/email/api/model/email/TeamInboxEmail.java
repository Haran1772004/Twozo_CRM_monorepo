package com.tlc.crm.core.notification.email.api.model.email;

/**
 * <p>
 * Represents an email associated with a Team Inbox. Extends {@link EntityEmail} to include
 * Team Inbox specific information such as the Team Inbox ID.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class TeamInboxEmail extends EntityEmail {

    /**
     * <p>
     * Stores the ID of the Team Inbox to which this email belongs.
     * </p>
     */
    private Long teamInboxId;

    /**
     * <p>
     * Default constructor for {@link TeamInboxEmail}.
     * </p>
     */
    public TeamInboxEmail() {
        this.setVariant(EmailVariant.of(EmailVariant.Type.TEAM_INBOX));
    }

    /**
     * <p>
     * Returns the Team Inbox ID associated with this email.
     * </p>
     *
     * @return the Team Inbox ID
     */
    public Long getTeamInboxId() {
        return teamInboxId;
    }

    /**
     * <p>
     * Sets the Team Inbox ID for this email.
     * </p>
     *
     * @param teamInboxId the Team Inbox ID to set
     */
    public void setTeamInboxId(final Long teamInboxId) {
        this.teamInboxId = teamInboxId;
    }
}