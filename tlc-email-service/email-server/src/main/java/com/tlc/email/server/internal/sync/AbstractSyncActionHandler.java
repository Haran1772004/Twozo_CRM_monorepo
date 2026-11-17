package com.tlc.email.server.internal.sync;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.page.Page;
import com.tlc.commons.page.PageHandler;
import com.tlc.commons.util.LoggerUtil;

import com.tlc.email.common.fields.EmailFields;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailCreateEvent;
import com.tlc.email.common.model.EmailUpdateEvent;
import com.tlc.email.common.model.ServiceType;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.publish.EmailEventRequestPublisher;
import com.tlc.email.common.request.EmailEventRequest;
import com.tlc.email.server.internal.handler.ThreadPageHandler;
import com.tlc.email.server.internal.manager.EmailManager;
import com.tlc.email.server.internal.webhook.receiver.EmailWebhookReceiver;
import com.tlc.email.server.internal.webhook.receiver.manager.EmailWebHookReceiverManager;
import com.tlc.email.server.model.EmailProviderDataStore;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.sync.SyncStatusInfo;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.publish.EmailRequestPublisher;
import com.tlc.email.common.publish.EmailSyncRequestPublisher;
import com.tlc.email.common.publish.EmailTenantSyncRequestPublisher;
import com.tlc.email.common.request.SyncRequest;
import com.tlc.email.common.model.config.FolderInterval;
import com.tlc.email.common.request.TenantSyncRequest;
import com.tlc.email.common.model.type.Email.MessageCategory;

import com.tlc.email.server.internal.handler.ConversationAttachmentHandler;
import com.tlc.email.server.internal.publisher.EmailEventPublisher;
import com.tlc.email.server.internal.sync.cache.SyncInfoWriteService;
import com.tlc.email.common.model.formatter.EmailReferenceFormatter;
import com.tlc.email.server.model.GmailDataStore;
import com.tlc.email.server.service.EmailProviderService;
import com.tlc.email.server.internal.manager.ThreadManager;
import com.tlc.email.server.internal.manager.ReferenceManager;
import com.tlc.email.server.internal.manager.SyncSettingsManager;
import com.tlc.email.server.internal.sync.interval.SyncIntervalHandler;
import com.tlc.email.server.model.sync.SyncContext;
import com.tlc.email.server.sql.resource.MEMADDRESS;
import com.tlc.email.server.sql.resource.MEMCONVERSATION;

import com.tlc.email.server.sql.resource.MEMEXTERNALMESSAGE;
import com.tlc.email.server.sql.resource.MEMSYNCSETTINGS;
import com.tlc.email.server.sql.resource.MEMTHREAD;
import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;

import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.JoinClause;
import com.tlc.sql.common.api.dml.SelectQuery;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sql.common.api.ds.AdminDataStore;
import com.tlc.sql.common.provider.SqlServiceProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.tlc.email.common.model.config.FolderType.ARCHIVE;
import static com.tlc.email.common.model.config.FolderType.DRAFT;
import static com.tlc.email.common.model.config.FolderType.INBOX;
import static com.tlc.email.common.model.config.FolderType.SENT;
import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;

/**
 * <p>
 * Handles the email retrieval, and synchronize the intervals, and manages tasks to load emails asynchronously
 * for efficient folder-specific operations.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public abstract class AbstractSyncActionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSyncActionHandler.class);

    private final SyncIntervalHandler syncIntervalHandler;
    private final ThreadManager threadManager;
    private final SyncSettingsManager syncSettingsManager;
    private final SyncInfoWriteService syncInfoWriteService;
    private final AdminDataStore adminDataStore;
    private final EmailEventPublisher emailEventPublisher;
    private final ConversationAttachmentHandler conversationAttachmentHandler;
    private final EmailRequestPublisher<SyncRequest> syncRequestPublisher;
    private final EmailRequestPublisher<TenantSyncRequest> tenantSyncRequestPublisher;
    private final EmailWebHookReceiverManager emailWebHookReceiverManager;
    private final EmailManager emailManager;

    protected final EmailEventRequestPublisher emailEventRequestPublisher;
    protected final ReferenceManager referenceManager;

    protected AbstractSyncActionHandler() {
        this.syncIntervalHandler = SyncIntervalHandler.getInstance();
        this.threadManager = ThreadManager.getInstance();
        this.syncSettingsManager = SyncSettingsManager.getInstance();
        this.referenceManager = ReferenceManager.getInstance();
        this.syncInfoWriteService = SyncInfoWriteService.get();
        this.adminDataStore = SqlServiceProvider.get().getAdminDataStore();
        this.emailEventPublisher = EmailEventPublisher.get();
        this.conversationAttachmentHandler = ConversationAttachmentHandler.getInstance();
        this.syncRequestPublisher = EmailSyncRequestPublisher.getInstance();
        this.tenantSyncRequestPublisher = EmailTenantSyncRequestPublisher.getInstance();
        this.emailWebHookReceiverManager = EmailWebHookReceiverManager.get();
        this.emailEventRequestPublisher = EmailEventRequestPublisher.getInstance();
        this.emailManager = EmailManager.getInstance();
    }

    /**
     * <p>
     * Handles the {@link SyncRequest}.
     * </p>
     *
     * @param syncRequest Holds the sync related attributes in {@link SyncRequest}
     */
    protected void handleSync(final SyncRequest syncRequest) {
        final List<FolderInterval> unSyncedIntervals = new ArrayList<>();
        final List<FolderInterval> syncedIntervals = new ArrayList<>();
        final EmailReceiverConfiguration receiverConfiguration = syncRequest.getReceiverConfiguration();
        final SyncAddressInfo syncAddressInfo = receiverConfiguration.getSyncEmailAddress();
        final Long syncRequestId = syncRequest.id();

        syncIntervalHandler.loadSyncAndUnsyncedIntervals(syncRequest, syncedIntervals, unSyncedIntervals);

        if (!syncedIntervals.isEmpty() && Objects.nonNull(syncRequestId)) {

            syncedIntervals.forEach(folderInterval -> {
                final String folderName = folderInterval.getFolder().name();
                final Collection<EmailInterval> intervals = folderInterval.getEmailIntervals();

                intervals.forEach(emailInterval -> {
                    final long startTime = emailInterval.startTime();
                    final long endTime = emailInterval.endTime();

                    LOGGER.debug("Email sync : Synced interval data retrieved and going to publish, start time: {}, end time: {}, syncId: {}", startTime, endTime, syncRequestId);

                    loadThreads(syncRequest, new ThreadPageHandler(syncRequestId, startTime, endTime, getCriteria(folderName)));
                });
            });
        }

        if (!unSyncedIntervals.isEmpty()) {
            sync(new SyncContext(syncRequestId, syncAddressInfo, unSyncedIntervals, syncRequest.getStateData()), syncRequest.getSyncJobType());
        }
        final String emailAddress = syncAddressInfo.getEmailAddress();
        final SyncStatusInfo syncStatusInfo = new SyncStatusInfo(syncRequestId, emailAddress, SyncStatus.SYNCED);
        final Long emailId = syncAddressInfo.id();

        if (syncRequest.getSyncJobType().isSyncType() && (
                emailWebHookReceiverManager.isUnsubscribed(emailId) || emailWebHookReceiverManager.isExpired(emailId)
        )) {
            final EmailWebhookReceiver emailWebhookReceiver = EmailWebhookReceiver.getService(syncAddressInfo);

            if (Objects.nonNull(emailWebhookReceiver)) {
                emailWebhookReceiver.subscribe();
            }
        }
        syncInfoWriteService.updateStatus(syncStatusInfo);
        emailEventPublisher.publishSyncStatus(syncAddressInfo, syncStatusInfo);
        LOGGER.debug("Email sync : Data retrieved for synced and un-synced interval completed for the syncId: {}", syncRequestId);
    }

    /**
     * <p>
     * Loads paginated {@link Thread} data using the provided {@link PageHandler},
     * trims email content, and publishes the threads via {@code syncRequestPublisher}.
     * Recursively loads next pages if available.
     * </p>
     *
     * @param syncRequest       The sync request containing the sync context
     * @param threadPageHandler The {@link PageHandler} to retrieve thread pages
     */
    private void loadThreads(final SyncRequest syncRequest, final PageHandler<Thread> threadPageHandler) {
        final Page<Thread> currentPage = threadPageHandler.getCurrentPage();

        if (Objects.nonNull(currentPage)) {
            final Collection<Thread> threads = currentPage.getData();

            if (Objects.nonNull(threads) && !threads.isEmpty()) {
                syncRequestPublisher.publish(syncRequest, threads);
            }
        }

        if (threadPageHandler.hasNextPage()) {
            threadPageHandler.goToNextPage();
            loadThreads(syncRequest, threadPageHandler);
        }
    }

    /**
     * <p>
     * Handles the {@link SyncRequest}.
     * </p>
     *
     * @param syncRequest Holds the sync related attributes in {@link SyncRequest}.
     * @param emails      Refers to the {@link Email}.
     */
    protected void handleSync(final SyncRequest syncRequest, final Collection<Email> emails) {
        if (Objects.nonNull(emails) && !emails.isEmpty()) {
            final EmailReceiverConfiguration emailReceiverConfiguration = syncRequest.getReceiverConfiguration();
            final Long syncId = syncRequest.id();
            final SyncAddressInfo emailAddress = emailReceiverConfiguration.getSyncEmailAddress();

            loadEmails(
                    emailManager.getOrInitEmail(emailAddress.getEmailAddress()), emailReceiverConfiguration, SyncJobType.AUTO_SYNC,
                    new SyncContext(syncId, emailAddress, null, syncRequest.getStateData()), emails
            );
        }
    }

    /**
     * <p>
     * Retrieves the criteria for filtering conversations based on the specified folder type.
     * </p>
     *
     * @param folder The name of the folder (e.g., "INBOX", "SENT", "DRAFT", "ARCHIVE").
     * @return A {@link WhereClause} object containing the criteria for filtering conversations.
     */
    private WhereClause getCriteria(final String folder) {
        final Table conversationTable = getConversationsTable();
        final FolderType folderType = FolderType.valueOf(folder);

        return switch (folderType) {
            case INBOX ->
                    new WhereClause(Criteria.eq(conversationTable.getColumn(MEMCONVERSATION.MESSAGE_CATEGORY), INBOUND.getId()))
                            .and(Criteria.eq(getThreadTable().getColumn(MEMTHREAD.IS_ARCHIVED), false));
            case SENT ->
                    new WhereClause(Criteria.eq(conversationTable.getColumn(MEMCONVERSATION.MESSAGE_CATEGORY), OUTBOUND.getId()))
                            .and(Criteria.eq(getThreadTable().getColumn(MEMTHREAD.IS_ARCHIVED), false));
            case DRAFT ->
                    new WhereClause(Criteria.eq(conversationTable.getColumn(MEMCONVERSATION.MESSAGE_CATEGORY), Email.MessageCategory.DRAFT.getId()));
            case ARCHIVE -> new WhereClause(Criteria.eq(getThreadTable().getColumn(MEMTHREAD.IS_ARCHIVED), true));
        };
    }

    /**
     * <p>
     * Loads and processes a collection of emails for synchronization.
     * </p>
     *
     * @param serviceType Represent the service type
     * @param emails      The collection of emails to be processed.
     * @param stateData   The state data that stores sync-related metadata.
     * @param syncRequest The synchronization request for tenant processing.
     */
    protected void loadEmails(final ServiceType serviceType, final Collection<Email> emails,
                              final StateData stateData, final TenantSyncRequest syncRequest) {
        final Collection<Thread> threads = new HashSet<>();
        final DataContainer targetContainer = DataContainer.create();

        for (final Email email : emails) {
            final SyncAddressInfo syncAddressInfo = syncRequest.getTenantConfiguration().getSyncEmailAddress();
            final EmailAddress emailAddress = emailManager.getOrInitEmail(syncAddressInfo.getEmailAddress());

            loadRetrievedMails(emailAddress, syncAddressInfo, serviceType.isSmartBcc() ? SyncJobType.SMART_BCC : SyncJobType.TEAM_INBOX, List.of(email), threads, targetContainer);
        }
        adminDataStore.commitChanges(targetContainer);

        if (!threads.isEmpty()) {
            populateMetaData(threads, emails);
            stateData.put(EmailFields.SERVICE_TYPE, serviceType.getName());
            syncRequest.setStateData(stateData);
            tenantSyncRequestPublisher.publish(syncRequest, threads);
        }
    }

    /**
     * <p>
     * Populates metadata values from the newly retrieved email into the mapped thread email.
     * This method compares the first available thread and first available retrieved email.
     * </p>
     *
     * @param threads Represents the {@link Collection} of {@link Thread}
     * @param emails  Represents the {@link Collection} of {@link Email}
     */
    private void populateMetaData(final Collection<Thread> threads, final Collection<Email> emails) {
        final Optional<Thread> optionalThread = threads.stream().findFirst();
        final Optional<Email> optionalNewEmail = emails.stream().findFirst();

        optionalThread.ifPresent(thread -> {
            final Optional<Email> optionalEmail = thread.getComposeEmail();

            if (optionalNewEmail.isPresent() && optionalEmail.isPresent()) {
                final Email newEmail = optionalNewEmail.get();
                final Email existingEmail = optionalEmail.get();

                loadMetaData(newEmail, existingEmail);
            }
        });
    }

    /**
     * <p>
     * Loads selected metadata fields from source email into the existing email metadata.
     * </p>
     *
     * @param email         Represents the source {@link Email}
     * @param existingEmail Represents the existing {@link Email}
     */
    private void loadMetaData(final Email email, final Email existingEmail) {
        final EmailMetaData emailMetaData = email.getMetaData();
        final EmailMetaData existingMetaData = existingEmail.getMetaData();

        if (Objects.nonNull(emailMetaData) && Objects.nonNull(existingMetaData)) {
            existingMetaData.setSenderId(emailMetaData.getSenderId());
            existingMetaData.setReceived(emailMetaData.getReceived());
        }
    }

    /**
     * <p>
     * Loads and fetches the sync id for the given from address
     * </p>
     *
     * @param fromAddress            Represents the from address
     * @param existingEmailContainer Represents the source container
     * @param targetContainer        Represents the target container
     * @return the sync id
     */
    private Long loadAndFetchSyncId(final String fromAddress, final DataContainer existingEmailContainer, final DataContainer targetContainer) {
        final Table addressTable = getAddressTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(addressTable.getColumn(MEMADDRESS.EMAIL_ADDRESS), fromAddress));
        final Row existingRow = existingEmailContainer.getRow(addressTable, whereClause);

        if (Objects.isNull(existingRow)) {
            final Row targetContainerRow = targetContainer.getRow(addressTable, whereClause);
            final Row newRow = Objects.isNull(targetContainerRow) ? new Row(addressTable) : targetContainerRow;

            if (newRow.isNewRow()) {
                newRow.set(MEMADDRESS.EMAIL_ADDRESS, fromAddress);
            }
            targetContainer.updateRow(newRow);
            return loadAndFetchSyncSettingsId(newRow.getPKValue(), existingEmailContainer, targetContainer);
        } else {
            targetContainer.updateRow(existingRow);
            return loadAndFetchSyncSettingsId(existingRow.getPKValue(), existingEmailContainer, targetContainer);
        }
    }

    /**
     * <p>
     * Fetches the sync setting id for the given email is
     * </p>
     *
     * @param emailId                Represents the email id
     * @param existingEmailContainer Represents the source container
     * @param targetContainer        Represents the target container
     * @return the sync id
     */
    private Long loadAndFetchSyncSettingsId(final Long emailId, final DataContainer existingEmailContainer, final DataContainer targetContainer) {
        final Table syncSettingsTable = getSyncSettingsTable();
        final WhereClause whereClause = new WhereClause(Criteria.eq(syncSettingsTable.getColumn(MEMSYNCSETTINGS.EMAIL_ID), emailId));
        final Row existingRow = existingEmailContainer.getRow(syncSettingsTable, whereClause);

        if (Objects.isNull(existingRow)) {
            final Row targetContainerRow = targetContainer.getRow(syncSettingsTable, whereClause);
            final Row newRow = Objects.isNull(targetContainerRow) ? new Row(syncSettingsTable) : targetContainerRow;

            if (newRow.isNewRow()) {
                final long currentTime = Instant.now().toEpochMilli();

                newRow.set(MEMSYNCSETTINGS.EMAIL_ID, emailId);
                newRow.set(MEMSYNCSETTINGS.CONNECTION_STATE, ConnectionStatus.INACTIVE.getId());
                newRow.set(MEMSYNCSETTINGS.IS_SYNC_ENABLED, false);
                newRow.set(MEMSYNCSETTINGS.LAST_SYNC_TIME, currentTime);
                newRow.set(MEMSYNCSETTINGS.SYNC_STATUS, SyncStatus.NEW.getId());
                newRow.set(MEMSYNCSETTINGS.CREATED_TIME, currentTime);

            }
            targetContainer.updateRow(newRow);
            return newRow.getPKValue();
        } else {
            targetContainer.updateRow(existingRow);
            return existingRow.getPKValue();
        }
    }

    /**
     * <p>
     * Processes the email synchronization by iterating over folder intervals, retrieving and processing
     * email intervals asynchronously, and updating the status of intervals until all are processed.
     * </p>
     *
     * @param syncContext Represents the {@link SyncContext}
     * @param syncJobType Refers to the sync job type
     */
    private void sync(final SyncContext syncContext, final SyncJobType syncJobType) {
        final Collection<FolderInterval> folderIntervals = syncContext.folderIntervals();

        try {

            folderIntervals.forEach(folderInterval -> {
                final String folderName = folderInterval.getFolder().name();
                final Collection<EmailInterval> intervals = folderInterval.getEmailIntervals();

                intervals.forEach(interval -> {
                    LOGGER.debug("Email sync: Retrieval folder {}: processing Interval({}, {})",
                            folderName, interval.startTime(), interval.endTime());
                    processData(folderName, interval, syncContext, syncJobType);

                    syncSettingsManager.updateDateConfigIntoContainer(syncContext.syncId(), folderName, interval);
                });
            });
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Email sync: Error occurred during email sync process: {}", exception);
            throw ErrorCode.get(EmailErrorCodes.EMAIL_RETRIEVE_FAILED, exception);
        }
    }

    /**
     * <p>
     * Processes the {@link EmailInterval} and publishes the data.
     * </p>
     *
     * @param folderName  Represents the folder name
     * @param interval    Represents the {@link EmailInterval}
     * @param syncContext Represents the {@link SyncContext}
     */
    private void processData(final String folderName, final EmailInterval interval, final SyncContext syncContext,
                             final SyncJobType syncJobType) {
        final SyncAddressInfo emailAddress = syncContext.syncAddressInfo();
        final Long startTime = interval.startTime();
        final Long endTime = interval.endTime();
        final EmailReceiverConfiguration emailReceiverConfiguration = new EmailReceiverConfiguration();
        final FolderType folderType = FolderType.valueOf(folderName);
        final Consumer<Collection<Email>> loadEmails = emails -> {
            if (Objects.nonNull(emails) && !emails.isEmpty()) {
                loadEmails(emailManager.getOrInitEmail(emailAddress.getEmailAddress()), emailReceiverConfiguration, syncJobType, syncContext, emails);
            }
        };

        emailReceiverConfiguration.setEmailIntervals(Map.of(folderType, interval));
        emailReceiverConfiguration.setSyncEmailAddress(emailAddress);
        emailReceiverConfiguration.setFolderTypes(List.of(folderType));

        LOGGER.debug("Email sync: Preparing to fetch mails from provider and load emails into db for folder '{}' with interval ({}, {})",
                folderName, startTime, endTime);

        final EmailProviderService emailProviderService = EmailProviderService.getService(emailAddress);
        final EmailProviderDataStore emailProviderDataStore = emailProviderService.receive(emailReceiverConfiguration);

        if (Objects.nonNull(emailProviderDataStore)) {
            loadEmails.accept(emailProviderDataStore.getEmails());

            while ((emailProviderDataStore.hasNextLink())
                    || (emailProviderDataStore instanceof GmailDataStore gmailDataStore && Objects.nonNull(gmailDataStore.getPageToken()))) {

                emailProviderService.loadEmailProviderData(emailProviderDataStore);
                loadEmails.accept(emailProviderDataStore.getEmails());
            }
        }

        LOGGER.debug("Email sync: Successfully fetch mails from provider and load emails into db for folder '{}' with interval ({}, {})",
                folderName, startTime, endTime);
    }

    /**
     * <p>
     * Loads and processes a collection of emails for synchronization.
     * </p>
     *
     * @param emailAddress               Represents the email address.
     * @param emailReceiverConfiguration The configuration for the email receiver.
     * @param syncJobType                The type of synchronization job (manual sync or auto-sync).
     * @param syncContext                The synchronization context containing metadata and state data.
     * @param emails                     The collection of emails to be processed.
     */
    protected void loadEmails(final EmailAddress emailAddress, final EmailReceiverConfiguration emailReceiverConfiguration,
                              final SyncJobType syncJobType, final SyncContext syncContext, final Collection<Email> emails) {
        final Collection<Thread> threads = new HashSet<>();
        final DataContainer targetContainer = DataContainer.create();
        final SyncAddressInfo address = emailReceiverConfiguration.getSyncEmailAddress();

        try {
            LOGGER.debug("Email sync: Going to load the provider message into container");

            loadRetrievedMails(emailAddress, address, syncJobType, emails, threads, targetContainer);
            adminDataStore.commitChanges(targetContainer);

            LOGGER.debug("Email sync: Provider message loaded and committed successfully");
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Email sync: Error occurred during data loading into database: {}", exception.getMessage());
            throw ErrorCode.get(EmailErrorCodes.DB_LOAD_ACTION_FAILED, exception);
        }
        emailReceiverConfiguration.setSyncEmailAddress(address);
        final SyncRequest syncRequest = new SyncRequest(emailReceiverConfiguration);

        syncRequest.setStateData(syncContext.stateData());
        syncRequest.setId(syncContext.syncId());

        if (!threads.isEmpty()) {
            switch (syncJobType) {
                case SYNC -> syncRequestPublisher.publish(syncRequest, threads);
                case AUTO_SYNC -> {
                    final ThreadHolder threadHolder = new ThreadHolder();

                    threadHolder.setThreads(threads);
                    threadHolder.setStateData(syncContext.stateData());
                    threadHolder.setSyncedEmailAddress(address);
                    final EmailEventRequest emailEventRequest = new EmailEventRequest();

                    emailEventRequest.setReceiverConfiguration(emailReceiverConfiguration);
                    emailEventRequest.setCreateEvent(new EmailCreateEvent(threadHolder));

                    emailEventRequestPublisher.publish(emailEventRequest);
                }
            }
            LOGGER.debug("Email sync: Provider side retrieved messages loaded and published successfully");
        }
    }

    /**
     * <p>
     * Processes a collection of retrieved emails, categorizing them into appropriate folder types
     * (e.g., SENT, INBOX, DRAFT, ARCHIVE) and loading their data into the target container.
     * </p>
     *
     * @param emails          A collection of {@link Email} objects representing the retrieved emails.
     * @param threads         Represents the {@link Collection} of {@link Thread}
     * @param targetContainer The {@link DataContainer} to store the processed email data.
     */
    private void loadRetrievedMails(final EmailAddress emailAddress, final SyncAddressInfo syncAddressInfo, final SyncJobType syncJobType, final Collection<Email> emails, final Collection<Thread> threads,
                                    final DataContainer targetContainer) {

        if (!emails.isEmpty()) {
            final Collection<String> messageIds = emails.stream().map(Email::getMetaData).map(EmailMetaData::getMessageId).toList();
            final Map<String, Long> messageIdCount = referenceManager.getMessageIdsCount(messageIds);
            final Collection<Email> existingEmails = new ArrayList<>();
            final boolean autoSync = syncJobType.isAutoSync();

            for (final Email email : emails) {
                final MessageCategory messageCategory = email.getMessageCategory();
                final FolderType folderType = getFolderType(messageCategory);
                final String messageId = email.getMetaData().getMessageId();
                final boolean isArchived = messageCategory.isArchive();
                final long messageCount = messageIdCount.getOrDefault(messageId, 0L);
                final Long emailId = emailAddress.id();

                if (Objects.nonNull(folderType)) {
                    if (messageCount > 0) {
                        if (referenceManager.isMessageRowExists(emailId, email)) {
                            if (isArchived) {
                                threadManager.updateMessageState(messageId, targetContainer);
                            }

                            if (autoSync && (messageCategory.isDraft() || isDraftMessage(emailId, email))) {
                                handleEmail(emailAddress, false, email, threads, targetContainer, false);
                            } else {
                                existingEmails.add(email);
                            }
                        } else {
                            handleEmail(emailAddress, true, email, threads, targetContainer, isArchived);
                        }
                    } else if (messageCount == 0) {
                        handleEmail(emailAddress, true, email, threads, targetContainer, isArchived);
                    }
                }
            }

            if (!existingEmails.isEmpty()) {
                loadExistingEmails(emailAddress.id(), existingEmails, threads);
            }
        }
    }

    /**
     * Checks whether the given email is categorized as a draft for the provided synced email ID.
     *
     * @param emailId represent the email id.
     * @param email   The {@link Email} object to check.
     * @return {@code true} if the email is a draft, otherwise {@code false}.
     */
    private boolean isDraftMessage(final Long emailId, final Email email) {
        final Optional<MessageCategory> optionalMessageCategory = threadManager.getMessageCategory(emailId, email);

        return optionalMessageCategory.isPresent() && optionalMessageCategory.get() == MessageCategory.DRAFT;
    }

    /**
     * <p>
     * Handles the logic for processing a new email that needs to be uploaded and possibly archived.
     * </p>
     *
     * @param emailAddress    The ID of the synced email.
     * @param isCreateEvent   Represents whether it is created event or not.
     * @param email           The email object to be processed.
     * @param threads         The collection of threads associated with the email.
     * @param targetContainer The target container where the email will be stored.
     * @param isArchived      A flag indicating whether the email should be marked as archived.
     */
    private void handleEmail(final EmailAddress emailAddress, final boolean isCreateEvent, final Email email, final Collection<Thread> threads,
                             final DataContainer targetContainer, final boolean isArchived) {
        if (isArchived) {
            email.setArchived(true);
        }

        conversationAttachmentHandler.upload(emailAddress.id(), email);
        processRetrievedEmails(emailAddress, isCreateEvent, email, threads, targetContainer);
    }

    /**
     * <p>
     * Loads the conversation threads associated with the emails that already exist in the system.
     * </p>
     *
     * @param emailId        Represent the email id.
     * @param existingEmails The collection of emails already loaded or present in the system.
     * @param threads        The collection to which corresponding existing threads will be added.
     */
    private void loadExistingEmails(final Long emailId, final Collection<Email> existingEmails, final Collection<Thread> threads) {
        final Collection<String> messageIds = existingEmails.stream()
                .map(Email::getMetaData)
                .map(EmailMetaData::getMessageId)
                .collect(Collectors.toSet());
        final Collection<Thread> existingThreads = messageIds.isEmpty() ? Collections.emptyList() : threadManager.getThreadsByEmailId(emailId, false, messageIds);

        if (!existingThreads.isEmpty()) {
            threads.addAll(existingThreads);
        }
    }

    /**
     * Processes a retrieved email and associates it with an existing or new thread and conversation.
     *
     * @param emailAddress    Represents the email address.
     * @param isCreateEvent   Represents whether it is created event or not.
     * @param email           The {@link Email} containing the email details.
     * @param threads         Represents the {@link Collection} of {@link Thread}
     * @param targetContainer The {@link DataContainer} where the email, thread, and conversation data will be stored or updated.
     */
    private void processRetrievedEmails(final EmailAddress emailAddress, final boolean isCreateEvent, final Email email,
                                        final Collection<Thread> threads, final DataContainer targetContainer) {
        final Thread thread = new Thread();
        final EmailMetaData metaData = email.getMetaData();
        final String referencesHeader = metaData.getReferences();
        final String conversationThreadId = metaData.getConversationThreadId();
        final Long emailId = emailAddress.id();

        thread.setEmails(List.of(email));

        if (isCreateEvent) {
            if (Objects.isNull(conversationThreadId) && Objects.isNull(referencesHeader)) {
                threadManager.loadThreadIntoContainer(emailAddress, thread, null, targetContainer);
            } else {
                final Collection<String> referenceAddresses = new HashSet<>();

                if (Objects.nonNull(referencesHeader)) {
                    final Collection<String> references = EmailReferenceFormatter.splitReferences(referencesHeader);
                    final Collection<String> ipAddressReferences = references.stream().map(EmailReferenceFormatter::formatIpReferences).toList();

                    referenceAddresses.addAll(references);
                    referenceAddresses.addAll(ipAddressReferences);
                }
                final Optional<Long> existingThreadId = threadManager.getThreadId(emailId, conversationThreadId, referenceAddresses);

                if (existingThreadId.isEmpty()) {
                    threadManager.loadThreadBasedOnReferences(emailAddress, thread, referenceAddresses, targetContainer);
                } else {
                    final Long threadId = existingThreadId.get();
                    final DataContainer dataContainer = threadManager.getDataContainer(threadId);

                    thread.setId(threadId);
                    threadManager.loadThreadIntoContainer(emailAddress, thread, dataContainer, targetContainer);
                }
            }
        } else {
            final Table externalMessageTable = referenceManager.getExternalMessageTable();
            final String messageId = metaData.getMessageId();
            final DataContainer dataContainer = threadManager.getContainerByMessageId(emailId, messageId);

            final WhereClause whereClause = new WhereClause(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.MESSAGE_ID),
                    messageId)).and(Criteria.eq(externalMessageTable.getColumn(MEMEXTERNALMESSAGE.EMAIL_ID), emailId));
            final Optional<Row> optionalExternalMessageRow = Optional.ofNullable(dataContainer.getRow(externalMessageTable, whereClause))
                    .or(() -> Optional.ofNullable(targetContainer.getRow(externalMessageTable, whereClause)));

            optionalExternalMessageRow.ifPresent(externalMessageRow -> {
                final Long conversationId = externalMessageRow.get(MEMEXTERNALMESSAGE.CONVERSATION_ID);

                email.setId(conversationId);
                final Table conversationTable = getConversationsTable();

                final Optional<Row> optionalConversationRow = Optional.ofNullable(dataContainer.getRow(conversationTable, conversationId))
                        .or(() -> Optional.ofNullable(targetContainer.getRow(conversationTable, conversationId)));

                optionalConversationRow.ifPresent(conversationRow -> {
                    thread.setId(conversationRow.get(MEMCONVERSATION.THREAD_ID));
                });
            });
            threadManager.loadThreadIntoContainer(emailAddress, thread, dataContainer, targetContainer);
        }

        thread.setHasDrafts(email.isDraft());
        thread.setArchived(email.isArchived());
        thread.setStarred(email.isStarred());

        threads.add(thread);
    }

    /**
     * <p>
     * Gets the {@link FolderType} for the given {@link Email.MessageCategory}.
     * </p>
     *
     * @param messageCategory Represents the  {@link Email.MessageCategory}
     * @return The {@link Email.MessageCategory}
     */
    private FolderType getFolderType(final Email.MessageCategory messageCategory) {
        return switch (messageCategory) {
            case INBOUND -> INBOX;
            case OUTBOUND -> SENT;
            case DRAFT -> DRAFT;
            case ARCHIVE -> ARCHIVE;
            default -> null;
        };
    }

    /**
     * <p>
     * Gets the {@link Table} of email address.
     * </p>
     *
     * @return email address {@link Table}
     */
    private Table getAddressTable() {
        return Table.get(MEMADDRESS.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of thread table.
     * </p>
     *
     * @return email address {@link Table}
     */
    private Table getThreadTable() {
        return Table.get(MEMTHREAD.TABLE);
    }

    /**
     * <p>
     * Gets the {@link Table} of conversations.
     * </p>
     *
     * @return email address {@link Table}
     */
    private Table getConversationsTable() {
        return Table.get(MEMCONVERSATION.TABLE);
    }

    /**
     * <p>
     * Updates the existing updatedEmails
     * </p>
     *
     * @param emailAddress   The {@link EmailAddress} of the synced account
     * @param receiverConfig The receiver configuration used for this sync
     * @param syncContext    The sync context carrying sync ID and address info
     * @param updatedEmails         The updatedEmails fetched from the provider that need updating
     */
    protected void updateExistingEmails(final EmailAddress emailAddress, final EmailReceiverConfiguration receiverConfig,
                                        final SyncContext syncContext, final Collection<Email> updatedEmails) {
        final Collection<Thread> threads = new HashSet<>();
        final DataContainer targetContainer = DataContainer.create();
        final SyncAddressInfo address = receiverConfig.getSyncEmailAddress();

        try {
            for (final Email updatedEmail : updatedEmails) {
                handleEmail(emailAddress, false, updatedEmail, threads, targetContainer, updatedEmail.isArchived());
            }
            adminDataStore.commitChanges(targetContainer);
        } catch (final Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Usynced email load: Error occurred during email update into database: {}", exception.getMessage());
            throw ErrorCode.get(EmailErrorCodes.DB_LOAD_ACTION_FAILED, exception);
        }

        if (!threads.isEmpty()) {
            final ThreadHolder threadHolder = new ThreadHolder();

            threadHolder.setThreads(threads);
            threadHolder.setStateData(syncContext.stateData());
            threadHolder.setSyncedEmailAddress(address);
            final EmailEventRequest emailEventRequest = new EmailEventRequest();

            receiverConfig.setSyncEmailAddress(address);
            emailEventRequest.setReceiverConfiguration(receiverConfig);
            emailEventRequest.setUpdateEvent(new EmailUpdateEvent(threadHolder));
            emailEventRequestPublisher.publish(emailEventRequest);
        }
    }

    /**
     * <p>
     * Gets the {@link Table} of sync settings.
     * </p>
     *
     * @return sync settings {@link Table}
     */
    private Table getSyncSettingsTable() {
        return Table.get(MEMSYNCSETTINGS.TABLE);
    }
}