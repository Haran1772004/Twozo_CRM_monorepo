package com.tlc.crm.core.notification.email.api.service.impl;

import com.tlc.crm.common.email.service.NoReplyMailService;
import com.tlc.crm.common.email.alert.models.EmailNotificationTemplate;
import com.tlc.crm.common.models.CommonFields;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.api.service.TeamInboxService;
import com.tlc.crm.core.notification.email.inbox.api.model.EmailVerificationLink;
import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.core.notification.email.inbox.manager.EmailVerificationLinkManager;
import com.tlc.email.client.template.mail.EmailNotificationBuilder;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;
import com.tlc.notification.model.alert.EmailNotifiableModel;
import com.tlc.notification.service.template.EmailTemplateBuildRequest;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>
 * Implementation of {@link TeamInboxService} that provides functionality to manage
 * user team inbox emails, including retrieving user-specific email addresses and sending verification emails.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class TeamInboxServiceImpl implements TeamInboxService {

    private final EmailNotificationBuilder emailNotificationBuilder;
    private final NoReplyMailService noReplyMailService;
    private final TeamInboxManager teamInboxManager;
    private final EmailVerificationLinkManager emailVerificationLinkManager;

    private TeamInboxServiceImpl() {
        this.emailNotificationBuilder = EmailNotificationBuilder.getInstance();
        this.noReplyMailService = NoReplyMailService.get();
        this.teamInboxManager = TeamInboxManager.getInstance();
        this.emailVerificationLinkManager = EmailVerificationLinkManager.get();
    }

    private static class Instance {
        private static final TeamInboxService INSTANCE = new TeamInboxServiceImpl();
    }

    /**
     * <p>
     * Retrieves the singleton instance of {@link TeamInboxServiceImpl}.
     * </p>
     *
     * @return the {@link TeamInboxServiceImpl} instance
     */
    public static TeamInboxService getInstance() {
        return TeamInboxServiceImpl.Instance.INSTANCE;
    }

    /**
     * <p>
     * Retrieves a paginated list of {@link TeamInbox} objects associated with a specific user in an organization.
     * </p>
     *
     * @param orgId  Represents the organization id
     * @param userId Represents the user id
     * @param start  The starting index for pagination
     * @param limit  The maximum number of {@link TeamInbox} objects to return
     * @return A collection of {@link TeamInbox} objects for the specified user
     */
    public Collection<TeamInbox> getUserTeamInboxList(final Long orgId, final Long userId, final int start, final int limit) {
        return teamInboxManager.getTeamInboxList(orgId, userId, start, limit);
    }

    /**
     * <p>
     * Sends a verification email to the specified recipient address. The email contains
     * a verification link generated using {@link EmailVerificationLink}.
     * </p>
     *
     * @param emailVerificationLink The {@link EmailVerificationLink} to represent the email verification link
     */
    @Override
    public void sendVerificationEmail(final EmailVerificationLink emailVerificationLink) {
        final EmailAddress recipientAddress = emailVerificationLink.getEmailAddress();
        final Map<String, EmailNotifiableModel> emailNotifiableModelMap = new HashMap<>();

        emailNotifiableModelMap.put(CommonFields.MODEL, emailVerificationLink);
        final Optional<Email> optionalVerificationEmail = emailNotificationBuilder
                .build(EmailNotificationTemplate.TEAM_INBOX_VERIFICATION_EMAIL.getName(),
                        new EmailTemplateBuildRequest(emailNotifiableModelMap));

        optionalVerificationEmail.ifPresent(email -> {
            final EmailRecipients emailRecipients = new EmailRecipients();

            emailRecipients.addToRecipient(recipientAddress);
            email.setRecipients(List.of(emailRecipients));

            noReplyMailService.send(email);
            emailVerificationLinkManager.save(emailVerificationLink);
        });
    }

    /**
     * <p>
     * Retrieves the auto-forwarding email address for a specific organization.
     * </p>
     *
     * @param orgId The organization id
     * @return Auto-forwarding email address as {@link String}
     */
    @Override
    public String getAutoForwardingAddress(final Long orgId) {
        return teamInboxManager.fetchAutoForwardingAddress(orgId);
    }
}