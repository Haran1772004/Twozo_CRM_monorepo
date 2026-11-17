package com.tlc.crm.email.core.handler;

import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.notification.email.api.manager.addon.team.TeamInboxManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.TeamInboxEmail;
import com.tlc.crm.core.notification.email.inbox.api.model.TeamInbox;
import com.tlc.crm.email.core.InboundEmailLinkerService;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.service.AutoForwardingEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * <p>
 * Handles the loading of Team Inbox related email threads for a given {@link ThreadHolder}.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class TeamInboxDataLoadActionHandler extends AbstractEmailDataLoadActionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeamInboxDataLoadActionHandler.class);

    private static final String ANGLE_BRACKETS_REGEX = "[<>]";
    private static final String EMPTY_STRING = "";

    private final TeamInboxManager teamInboxManager;
    private final InboundEmailLinkerService inboundEmailLinkerService;
    private final ContactManager contactManager;
    private final AutoForwardingEmailService autoForwardingEmailService;

    private TeamInboxDataLoadActionHandler() {
        this.teamInboxManager = TeamInboxManager.getInstance();
        this.inboundEmailLinkerService = InboundEmailLinkerService.getInstance();
        this.contactManager = ContactManager.getInstance();
        this.autoForwardingEmailService = AutoForwardingEmailService.getInstance();
    }

    private static class Instance {
        private static final EmailAddOnServiceDataLoadActionHandler INSTANCE = new TeamInboxDataLoadActionHandler();
    }

    /**
     * <p>
     * Returns the singleton instance of {@link TeamInboxDataLoadActionHandler}.
     * </p>
     *
     * @return singleton instance
     */
    public static EmailAddOnServiceDataLoadActionHandler getInstance() {
        return TeamInboxDataLoadActionHandler.Instance.INSTANCE;
    }

    /**
     * <p>
     * Returns a new {@link TeamInboxEmail} with message category set to INBOUND.
     * </p>
     *
     * @return a new {@link EntityEmail} instance
     */
    protected EntityEmail getEmail() {
        final EntityEmail entityEmail = new TeamInboxEmail();

        entityEmail.setMessageCategory(Email.MessageCategory.INBOUND);

        return entityEmail;
    }

    /**
     * <p>
     * Processes the threads contained in the given {@link ThreadHolder}.
     * This method is invoked as part of the email data load action for Team Inbox.
     * </p>
     *
     * @param threadHolder The {@link ThreadHolder} containing threads to handle
     */
    @Override
    public void handle(final ThreadHolder threadHolder) {
        final Collection<Thread> threads = threadHolder.getThreads();
        final Collection<String> senderAddresses = threads.stream()
                .flatMap(thread -> thread.getEmails().stream())
                .map(Email::getMetaData)
                .filter(Objects::nonNull)
                .map(EmailMetaData::getSenderId)
                .filter(Objects::nonNull)
                .toList();
        final Collection<String> extractedAddresses = extractEmails(senderAddresses);

        final Map<String, List<TeamInbox>> teamInboxes = senderAddresses.isEmpty() ? Collections.emptyMap() :
                teamInboxManager.getTeamInbox(extractedAddresses);

        threads.forEach(thread -> {
            thread.getEmails().forEach(email -> {
                final Optional<String> optionalSenderAddress = extractEmail(email.getMetaData().getSenderId());

                if (optionalSenderAddress.isPresent()) {
                    loadEmail(teamInboxes.get(optionalSenderAddress.get()), thread, email);
                    return;
                }
                final Collection<TeamInbox> verificationMailTeamInboxes = getTeamInboxes(email);

                if (!verificationMailTeamInboxes.isEmpty()) {
                    loadEmail(verificationMailTeamInboxes, thread, email);
                }
            });
        });
    }

    /**
     * <p>
     * Retrieves the list of {@link TeamInbox} instances associated with the given {@link Email}.
     * </p>
     *
     * @param email The {@link Email} to evaluate for Team Inbox association
     * @return A collection of {@link TeamInbox} instances that match the given email,
     * or an empty collection if no valid match is found
     */
    private Collection<TeamInbox> getTeamInboxes(final Email email) {
        final String autoForwardedAddress = email.getMetaData().getAutoForwardingAddress();

        if (Objects.isNull(autoForwardedAddress) || autoForwardedAddress.isEmpty() ||
                !autoForwardingEmailService.isValidForwardingSource(email.getSender().getEmailAddress())) {
            return Collections.emptyList();
        }
        final String currentMailBoxId = email.getMetaData().getCurrentMailboxId();

        final Collection<TeamInbox> teamInboxList = teamInboxManager.getTeamInbox(List.of(autoForwardedAddress))
                .get(autoForwardedAddress);
        final Collection<TeamInbox> teamInboxes = new HashSet<>();

        teamInboxList.forEach(recipientTeamInbox -> {
            final long orgId = recipientTeamInbox.orgId();

            secureProcess(orgId, recipientTeamInbox.getOwnerId(), () -> {
                final String autoForwardingAddress = teamInboxManager.fetchAutoForwardingAddress(orgId);

                if (Objects.equals(autoForwardingAddress, currentMailBoxId)) {
                    teamInboxes.add(recipientTeamInbox);
                }
            });
        });

        return teamInboxes;
    }

    /**
     * <p>
     * Loads and persists the given {@link Email} into all matching {@link TeamInbox} instances.
     * </p>
     *
     * @param teamInboxes The collection of {@link TeamInbox} instances where the email should be loaded
     * @param thread      The {@link Thread} containing the email to be processed
     * @param email       The {@link Email} to load into the Team Inbox
     */
    private void loadEmail(final Collection<TeamInbox> teamInboxes, final Thread thread, final Email email) {
        if (Objects.isNull(teamInboxes)) {
            LOGGER.warn("Team inbox: There is no active team inbox is present");
        } else {
            LOGGER.debug("Team inbox: Going to load the email for the team inboxes of size: {}", teamInboxes.size());

            for (final TeamInbox teamInbox : teamInboxes) {
                final Long orgId = teamInbox.orgId();
                final Long userId = teamInbox.getOwnerId();

                secureProcess(orgId, userId, () -> {

                    if (teamInbox.getVerificationState().isVerified()) {
                        final EntityThread entityThread = composeThread(orgId, teamInbox.getOwnerId(), thread.id(), email);
                        final EntityEmail entityEmail = entityThread.getComposedEmail();

                        if (entityEmail instanceof TeamInboxEmail teamInboxEmail) {
                            teamInboxEmail.setTeamInboxId(teamInbox.id());
                        }
                        loadRecipients(email, entityEmail);
                        preProcess(entityThread);
                        inboundEmailLinkerService.createThread(entityThread);
                    }
                });
            }
        }
    }

    /**
     * <p>
     * Loads and maps recipients from the given {@link Email} into the provided {@link EntityEmail}.
     * </p>
     *
     * @param email       The source {@link Email} to extract recipients from
     * @param entityEmail The target {@link EntityEmail} to populate recipients
     */
    protected void loadRecipients(final Email email, final EntityEmail entityEmail) {
        final EmailRecipients emailRecipients = email.getEmailRecipients();

        if (Objects.nonNull(emailRecipients)) {
            final Map<String, Contact> contacts = contactManager.fetchContactsByEmails(entityEmail.orgId(), emailRecipients.getAllEmailAddresses());
            final Consumer<AddressType> contactRecipientsConsumer = (addressType) -> {
                final Map<AddressType, Collection<EmailAddress>> addressMap = emailRecipients.get();

                if (Objects.nonNull(addressMap)) {
                    final Collection<EmailAddress> emailAddresses = addressMap.get(addressType);

                    if (Objects.nonNull(emailAddresses) && !emailAddresses.isEmpty()) {
                        final Collection<String> addresses = emailAddresses.stream()
                                .map(EmailAddress::getEmailAddress)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toSet());
                        final ContactRecipients contactRecipients = new ContactRecipients();

                        for (final String address : addresses) {
                            if (contacts.containsKey(address)) {
                                contactRecipients.addContact(addressType, contacts.get(address));
                            }
                        }
                        entityEmail.setContactRecipients(contactRecipients);
                    }
                }
            };
            contactRecipientsConsumer.accept(AddressType.TO);
            contactRecipientsConsumer.accept(AddressType.BCC);
            contactRecipientsConsumer.accept(AddressType.CC);

            entityEmail.setRecipients(List.of(emailRecipients));
        }
    }

    /**
     * <p>
     * Cleans a collection of unformatted email strings by removing angle brackets and extra spaces.
     * </p>
     *
     * @param emails the collection of unformatted email strings
     * @return a collection of cleaned email addresses
     */
    private Collection<String> extractEmails(Collection<String> emails) {
        if (Objects.isNull(emails) || emails.isEmpty()) {
            return Collections.emptyList();
        }

        return emails.stream()
                .filter(Objects::nonNull)
                .map(path -> path.replaceAll(ANGLE_BRACKETS_REGEX, EMPTY_STRING).trim())
                .toList();
    }

    /**
     * <p>
     * Extracts a single email address from a Return-Path string and returns it as an {@link Optional}.
     * </p>
     *
     * @param email the unformatted email string
     * @return an {@link Optional} containing the cleaned email address, or empty if invalid
     */
    private Optional<String> extractEmail(final String email) {
        return extractEmails(Collections.singletonList(email))
                .stream()
                .findFirst();
    }
}