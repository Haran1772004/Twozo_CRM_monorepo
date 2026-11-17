package com.tlc.email.server.internal.handler;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.JavaUtil;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailDeleteEvent;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.EmailUpdateEvent;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.config.FolderInterval;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.request.EmailEventRequest;
import com.tlc.email.common.service.EmailTrackingService;
import com.tlc.email.server.internal.manager.EmailManager;
import com.tlc.email.server.internal.manager.MailBoxConfManager;
import com.tlc.email.server.internal.manager.ReferenceManager;
import com.tlc.email.server.internal.manager.ThreadManager;
import com.tlc.email.server.internal.protocols.imap.ImapClient;
import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;
import com.tlc.email.server.internal.protocols.models.MessageFlagsInfo;
import com.tlc.email.server.internal.provider.jakarta.models.EmailFolder;
import com.tlc.email.server.internal.provider.jakarta.provider.JakartaImapClientFactory;
import com.tlc.email.server.internal.sync.AbstractSyncActionHandler;
import com.tlc.email.server.model.EmailProviderDataStore;
import com.tlc.email.server.model.sync.SyncContext;
import com.tlc.email.server.service.EmailProviderService;
import jakarta.mail.Folder;
import jakarta.mail.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * <p>
 * Base handler for executing automatic email synchronization actions.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public abstract class AbstractAutoSyncActionHandler extends AbstractSyncActionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAutoSyncActionHandler.class);

    private final MailBoxConfManager mailBoxConfManager;
    private final JakartaImapClientFactory jakartaImapConnectionHelper;
    private final ThreadManager threadManager;
    private final EmailTrackingService emailTrackingService;
    private final ReferenceManager referenceManager;
    private final EmailManager emailManager;

    protected AbstractAutoSyncActionHandler() {
        this.mailBoxConfManager = MailBoxConfManager.getInstance();
        this.jakartaImapConnectionHelper = JakartaImapClientFactory.getInstance();
        this.threadManager = ThreadManager.getInstance();
        this.emailTrackingService = EmailTrackingService.getInstance();
        this.referenceManager = ReferenceManager.getInstance();
        this.emailManager = EmailManager.getInstance();
    }

    /**
     * <p>
     * Handles the auto-sync process for the specified email address and folder intervals.
     * </p>
     *
     * @param emailAddress    The {@link SyncAddressInfo} representing the email address to sync
     * @param folderIntervals The collection of {@link FolderInterval} specifying folders and intervals to sync
     */
    public void handle(final EmailAddress emailAddress, final Collection<FolderInterval> folderIntervals) {
        final EmailReceiverConfiguration emailReceiverConfiguration = new EmailReceiverConfiguration();
        final ImapClient imapClient = get(emailAddress);
        final SyncAddressInfo syncAddressInfo = new SyncAddressInfo(emailAddress.getEmailAddress());

        syncAddressInfo.setId(emailAddress.id());
        emailReceiverConfiguration.setIncludeMessageIds(true);
        emailReceiverConfiguration.setSyncEmailAddress(syncAddressInfo);

        folderIntervals.forEach(folderInterval -> {
            final FolderType folderType = folderInterval.getFolder();
            final EmailProviderService emailProviderService = EmailProviderService.getService(emailAddress);
            final Email.MessageCategory messageCategory = Email.MessageCategory.getMessageCategory(folderType);

            emailReceiverConfiguration.setFolderTypes(List.of(folderType));

            folderInterval.getEmailIntervals().forEach(emailInterval -> {
                emailReceiverConfiguration.setEmailIntervals(Map.of(folderType, emailInterval));
                EmailProviderDataStore emailProviderDataStore = null;

                try {
                    emailProviderDataStore = emailProviderService.receive(emailReceiverConfiguration);
                    final Long emailId = emailAddress.id();

                    if (Objects.nonNull(emailProviderDataStore)) {
                        final Collection<String> currentProviderMessageIds =
                                imapClient.getMessageIds(emailProviderDataStore.getFolder(), get(folderType), emailInterval);

                        processFolderSync(
                                emailAddress, imapClient, folderType, currentProviderMessageIds,
                                emailReceiverConfiguration, emailManager.getMessageIds(emailId, currentProviderMessageIds)
                        );
                        handleMessages(emailId, syncAddressInfo, imapClient, emailManager.getMessageIds(emailId, messageCategory, currentProviderMessageIds));
                    }
                } catch (Exception exception) {
                    LoggerUtil.logError(LOGGER, exception, "Auto sync: Action failed, message: {}, trace: {}", exception.getMessage(), exception);

                    throw ErrorCode.get(EmailErrorCodes.AUTO_SYNC_ACTION_FAILED);
                } finally {
                    cleanUp(emailProviderDataStore);
                }
            });
        });
    }

    /**
     * <p>
     * Handles a collection of email messages by updating modified threads
     * and collecting deleted message IDs.
     * </p>
     *
     * @param emailId         The unique sync operation ID
     * @param syncAddressInfo The {@link SyncAddressInfo} representing the email address
     * @param imapClient      The {@link ImapClient} used to fetch message details
     * @param messageIds      The collection of provider message IDs to process
     */
    private void handleMessages(final Long emailId, final SyncAddressInfo syncAddressInfo,
                                final ImapClient imapClient, final Collection<String> messageIds) {
        final Collection<Long> deletedMessages = new HashSet<>();
        final Collection<Thread> modifiedMessages = new HashSet<>();

        for (final String messageId : messageIds) {
            final Email email = imapClient.getMessage(messageId);

            if (Objects.isNull(email)) {
                deletedMessages.add(emailManager.getConversationId(messageId));
            } else {
                final Collection<Thread> threads = getExistingThreads(emailId, messageId);

                if (Objects.nonNull(threads) && !threads.isEmpty()) {
                    final Thread thread = threads.stream().findFirst().get();

                    thread.getComposeEmail().ifPresent(existingEmail -> {
                        boolean modified = false;

                        if (!Objects.equals(existingEmail.isActive(), email.isActive())) {
                            modified = true;
                            existingEmail.isActive(email.isActive());
                        }

                        if (!Objects.equals(existingEmail.isArchived(), email.isArchived())) {
                            modified = true;
                            existingEmail.setArchived(email.isArchived());
                        }

                        if (modified) {
                            modifiedMessages.add(thread);
                        }
                    });
                }
            }
        }
        handleModifiedMails(syncAddressInfo, modifiedMessages, deletedMessages);
    }

    /**
     * <p>
     * Processes a specific folder by detecting new messages, existing messages,
     * and triggering the appropriate create/update flows.
     * </p>
     *
     * @param emailAddress               Represents te email address.
     * @param imapClient                 The {@link ImapClient} for fetching messages
     * @param folderType                 The {@link FolderType} being processed
     * @param providerMessageIds         The collection of message IDs from the provider
     * @param emailReceiverConfiguration The configuration for the receiver
     * @param localMessageIds            The collection of locally persisted message IDs
     */
    private void processFolderSync(final EmailAddress emailAddress, final ImapClient imapClient,
                                   final FolderType folderType, final Collection<String> providerMessageIds,
                                   final EmailReceiverConfiguration emailReceiverConfiguration, final Collection<String> localMessageIds) {
        final Collection<String> newMessageIds = new ArrayList<>(providerMessageIds);
        final Collection<String> existingMessageIds = new ArrayList<>(providerMessageIds);

        newMessageIds.removeAll(localMessageIds);
        existingMessageIds.retainAll(localMessageIds);

        if (!existingMessageIds.isEmpty()) {
            final Long emailId = emailAddress.id();

            if (folderType == FolderType.DRAFT) {
                processDraftUpdates(emailAddress, imapClient, folderType, emailReceiverConfiguration, existingMessageIds);
            } else {
                processUpdatedMessages(emailId, emailReceiverConfiguration.getSyncEmailAddress(), imapClient, folderType, existingMessageIds);
            }
        }

        if (!newMessageIds.isEmpty()) {
            processNewMessages(emailAddress, imapClient, folderType, emailReceiverConfiguration, newMessageIds);
        }
    }

    /**
     * <p>
     * Loads sender information and tracking IDs for messages in the SENT folder.
     * </p>
     *
     * @param emailId         The internal ID for the synced email
     * @param syncAddressInfo The {@link SyncAddressInfo} representing the sender
     * @param folderType      The {@link FolderType} of the emails
     * @param emails          The collection of {@link Email} messages to update
     */
    private void loadMessage(final Long emailId, final SyncAddressInfo syncAddressInfo,
                             final FolderType folderType, final Collection<Email> emails) {
        if (!Objects.equals(FolderType.SENT, folderType)) {
            return;
        }

        for (final Email email : emails) {
            final SyncAddressInfo addressInfo = email.getSender();

            if (Objects.isNull(addressInfo)) {
                email.setSender(syncAddressInfo);
            }
            loadTrackingId(emailId, email);
        }
    }


    /**
     * <p>
     * Assigns the sender information to a collection of emails.
     * </p>
     *
     * @param syncAddressInfo The {@link SyncAddressInfo} to assign
     * @param emails          The collection of {@link Email} messages
     */
    private void loadSenderInfo(final SyncAddressInfo syncAddressInfo, final Collection<Email> emails) {
        if (Objects.isNull(emails) || emails.isEmpty()) {
            return;
        }

        for (final Email email : emails) {
            email.setSender(syncAddressInfo);
        }
    }

    /**
     * <p>
     * Extracts the tracking ID from an {@link Email} content and updates
     * its metadata with the resolved message ID and tracking ID.
     * </p>
     *
     * @param emailId The internal synced email ID
     * @param email   The {@link Email} whose metadata will be updated
     */
    private void loadTrackingId(final Long emailId, final Email email) {
        final Optional<String> optionalTrackingId = emailTrackingService.getTrackingId(email.getContent());

        optionalTrackingId.ifPresent(trackingId -> {
            referenceManager.getMessageIdByTrackingId(emailId, trackingId)
                    .ifPresent(messageId -> {
                        final EmailMetaData metaData = email.getMetaData();

                        metaData.setMessageId(messageId);
                        metaData.setTrackingId(trackingId);
                    });
        });
    }

    /**
     * <p>
     * Processes new messages by fetching them from the email provider and creating them locally.
     * </p>
     *
     * @param emailAddress               The internal ID for the current sync operation
     * @param imapClient                 The {@link ImapClient} used to fetch messages from the provider
     * @param folderType                 The {@link FolderType} indicating which folder the messages belong to
     * @param emailReceiverConfiguration The {@link EmailReceiverConfiguration} containing sync-specific configuration
     * @param messageIds                 A collection of provider message IDs to fetch and process
     */
    private void processNewMessages(final EmailAddress emailAddress, final ImapClient imapClient, final FolderType folderType,
                                    final EmailReceiverConfiguration emailReceiverConfiguration, final Collection<String> messageIds) {
        for (final Collection<String> batch : JavaUtil.splitCollection(messageIds, 10)) {
            final Collection<Email> newEmails = imapClient.getMessages(get(folderType), batch);
            final Long emailId = emailAddress.id();

            if (Objects.nonNull(newEmails) && !newEmails.isEmpty()) {
                loadMessage(emailId, emailReceiverConfiguration.getSyncEmailAddress(), folderType, newEmails);
                loadEmails(emailAddress, emailReceiverConfiguration, SyncJobType.AUTO_SYNC,
                        new SyncContext(emailId, emailReceiverConfiguration.getSyncEmailAddress()), newEmails);
            }
        }
    }

    /**
     * <p>
     * Processes updated messages by checking for changes in email flags such as read, starred, and archived.
     * </p>
     *
     * @param emailId         The internal ID for the current sync operation
     * @param syncAddressInfo The {@link SyncAddressInfo} representing the email account being synced
     * @param imapClient      The {@link ImapClient} used to fetch message flag information from the provider
     * @param folderType      The {@link FolderType} indicating the folder of the messages
     * @param messageIds      A collection of provider message IDs that need to be checked for updates
     */
    private void processUpdatedMessages(final Long emailId, final SyncAddressInfo syncAddressInfo,
                                        final ImapClient imapClient, final FolderType folderType,
                                        final Collection<String> messageIds) {
        for (final Collection<String> batchMessages : JavaUtil.splitCollection(messageIds, 10)) {
            final Collection<Thread> modifiedMessages = new HashSet<>();

            for (final String messageId : batchMessages) {
                try {
                    final MessageFlagsInfo providerFlags = imapClient.getLabelInfo(get(folderType), messageId);
                    final Collection<Thread> threads = getExistingThreads(emailId, messageId);

                    if (Objects.nonNull(threads) && !threads.isEmpty()) {
                        final Thread thread = threads.stream().findFirst().get();

                        thread.getComposeEmail().ifPresent(email -> {
                            if (hasFlagsChanged(email, providerFlags, folderType)) {
                                updateEmailFlags(email, providerFlags, folderType);
                                modifiedMessages.add(thread);
                            }
                        });
                    }
                } catch (Exception exception) {
                    LoggerUtil.logError(LOGGER, exception, "Auto sync: Processing updated messages action failed, message: {}, trace: {}", exception.getMessage(), exception);

                    throw ErrorCode.get(EmailErrorCodes.AUTO_SYNC_ACTION_FAILED);
                }
            }
            handleModifiedMails(syncAddressInfo, modifiedMessages, null);
        }
    }

    /**
     * <p>
     * Processes modified and deleted emails for a specific email account during sync.
     * </p>
     *
     * @param syncAddressInfo   The {@link SyncAddressInfo} representing the email account being synced.
     * @param modifiedThreads   A collection of {@link Thread} instances whose emails were modified.
     * @param deletedMessageIds A collection of internal message IDs representing emails
     *                          that have been deleted.
     */
    private void handleModifiedMails(final SyncAddressInfo syncAddressInfo,
                                     final Collection<Thread> modifiedThreads, final Collection<Long> deletedMessageIds) {
        if (Objects.nonNull(modifiedThreads) && !modifiedThreads.isEmpty()) {
            threadManager.updateThreadLabels(modifiedThreads);
        }

        if (Objects.nonNull(deletedMessageIds) && !deletedMessageIds.isEmpty()) {
            threadManager.deleteMessages(deletedMessageIds);
        }
        publishModifiedMails(syncAddressInfo, modifiedThreads, deletedMessageIds);
    }

    /**
     * <p>
     * Publishes changes to emails via webhooks. This includes updated threads and
     * deleted messages.
     * </p>
     *
     * @param syncAddressInfo   The {@link SyncAddressInfo} representing the synced email account.
     * @param threads           A collection of {@link Thread} instances that have been modified.
     * @param deletedMessageIds A collection of internal message IDs representing deleted messages.
     */
    private void publishModifiedMails(final SyncAddressInfo syncAddressInfo,
                                      final Collection<Thread> threads,
                                      final Collection<Long> deletedMessageIds) {
        final boolean hasToBeUpdated = Objects.nonNull(threads) && !threads.isEmpty();
        final boolean hasToBeDeleted = Objects.nonNull(deletedMessageIds) && !deletedMessageIds.isEmpty();

        if (!hasToBeUpdated && !hasToBeDeleted) {
            return;
        }
        final EmailEventRequest emailEventRequest = new EmailEventRequest();
        final EmailReceiverConfiguration emailReceiverConfiguration = new EmailReceiverConfiguration();

        emailReceiverConfiguration.setSyncEmailAddress(syncAddressInfo);
        emailEventRequest.setReceiverConfiguration(emailReceiverConfiguration);

        if (hasToBeUpdated) {
            final ThreadHolder threadHolder = new ThreadHolder();

            threadHolder.setSyncedEmailAddress(syncAddressInfo);
            threadHolder.setThreads(threads);
            emailEventRequest.setUpdateEvent(new EmailUpdateEvent(threadHolder));
        }

        if (hasToBeDeleted) {
            emailEventRequest.setDeleteEvent(new EmailDeleteEvent(deletedMessageIds));
        }
        emailEventRequestPublisher.publish(emailEventRequest);
    }

    /**
     * <p>
     * Checks whether local email flags differ from provider flags.
     * </p>
     *
     * @param localEmail    The local {@link Email}
     * @param providerFlags The {@link MessageFlagsInfo} from the provider
     * @param folderType    The {@link FolderType} to determine category
     * @return {@code true} if flags differ; {@code false} otherwise
     */
    private boolean hasFlagsChanged(final Email localEmail, final MessageFlagsInfo providerFlags, final FolderType folderType) {
        if (localEmail.isRead() != providerFlags.isSeen()) {
            return true;
        }

        if (localEmail.isStarred() != providerFlags.isFlagged()) {
            return true;
        }
        final Email.MessageCategory category = Email.MessageCategory.getMessageCategory(folderType);
        boolean providerArchived = category == Email.MessageCategory.ARCHIVE;

        return localEmail.isArchived() != providerArchived || !localEmail.isActive();
    }

    /**
     * <p>
     * Updates local email flags based on the flags provided by the email provider.
     * </p>
     *
     * @param email         The {@link Email} object whose flags should be updated
     * @param providerFlags The {@link MessageFlagsInfo} containing flag information from the provider
     * @param folderType    The {@link FolderType} of the email to determine archival status
     */
    private void updateEmailFlags(final Email email, final MessageFlagsInfo providerFlags, final FolderType folderType) {
        email.setRead(providerFlags.isSeen());
        email.setStarred(providerFlags.isFlagged());
        email.setArchived(folderType.equals(FolderType.ARCHIVE));
        email.isActive(true);
    }

    /**
     * <p>
     * Maps a {@link FolderType} to the corresponding {@link EmailFolder} used
     * by the provider implementation.
     * </p>
     *
     * @param folderType The {@link FolderType} to map
     * @return The corresponding {@link EmailFolder} used for IMAP operations
     */
    private EmailFolder get(final FolderType folderType) {
        return switch (folderType) {
            case INBOX -> EmailFolder.INBOX;
            case SENT -> EmailFolder.SENT;
            case DRAFT -> EmailFolder.DRAFT;
            case ARCHIVE -> EmailFolder.ARCHIVE;
        };
    }

    /**
     * <p>
     * Retrieves an {@link ImapClient} for the given email address by looking up
     * the persisted {@link MailBoxConfiguration} and establishing a connection
     * using {@link JakartaImapClientFactory}.
     * </p>
     *
     * @param emailAddress The {@link EmailAddress} for which the IMAP client should be retrieved
     * @return A connected {@link ImapClient} for the specified email address
     * @throws ErrorCode if the mailbox configuration is invalid or missing
     */
    private ImapClient get(final EmailAddress emailAddress) {
        final Optional<MailBoxConfiguration> optionalMailBoxConfiguration =
                mailBoxConfManager.get(emailAddress.getEmailAddress());

        if (optionalMailBoxConfiguration.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_CONNECTION_CONFIGURATION);
        }

        return jakartaImapConnectionHelper.createClient(optionalMailBoxConfiguration.get());
    }

    /**
     * <p>
     * Retrieves existing threads for a given synced email ID and message ID.
     * </p>
     *
     * @param emailId   The internal synced email ID
     * @param messageId The provider message ID to search for in threads
     * @return A collection of {@link Thread} instances corresponding to the message ID
     */
    private Collection<Thread> getExistingThreads(final Long emailId, final String messageId) {
        return threadManager.getThreadsByEmailId(emailId, true, List.of(messageId));
    }

    /**
     * <p>
     * Processes draft updates, comparing content, recipients, and flags
     * between local and provider drafts.
     * </p>
     *
     * @param emailAddress               Represents the email address.
     * @param imapClient                 The {@link ImapClient} for fetching drafts
     * @param folderType                 The {@link FolderType} (DRAFT)
     * @param emailReceiverConfiguration The {@link EmailReceiverConfiguration} for the receiver
     * @param messageIds                 The collection of draft message IDs to process
     */
    private void processDraftUpdates(final EmailAddress emailAddress, final ImapClient imapClient, final FolderType folderType,
                                     final EmailReceiverConfiguration emailReceiverConfiguration,
                                     final Collection<String> messageIds) {
        for (final Collection<String> batch : JavaUtil.splitCollection(messageIds, 10)) {
            final Collection<Email> providerDrafts = imapClient.getMessages(get(folderType), batch);
            final Collection<Email> messageToBeProcessed = new HashSet<>();
            final Long emailId = emailAddress.id();

            if (Objects.nonNull(providerDrafts) && !providerDrafts.isEmpty()) {
                for (final Email providerDraft : providerDrafts) {

                    try {
                        final Collection<Thread> threads = getExistingThreads(emailId, providerDraft.getMetaData().getMessageId());

                        if (Objects.nonNull(threads) && !threads.isEmpty()) {
                            final Thread thread = threads.stream().findFirst().get();

                            thread.getComposeEmail().ifPresent(localDraft -> {

                                if (hasDraftChanged(localDraft, providerDraft)) {
                                    updateDraftContent(localDraft, providerDraft);

                                    messageToBeProcessed.add(localDraft);
                                }
                            });
                        }
                    } catch (Exception exception) {
                        LoggerUtil.logError(LOGGER, exception, "Auto sync: Draft update processing action failed, message: {}, trace: {}", exception.getMessage(), exception);

                        throw ErrorCode.get(EmailErrorCodes.AUTO_SYNC_ACTION_FAILED);
                    }
                }
            }

            if (!messageToBeProcessed.isEmpty()) {
                loadSenderInfo(emailReceiverConfiguration.getSyncEmailAddress(), messageToBeProcessed);
                loadEmails(emailAddress, emailReceiverConfiguration, SyncJobType.AUTO_SYNC
                        , new SyncContext(emailId, emailReceiverConfiguration.getSyncEmailAddress()), messageToBeProcessed);
            }
        }
    }

    /**
     * <p>
     * Checks whether a draft has changed in content, recipients, or flags.
     * </p>
     *
     * @param localDraft    The local {@link Email} draft
     * @param providerDraft The provider {@link Email} draft
     * @return {@code true} if the draft has changed, {@code false} otherwise
     */
    private boolean hasDraftChanged(final Email localDraft, final Email providerDraft) {
        if (!Objects.equals(localDraft.getSubject(), providerDraft.getSubject())) {
            return true;
        }

        if (!Objects.equals(localDraft.getContent(), providerDraft.getContent())) {
            return true;
        }
        final EmailRecipients localRecipients = localDraft.getEmailRecipients();
        final EmailRecipients providerRecipients = providerDraft.getEmailRecipients();

        if (Objects.nonNull(localRecipients)) {

            if (areRecipientsChanged(localRecipients.getToRecipients(), providerRecipients.getToRecipients())) {
                return true;
            }

            if (areRecipientsChanged(localRecipients.getToRecipients(), providerRecipients.getToRecipients())) {
                return true;
            }

            if (areRecipientsChanged(localRecipients.getCcRecipients(), providerRecipients.getCcRecipients())) {
                return true;
            }

            if (areRecipientsChanged(localRecipients.getBccRecipients(), providerRecipients.getBccRecipients())) {
                return true;
            }
        }

        return localDraft.isRead() != providerDraft.isRead() || localDraft.isStarred() != providerDraft.isStarred();
    }

    /**
     * <p>
     * Compares two recipient collections and returns {@code true} if they differ.
     * </p>
     *
     * @param existingRecipients The existing recipients
     * @param currentRecipients  The current recipients
     * @return {@code true} if collections differ; {@code false} otherwise
     */
    private boolean areRecipientsChanged(final Collection<?> existingRecipients, final Collection<?> currentRecipients) {
        final boolean isExistingRecipientIsNull = Objects.isNull(existingRecipients);
        final boolean isCurrentRecipientIsNull = Objects.isNull(currentRecipients);

        if (isExistingRecipientIsNull && isCurrentRecipientIsNull) {
            return false;
        }

        if (isCurrentRecipientIsNull || isExistingRecipientIsNull) {
            return true;
        }

        if (existingRecipients.size() != currentRecipients.size()) {
            return true;
        }

        return !existingRecipients.containsAll(currentRecipients) || !currentRecipients.containsAll(existingRecipients);
    }

    /**
     * <p>
     * Updates a local draft with content, recipients, and flags from provider draft.
     * </p>
     *
     * @param localDraft    The local {@link Email} draft to update
     * @param providerDraft The provider {@link Email} draft with updated content
     */
    private void updateDraftContent(final Email localDraft, final Email providerDraft) {
        localDraft.setSubject(providerDraft.getSubject());
        localDraft.setContent(providerDraft.getContent());

        final EmailRecipients emailRecipients = new EmailRecipients();
        final EmailRecipients providerRecipients = providerDraft.getEmailRecipients();

        if (Objects.nonNull(providerRecipients)) {
            providerRecipients.getToRecipients().forEach(emailRecipients::addToRecipient);
            providerRecipients.getCcRecipients().forEach(emailRecipients::addCcRecipient);
            providerRecipients.getBccRecipients().forEach(emailRecipients::addBccRecipient);
        }
        localDraft.setRecipients(List.of(emailRecipients));
        localDraft.setRead(providerDraft.isRead());
        localDraft.setStarred(providerDraft.isStarred());
    }

    /**
     * <p>
     * Safely closes the {@link Folder} and {@link Store} associated with
     * the {@link EmailProviderDataStore}.
     * </p>
     *
     * @param emailProviderDataStore The {@link EmailProviderDataStore} to clean up
     */
    private void cleanUp(final EmailProviderDataStore emailProviderDataStore) {
        if (Objects.nonNull(emailProviderDataStore)) {
            final Folder folder = emailProviderDataStore.getFolder();
            final Store store = emailProviderDataStore.getStore();

            try {
                if (folder.isOpen()) {
                    folder.close();
                }

                if (store.isConnected()) {
                    store.close();
                }
            } catch (Exception exception) {
                LoggerUtil.logError(LOGGER, exception, "Auto sync: Clean up action failed, message: {}, trace: {}", exception.getMessage(), exception);

                throw ErrorCode.get(EmailErrorCodes.EMAIL_SEND_FAILED);
            }
        }
    }
}