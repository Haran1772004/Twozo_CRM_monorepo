package com.tlc.email.server.internal.handler;

import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.commons.util.constants.NumericalConstant;
import com.tlc.email.common.model.CategorizedMailContent;
import com.tlc.email.common.model.EmailDeleteEvent;
import com.tlc.email.common.model.EmailUpdateEvent;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.request.EmailEventRequest;
import com.tlc.email.server.internal.manager.EmailManager;
import com.tlc.email.server.internal.manager.ThreadManager;
import com.tlc.email.server.internal.sync.AbstractSyncActionHandler;
import com.tlc.email.server.internal.sync.interval.SyncIntervalHandler;
import com.tlc.email.server.model.EmailProviderDataStore;
import com.tlc.email.server.model.GmailDataStore;
import com.tlc.email.server.model.sync.SyncContext;
import com.tlc.email.server.internal.provider.gmail.GmailClient;
import com.tlc.email.server.internal.provider.EmailTokenHandler;
import com.tlc.email.server.internal.provider.gmail.model.MessageLabelInfo;
import com.tlc.email.server.internal.provider.gmail.model.Label.Name;
import com.tlc.email.server.service.EmailProviderService;
import com.tlc.email.server.sql.resource.MEMADDRESS;
import com.tlc.email.server.sql.resource.MEMSYNCSETTINGS;
import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.CountQuery;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.LimitClause;
import com.tlc.sql.common.api.dml.MultiQuery;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;

import com.tlc.web.HttpMethod;
import com.tlc.web.HttpRequest;
import com.tlc.web.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * Service to load emails which are not loaded due to internal server issue
 * </p>
 *
 * @author Mohammed Asif J
 * @version 1.0
 */
public final class UnsyncedEmailLoaderService extends AbstractSyncActionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnsyncedEmailLoaderService.class);
    private static final String USER_ID = "me";
    private static final String DELETED_ITEMS = "deleteditems";
    private static final int LIMIT = NumericalConstant.TEN;

    private final SyncIntervalHandler syncIntervalHandler;
    private final EmailManager emailManager;
    private final ThreadManager threadManager;
    private final AdminDataStore adminDataStore;
    private final EmailTokenHandler emailTokenHandler;

    private UnsyncedEmailLoaderService() {
        this.syncIntervalHandler = SyncIntervalHandler.getInstance();
        this.emailManager = EmailManager.getInstance();
        this.threadManager = ThreadManager.getInstance();
        this.adminDataStore = SqlServiceProvider.get().getAdminDataStore();
        this.emailTokenHandler = EmailTokenHandler.getInstance();
    }

    private static class Instance {
        private static final UnsyncedEmailLoaderService INSTANCE = new UnsyncedEmailLoaderService();
    }

    public static UnsyncedEmailLoaderService getInstance() {
        return Instance.INSTANCE;
    }

    public Table getSyncSettingsTable() {
        return Table.get(MEMSYNCSETTINGS.TABLE);
    }

    public Table getEmailAddressTable() {
        return Table.get(MEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Loads the unsynced emails
     * </p>
     */
    public void loadEmails() {
        LOGGER.info("Unsynced email load: Starting process for all active synced accounts");
        final long totalCount = getActiveSyncEmailCount();
        LOGGER.info("Unsynced email load: Total active synced accounts found: {}", totalCount);

        if (totalCount > NumericalConstant.ZERO) {
            int start = NumericalConstant.ONE;

            do {
                int batchSize = (int) Math.min(LIMIT, totalCount - start + NumericalConstant.ONE);
                final DataContainer dataContainer = getActiveSyncEmailContainer(start, batchSize);

                dataContainer.getRows(getSyncSettingsTable()).forEach(row -> {
                    final Long emailId = row.get(MEMSYNCSETTINGS.EMAIL_ID);
                    final String emailAddress = dataContainer.getRow(getEmailAddressTable(), emailId)
                            .get(MEMADDRESS.EMAIL_ADDRESS);
                    final Long syncId = row.getPKValue();

                    try {
                        LOGGER.info("Unsynced email load: Processing for address: {}, syncId: {}", emailAddress, syncId);
                        initiateSync(syncId, emailId, emailAddress, row.get(MEMSYNCSETTINGS.DATE_CONFIG));
                    } catch (final Exception exception) {
                        LoggerUtil.logError(LOGGER, exception, "Unsynced email sync: Failed to process address: {}, syncId: {}, message: {}",
                                emailAddress, syncId, exception.getMessage());
                    }
                });
                start += LIMIT;
                LOGGER.info("Unsynced email load: Process completed");
            } while (start - NumericalConstant.ONE <= totalCount);

        } else {
            LOGGER.info("Unsynced email load: No active synced accounts found to process");
        }
    }

    /**
     * <p>
     * Loads the unsynced emails for a specific email address and folder within the given time interval.
     * </p>
     *
     * @param emailAddress The email address to process
     * @param folderType   The folder type to process
     * @param startTime    The start time of the interval to check for unsynced emails (epoch millis)
     * @param endTime      The end time of the interval to check for unsynced emails (epoch millis)
     */
    public void loadEmails(final String emailAddress, final FolderType folderType,
                           final long startTime, final long endTime) {
        final Map<Long, Long> syncIdWithEmailId = getSyncData(emailAddress);

        if (!syncIdWithEmailId.isEmpty()) {
            loadIntervalEmails(syncIdWithEmailId.entrySet().stream().findFirst().get().getKey(),
                    syncIdWithEmailId.entrySet().stream().findFirst().get().getValue(),
                    emailAddress, folderType, startTime, new EmailInterval(startTime, endTime));
        }
    }

    /**
     * <p>
     * Retrieves the active sync data for the given email address, including sync IDs and corresponding email IDs.
     * Only returns entries where sync is enabled and status is SYNCED.
     * </p>
     *
     * @param emailAddress The email address to query for
     * @return A map of sync ID to email ID for active synced accounts matching the email address
     */
    private Map<Long, Long> getSyncData(final String emailAddress) {
        final Table syncTable = Table.get(MEMSYNCSETTINGS.TABLE);
        final Table emailTable = Table.get(MEMADDRESS.TABLE);
        final SelectQuery selectQuery = SelectQuery.get(syncTable);

        selectQuery.addSelectClause(syncTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID));
        selectQuery.addJoinClause(new JoinClause(syncTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID), emailTable.getColumn(MEMADDRESS.ID)));
        selectQuery.setWhereClause(new WhereClause(
                Criteria.eq(syncTable.getColumn(MEMSYNCSETTINGS.IS_SYNC_ENABLED), true))
                .and(Criteria.eq(syncTable.getColumn(MEMSYNCSETTINGS.SYNC_STATUS), SyncStatus.SYNCED.getId()))
                .and(Criteria.eq(emailTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), emailAddress)));

        return adminDataStore.get(selectQuery).getRows(syncTable)
                .collect(Collectors.toMap(Row::getPKValue, row -> row.get(MEMSYNCSETTINGS.EMAIL_ID)));
    }

    /**
     * <p>
     * Processes a single email address and derives intervals and fetches missing emails for each folder interval.
     * </p>
     *
     * @param syncId       Represents the syncId
     * @param emailId      Represents the email ID
     * @param emailAddress Represents the email address
     * @param dateConfig   Represents the date config JSON
     */
    private void initiateSync(final Long syncId, final Long emailId,
                              final String emailAddress, final String dateConfig) {

        if (Objects.isNull(dateConfig) || dateConfig.isBlank()) {
            LOGGER.info("Unsynced email load: Processing stopped due to missing date config. email address: {}", emailAddress);
            return;
        }
        final Map<FolderType, List<EmailInterval>> intervalsByFolder =
                syncIntervalHandler.retrieveEmailIntervalsByFolderType(dateConfig);

        for (final Map.Entry<FolderType, List<EmailInterval>> entry : intervalsByFolder.entrySet()) {
            final FolderType folderType = entry.getKey();
            final List<EmailInterval> intervals = entry.getValue();

            intervals.forEach(emailInterval ->
                    loadIntervalEmails(syncId, emailId, emailAddress, folderType,
                            emailInterval.startTime(), emailInterval)
            );
            final long latestEndtime = intervals.stream().mapToLong(EmailInterval::endTime)
                    .max().orElse(0L);

            loadIntervalEmails(syncId, emailId, emailAddress, folderType, latestEndtime,
                    new EmailInterval(latestEndtime, System.currentTimeMillis()));
        }
    }

    /**
     * <p>
     * Initiates the sync for a given folder interval by fetching emails from the provider and processing them.
     * </p>
     *
     * @param syncId            Represents the syncId for logging and context
     * @param emailId           Represents the email ID
     * @param emailAddress      Represents the email address
     * @param folderType        Represents the folder type to be synced
     * @param intervalStartTime Represents the latest end time among intervals for this folder, used for logging context
     * @param emailInterval     Represents the specific email interval to sync
     */
    private void loadIntervalEmails(final Long syncId, final Long emailId, final String emailAddress, final FolderType folderType,
                                    final long intervalStartTime, final EmailInterval emailInterval) {
        try {
            LOGGER.info("Unsynced email load: Processing email address: {}, syncId: {}, folder: {}, interval ({}, {})",
                    emailAddress, syncId, folderType.name(), intervalStartTime, emailInterval.endTime());
            processInterval(syncId, emailId, emailAddress, folderType, emailInterval);
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Unsynced email load: Failed processing folder {} interval ({}, {}) for address: {}, message: {}",
                    folderType.name(), intervalStartTime, emailInterval.endTime(), emailAddress, exception.getMessage());
        }
    }

    /**
     * <p>
     * Fetches and loads the required emails from the provider for the given folder interval.
     * </p>
     *
     * @param syncId       Represents the syncId for logging and context
     * @param emailId      Represents the email ID
     * @param emailAddress Represents the email address
     * @param folderType   Represents the folder type to be synced
     * @param interval     Represents the folder interval
     */
    private void processInterval(final Long syncId, final Long emailId, final String emailAddress,
                                 final FolderType folderType, final EmailInterval interval) {
        final SyncAddressInfo syncAddressInfo = new SyncAddressInfo(emailAddress);

        syncAddressInfo.setId(emailId);
        final EmailAddress address = new EmailAddress(emailId, emailAddress);
        final EmailReceiverConfiguration receiverConfig = new EmailReceiverConfiguration();

        receiverConfig.setSyncEmailAddress(syncAddressInfo);
        receiverConfig.setFolderTypes(List.of(folderType));
        receiverConfig.setEmailIntervals(Map.of(folderType, interval));
        final EmailProviderService providerService = EmailProviderService.getService(address);
        final EmailProviderDataStore dataStore = providerService.receive(receiverConfig);

        if (Objects.isNull(dataStore)) {
            LOGGER.info("Unsynced email load: Failed to retrieve data from provider for address: {}, folder: {}, interval ({}, {})",
                    emailAddress, folderType.name(), interval.startTime(), interval.endTime());
            return;
        }
        final SyncContext syncContext = new SyncContext(syncId, syncAddressInfo);
        final Set<String> intervalMessageIds = new HashSet<>();

        loadPage(emailId, address, receiverConfig, syncContext, dataStore.getEmails(), intervalMessageIds);

        while (dataStore.hasNextLink() ||
                (dataStore instanceof GmailDataStore gmailDataStore && Objects.nonNull(gmailDataStore.getPageToken()))) {
            providerService.loadEmailProviderData(dataStore);
            loadPage(emailId, address, receiverConfig, syncContext, dataStore.getEmails(), intervalMessageIds);
        }
        processDeletedEmails(emailId, syncAddressInfo, folderType, interval, intervalMessageIds);
    }

    /**
     * <p>
     * Processes a single page of provider emails
     * </p>
     *
     * @param emailId            Represents the email ID
     * @param address            Represents the sync address
     * @param receiverConfig     The receiver configuration used for this batch
     * @param syncContext        The sync context carrying sync ID and address info
     * @param emails             The emails fetched from the provider for this page
     * @param intervalMessageIds Represents the complete collection of message IDs for the provided inteval
     */
    private void loadPage(final Long emailId, final EmailAddress address, final EmailReceiverConfiguration receiverConfig,
                          final SyncContext syncContext, final Collection<Email> emails, final Set<String> intervalMessageIds) {
        if (Objects.isNull(emails) || emails.isEmpty()) {
            return;
        }
        final Set<String> messageIds = emails.stream().map(email ->
                email.getMetaData().getMessageId()).filter(Objects::nonNull).collect(Collectors.toSet());

        intervalMessageIds.addAll(messageIds);

        if (messageIds.isEmpty()) {
            return;
        }
        final Collection<String> existingMessageIds = emailManager.getMessageIds(emailId, messageIds);
        final Collection<Email> newEmails = emails.stream().filter(email -> {
            final String messageId = email.getMetaData().getMessageId();

            return Objects.isNull(messageId) || !existingMessageIds.contains(messageId);
        }).collect(Collectors.toList());
        final Collection<Email> existingEmails = emails.stream().filter(email -> {
            final String messageId = email.getMetaData().getMessageId();

            return Objects.nonNull(messageId) && existingMessageIds.contains(messageId);
        }).collect(Collectors.toList());

        if (!newEmails.isEmpty()) {
            LOGGER.info("Unsynced email load: Loading {} new emails for address: {}", newEmails.size(), address.getEmailAddress());
            loadEmails(address, receiverConfig, SyncJobType.AUTO_SYNC, syncContext, newEmails);
        }

        if (!existingEmails.isEmpty()) {
            processUpdatedEmails(emailId, address, receiverConfig, syncContext, existingEmails);
        }
    }

    /**
     * <p>
     * Process the updated emails
     * </p>
     *
     * @param emailId         Represents the email ID
     * @param address         The {@link EmailAddress} of the synced account
     * @param config          The receiver configuration used for this batch
     * @param syncContext     The sync context carrying sync ID and address info
     * @param retrievedEmails Emails from the provider that already exist in the database
     */
    private void processUpdatedEmails(final Long emailId, final EmailAddress address, final EmailReceiverConfiguration config,
                                      final SyncContext syncContext, final Collection<Email> retrievedEmails) {
        final Collection<Email> updatedEmails = new ArrayList<>();

        for (final Email retrievedEmail : retrievedEmails) {
            final String messageId = retrievedEmail.getMetaData().getMessageId();
            final Collection<Thread> threads = threadManager.getThreadsByEmailId(emailId,
                    !retrievedEmail.getMessageCategory().equals(Email.MessageCategory.DRAFT), List.of(messageId));

            if (Objects.nonNull(threads) && !threads.isEmpty()) {
                threads.stream().findFirst().get().getComposeEmail().ifPresent(localEmail -> {

                    if (hasEmailChanged(localEmail, retrievedEmail)) {
                        updatedEmails.add(retrievedEmail);
                    }
                });
            }
        }

        if (!updatedEmails.isEmpty()) {
            updateExistingEmails(address, config, syncContext, updatedEmails);
        }
    }

    /**
     * <p>
     * Checks whether email is updated or not
     * </p>
     *
     * @param existingEmail  The email currently stored in the database
     * @param retrievedEmail The email returned by the provider
     * @return true if the email got changed based on the comparison of read, starred, archived status, subject, content and recipients (for draft emails)
     */
    private boolean hasEmailChanged(final Email existingEmail, final Email retrievedEmail) {

        if (existingEmail.isRead() != retrievedEmail.isRead()) {
            return true;
        }

        if (existingEmail.isStarred() != retrievedEmail.isStarred()) {
            return true;
        }

        if (existingEmail.isArchived() != retrievedEmail.isArchived()) {
            return true;
        }

        if (existingEmail.getMessageCategory().equals(Email.MessageCategory.DRAFT)) {
            final Optional<CategorizedMailContent> categorizedMailContent = emailManager.getCategorizedMailContent(existingEmail.id());

            if (categorizedMailContent.isPresent()) {

                if (!Objects.equals(categorizedMailContent.get().subject(), retrievedEmail.getSubject()) ||
                        !Objects.equals(categorizedMailContent.get().content(), retrievedEmail.getContent())) {
                    return true;
                }
            }
            final EmailRecipients localRecipients = existingEmail.getEmailRecipients();
            final EmailRecipients providerRecipients = retrievedEmail.getEmailRecipients();

            if (Objects.nonNull(localRecipients) && Objects.nonNull(providerRecipients)) {

                if (hasRecipientsChanged(localRecipients.toRecipients(), providerRecipients.toRecipients())) {
                    return true;
                }

                if (hasRecipientsChanged(localRecipients.ccRecipients(), providerRecipients.ccRecipients())) {
                    return true;
                }

                return hasRecipientsChanged(localRecipients.bccRecipients(), providerRecipients.bccRecipients());
            }
        }

        return false;
    }

    /**
     * <p>
     * Checks whether the recipients are changed or not by comparing the existing recipients with the retrieved recipients from the provider.
     * </p>
     *
     * @param existingRecipients  The recipients currently stored in the database
     * @param retrievedRecipients The recipients returned by the provider
     * @return true if the recipients are changed
     */
    private boolean hasRecipientsChanged(final Collection<EmailAddress> existingRecipients,
                                         final Collection<EmailAddress> retrievedRecipients) {
        final boolean isExistingNullOrEmpty = Objects.isNull(existingRecipients) || existingRecipients.isEmpty();
        final boolean isRetrievedNullOrEmpty = Objects.isNull(retrievedRecipients) || retrievedRecipients.isEmpty();

        if (isExistingNullOrEmpty && isRetrievedNullOrEmpty) {
            return false;
        }

        if (isExistingNullOrEmpty != isRetrievedNullOrEmpty) {
            return true;
        }

        if (existingRecipients.size() != retrievedRecipients.size()) {
            return true;
        }
        final Set<String> existingAddresses = existingRecipients.stream()
                .map(r -> r.getEmailAddress().toLowerCase())
                .collect(Collectors.toSet());

        final Set<String> retrievedAddress = retrievedRecipients.stream()
                .map(r -> r.getEmailAddress().toLowerCase())
                .collect(Collectors.toSet());

        return !existingAddresses.equals(retrievedAddress);
    }

    /**
     * <p>
     * Processes the emails which are deleted from provider but not reflected in CRM
     * by fetching the message ids in the given interval and cross verifying with provider.
     * </p>
     *
     * @param emailId            The MEMADDRESS primary key
     * @param syncAddressInfo    The {@link SyncAddressInfo} of the synced account
     * @param folderType         The folder being processed
     * @param interval           The time interval used to scope the DB query
     * @param intervalMessageIds All message IDs returned by the provider for this folder
     */
    private void processDeletedEmails(final Long emailId, final SyncAddressInfo syncAddressInfo,
                                      final FolderType folderType, final EmailInterval interval,
                                      final Set<String> intervalMessageIds) {
        final Email.MessageCategory messageCategory = Email.MessageCategory.getMessageCategory(folderType);

        if (Objects.isNull(messageCategory)) {
            return;
        }
        final Collection<String> missingMessageIds = emailManager.getMessageIdsInInterval(
                emailId, messageCategory, interval.startTime(), interval.endTime(), intervalMessageIds
        );

        if (missingMessageIds.isEmpty()) {
            return;
        }
        final Collection<Long> deletedConversationIds = new ArrayList<>();
        final Collection<Thread> trashedThreads = new ArrayList<>();

        for (final String messageId : missingMessageIds) {
            final Long conversationId = emailManager.getConversationId(messageId);

            if (Objects.nonNull(conversationId)) {

                if (existsInTrashFolder(syncAddressInfo, messageId)) {
                    threadManager.getThreadsByEmailId(syncAddressInfo.id(), false, List.of(messageId))
                            .forEach(thread -> {
                                thread.getComposeEmail().ifPresent(email -> {
                                    email.isActive(false);
                                    trashedThreads.add(thread);
                                });
                            });
                } else {
                    deletedConversationIds.add(conversationId);
                }
            }
        }

        if (!trashedThreads.isEmpty() || !deletedConversationIds.isEmpty()) {
            handleDeletedMails(syncAddressInfo, trashedThreads, deletedConversationIds);
        }
    }

    /**
     * <p>
     * Checks whether the email exists in trash folder
     * </p>
     *
     * @param syncAddressInfo The {@link SyncAddressInfo} of the synced account
     * @param messageId       The message ID to search for
     * @return {@code true} if the message exists in trash on the provider; {@code false} otherwise or on error
     */
    private boolean existsInTrashFolder(final SyncAddressInfo syncAddressInfo, final String messageId) {

        try {
            return switch (EmailProviderService.getProvider(syncAddressInfo)) {
                case GOOGLE -> checkGmailTrash(syncAddressInfo, messageId);
                case OUTLOOK -> checkOutlookTrash(syncAddressInfo, messageId);
                default -> false;
            };
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Unsynced email sync: Error checking if message {} exists in trash folder: {}",
                    messageId, exception.getMessage());
            return false;
        }
    }

    /**
     * <p>
     * Checks if a message exists in Gmail trash by querying for the TRASH label.
     * Converts RFC822 Message-ID to Gmail internal message ID first.
     * </p>
     *
     * @param syncAddressInfo The sync address information
     * @param messageId       The RFC822 Message-ID (internet message ID)
     * @return {@code true} if message has TRASH label; {@code false} otherwise
     */
    private boolean checkGmailTrash(final SyncAddressInfo syncAddressInfo, final String messageId) {
        try {
            final GmailClient gmailClient = getGmailClient(syncAddressInfo);

            if (Objects.isNull(gmailClient)) {
                LOGGER.info("Unsynced email load: Failed to obtain Gmail client for checking trash status for message: {}, address: {}",
                        messageId, syncAddressInfo.getEmailAddress());
                return false;
            }
            final Optional<String> gmailMessageId = getGmailMessageId(gmailClient, messageId);

            if (gmailMessageId.isEmpty()) {
                LOGGER.info("Unsynced email sync: Gmail message {} could not be found using RFC822 Message-ID", messageId);
                return false;
            }
            final MessageLabelInfo messageLabelInfo = gmailClient.getMessageLabelData(USER_ID, gmailMessageId.get());

            if (messageLabelInfo.getLabelList() == null) {
                LOGGER.info("Unsynced email sync: Gmail message {} not found or has no labels", messageId);
                return false;
            }
            final boolean isInTrash = messageLabelInfo.getLabelList().stream()
                    .anyMatch(label -> Objects.nonNull(label) && label.equals(Name.TRASH));

            if (isInTrash) {
                LOGGER.info("Unsynced email sync: Gmail message {} found in trash folder", messageId);
            }
            return isInTrash;
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Unsynced email sync: Error checking Gmail trash status for message: {}",
                    messageId, exception.getMessage());
            return false;
        }
    }

    /**
     * <p>
     * Converts an RFC822 Message-ID to a Gmail internal message ID by querying the Gmail API.
     * </p>
     *
     * @param gmailClient     The authenticated Gmail client
     * @param rfc822MessageId The RFC822 Message-ID (e.g., <CAGCCSpz=...@mail.gmail.com>)
     * @return An Optional containing the Gmail internal message ID if found, or empty if not found
     */
    private Optional<String> getGmailMessageId(final GmailClient gmailClient, final String rfc822MessageId) {
        try {
            return gmailClient.getMessageByRfcMessageId(USER_ID, rfc822MessageId)
                    .map(message -> message.getId());
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Unsynced email sync: Error converting RFC822 Message-ID to Gmail ID for message: {}",
                    rfc822MessageId, exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * <p>
     * Checks if a message exists in Outlook Deleted Items folder.
     * For Outlook, messages in Deleted Items folder have the isDeleted property set to true.
     * </p>
     *
     * @param syncAddressInfo The sync address information
     * @param messageId       The Outlook message ID (internet message ID)
     * @return {@code true} if message is in Deleted Items; {@code false} otherwise
     */
    private boolean checkOutlookTrash(final SyncAddressInfo syncAddressInfo, final String messageId) {
        try {
            final String accessToken = emailTokenHandler.getAccessToken(syncAddressInfo);

            if (Objects.isNull(accessToken) || accessToken.isBlank()) {
                LOGGER.info("Unsynced email sync: No access token available for Outlook address: {}", syncAddressInfo.getEmailAddress());
                return false;
            }
            final String escapedMessageId = messageId.replace("'", "''");
            final String filterValue = "internetMessageId eq '" + escapedMessageId + "'";
            final String url = "https://graph.microsoft.com/v1.0/me/mailFolders/" + DELETED_ITEMS +
                    "/messages?$filter=" + URLEncoder.encode(filterValue, StandardCharsets.UTF_8) +
                    "&$select=id,internetMessageId,isDeleted";
            final JsonObject response = HttpUtil.makeRequest(new HttpRequest(new URL(url),
                    Collections.singletonMap("Authorization", "Bearer " + accessToken), HttpMethod.GET, Collections.emptyMap())
            );

            if (Objects.isNull(response)) {
                return false;
            }

            if (response.containsKey("value")) {
                final JsonArray messages = response.getJsonArray("value");

                return messages.size() > 0;
            }

            return false;
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Unsynced email sync: Error checking Outlook trash status for message: {}",
                    messageId, exception.getMessage());
            return false;
        }
    }

    /**
     * <p>
     * Obtains a Gmail client with proper credentials for the given email address.
     * </p>
     *
     * @param syncAddressInfo The sync address information containing the email address
     * @return A configured {@link GmailClient} instance, or null if credentials cannot be obtained
     */
    private GmailClient getGmailClient(final SyncAddressInfo syncAddressInfo) {
        try {
            final String accessToken = emailTokenHandler.getAccessToken(syncAddressInfo);

            if (Objects.isNull(accessToken) || accessToken.isBlank()) {
                LOGGER.info("Unsynced email load: No access token available for address: {}", syncAddressInfo.getEmailAddress());
                return null;
            }

            return new GmailClient.Builder().setAccessToken(accessToken).build();
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Unsynced email sync: Failed to create Gmail client for address: {}, message: {}",
                    syncAddressInfo.getEmailAddress(), exception.getMessage());
            return null;
        }
    }

    /**
     * <p>
     * Gets the count of actively synced emails
     * </p>
     *
     * @return the count of actively synced emails
     */
    private long getActiveSyncEmailCount() {
        return adminDataStore.get((CountQuery) getActiveSyncEmailQuery(true));
    }

    /**
     * <p>
     * Gets the data container of actively synced emails based on the given start index and predefined limit.
     * </p>
     *
     * @param start the starting index for pagination
     * @return the data container containing actively synced emails for the specified page
     */
    private DataContainer getActiveSyncEmailContainer(final int start, final int batchSize) {
        final SelectQuery selectQuery = (SelectQuery) getActiveSyncEmailQuery(false);

        selectQuery.setLimitClause(new LimitClause(start, batchSize));

        return adminDataStore.get(selectQuery);
    }

    /**
     * <p>
     * Gets the query of actively synced emails
     * </p>
     *
     * @return the query of actively synced emails
     */
    public MultiQuery getActiveSyncEmailQuery(final boolean isCountQuery) {
        final Table syncTable = getSyncSettingsTable();
        final Table addressTable = getEmailAddressTable();
        final MultiQuery multiQuery = isCountQuery ? CountQuery.get(syncTable) : SelectQuery.get(syncTable);

        multiQuery.addJoinClause(new JoinClause(syncTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID), addressTable.getPKColumn()));
        multiQuery.setWhereClause(
                new WhereClause(Criteria.eq(syncTable.getColumn(MEMSYNCSETTINGS.SYNC_STATUS), SyncStatus.SYNCED.getId()))
                        .and(Criteria.eq(syncTable.getColumn(MEMSYNCSETTINGS.CONNECTION_STATE), ConnectionStatus.ACTIVE.getId()))
                        .and(Criteria.in(syncTable.getColumn(MEMSYNCSETTINGS.PROVIDER), EmailProvider.getOauthProviders()
                                .stream().map(EmailProvider::getId).collect(Collectors.toSet()))));

        return multiQuery;
    }

    /**
     * <p>
     * Processes the modified mails by handling deleted message IDs and updated threads.
     * Deletes messages and updates thread labels accordingly, then publishes the changes.
     * </p>
     *
     * @param syncAddressInfo        The {@link SyncAddressInfo} containing sync-related email information
     * @param trashedEmails          The collection of modified {@link Thread} instances
     * @param deletedConversationIds The collection of message IDs that were detected as deleted from the provider
     */
    private void handleDeletedMails(final SyncAddressInfo syncAddressInfo, final Collection<Thread> trashedEmails,
                                    final Collection<Long> deletedConversationIds) {
        if (Objects.nonNull(trashedEmails) && !trashedEmails.isEmpty()) {
            threadManager.updateThreadLabels(trashedEmails);
        }

        if (Objects.nonNull(deletedConversationIds) && !deletedConversationIds.isEmpty()) {
            threadManager.deleteMessages(deletedConversationIds);
        }
        publishDeletedMails(syncAddressInfo, trashedEmails, deletedConversationIds);
    }

    /**
     * <p>
     * Publishes modified mail data, including deleted message IDs and trashedThreads
     * </p>
     *
     * @param syncAddressInfo The {@link SyncAddressInfo} containing sync-related email information
     * @param trashedThreads  The collection of trashed {@link Thread} instances
     */
    private void publishDeletedMails(final SyncAddressInfo syncAddressInfo,
                                     final Collection<Thread> trashedThreads,
                                     final Collection<Long> deletedConversationIds) {

        final boolean hasToBeTrashed = Objects.nonNull(trashedThreads) && !trashedThreads.isEmpty();
        final boolean hasToBeDeleted = Objects.nonNull(deletedConversationIds) && !deletedConversationIds.isEmpty();

        if (!hasToBeTrashed && !hasToBeDeleted) {
            return;
        }
        final EmailEventRequest emailEventRequest = new EmailEventRequest();
        final EmailReceiverConfiguration emailReceiverConfiguration = new EmailReceiverConfiguration();

        emailReceiverConfiguration.setSyncEmailAddress(syncAddressInfo);
        emailEventRequest.setReceiverConfiguration(emailReceiverConfiguration);

        if (hasToBeTrashed) {
            final ThreadHolder threadHolder = new ThreadHolder();

            threadHolder.setSyncedEmailAddress(syncAddressInfo);
            threadHolder.setThreads(trashedThreads);

            emailEventRequest.setUpdateEvent(new EmailUpdateEvent(threadHolder));
        }

        if (hasToBeDeleted) {
            emailEventRequest.setDeleteEvent(new EmailDeleteEvent(deletedConversationIds));
        }
        emailEventRequestPublisher.publish(emailEventRequest);
    }
}