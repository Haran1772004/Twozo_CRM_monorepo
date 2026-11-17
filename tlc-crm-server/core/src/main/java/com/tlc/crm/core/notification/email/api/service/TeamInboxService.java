package com.tlc.crm.core.notification.email.api.service;

import com.tlc.crm.core.notification.email.inbox.api.model.EmailVerificationLink;
import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;

import java.util.Collection;

/**
 * <p>
 * Provides methods to interact with Team Inbox email addresses, including retrieval of user-specific emails
 * and sending verification emails.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface TeamInboxService {

    /**
     * <p>
     * Retrieves the collection of email addresses associated with a specific user in an organization.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @param start  Represents the start
     * @param limit  Represents the limit
     * @return A collection of email addresses as {@link String}
     */
    Collection<TeamInbox> getUserTeamInboxList(final Long orgId, final Long userId, final int start, final int limit);

    /**
     * <p>
     * Sends a verification email to the provided email address.
     * </p>
     *
     * @param emailVerificationLink The {@link EmailVerificationLink} to represent the email verification link
     */
    void sendVerificationEmail(final EmailVerificationLink emailVerificationLink);

    /**
     * <p>
     * Returns the auto-forwarding email address configured for the given organization.
     * </p>
     *
     * @param orgId Represents the organization id
     * @return Auto-forwarding email address as {@link String}
     */
    String getAutoForwardingAddress(final Long orgId);
}