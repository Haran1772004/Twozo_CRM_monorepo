package com.tlc.crm.email.core.internal.impl;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.crm.common.event.model.EventData;
import com.tlc.crm.common.event.model.SecureEventData;
import com.tlc.crm.common.iam.SecureUserTaskProcessor;
import com.tlc.crm.common.models.Action;
import com.tlc.crm.core.api.contact.manager.ContactManager;
import com.tlc.crm.core.api.contact.manager.LifeCycleStatusManager;
import com.tlc.crm.core.api.contact.model.Contact;
import com.tlc.crm.core.api.contact.model.converter.ContactConverter;
import com.tlc.crm.core.internal.event.contact.publisher.ContactCreatedEventHandler;
import com.tlc.crm.core.notification.email.api.manager.CoreMailManager;
import com.tlc.crm.core.notification.email.api.model.ContactRecipients;
import com.tlc.crm.core.notification.email.api.model.EntityThread;
import com.tlc.crm.core.notification.email.api.model.email.EmailVariant;
import com.tlc.crm.core.notification.email.api.model.email.EntityEmail;
import com.tlc.crm.core.notification.email.api.model.email.EntityOutboundEmail;
import com.tlc.crm.core.notification.email.settings.service.EmailSettingsService;
import com.tlc.crm.core.sql.resource.MCOCONTACT;
import com.tlc.crm.core.workflow.internal.WorkflowEmailTrackerManager;
import com.tlc.crm.core.workflow.internal.WorkflowEmailPublisher;
import com.tlc.crm.email.core.EmailLinkerService;
import com.tlc.crm.email.core.InboundEmailLinkerService;
import com.tlc.crm.email.core.OutboundEmailLinkerService;
import com.tlc.crm.email.core.fields.EmailLinkerFields;
import com.tlc.crm.email.core.manager.EmailLinkerManager;
import com.tlc.crm.email.notification.email.api.manager.EmailReferenceManager;
import com.tlc.crm.email.notification.email.api.model.address.UserBlockedEmailAddress;
import com.tlc.crm.email.notification.email.api.model.config.EmailSyncConfig;
import com.tlc.crm.email.notification.email.api.model.settings.EmailLinkerSettings;
import com.tlc.crm.email.notification.email.sync.api.EmailSyncManager;

import com.tlc.crm.email.notification.email.sync.api.model.EmailSyncSettings;
import com.tlc.crm.email.notification.sql.resource.OMEMEXTERNALMESSAGE;
import com.tlc.crm.email.notification.sql.resource.OMEMCONVERSATIONS;
import com.tlc.crm.fields.sql.resource.OMETEMAIL;
import com.tlc.crm.notifications.internal.event.NotificationEventListener;
import com.tlc.email.common.codec.ThreadCodec;
import com.tlc.email.common.model.ConsentType;
import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.StateData;
import com.tlc.email.common.model.SyncJobType;
import com.tlc.email.common.model.Thread;
import com.tlc.email.common.model.ThreadHolder;
import com.tlc.email.common.model.address.AddressType;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.EmailRecipients;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.config.TenantConfiguration;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.model.type.InboundEmail;
import com.tlc.email.common.model.type.OutboundEmail;
import com.tlc.email.common.publish.EmailRequestPublisher;
import com.tlc.email.common.publish.EmailTenantSyncRequestPublisher;
import com.tlc.email.common.request.TenantSyncRequest;
import com.tlc.email.common.model.formatter.EmailReferenceFormatter;
import com.tlc.email.common.model.type.Email.MessageCategory;
import com.tlc.file.common.models.Attachment;
import com.tlc.notification.model.sync.ConnectionStatus;
import com.tlc.notification.model.sync.SyncStatus;
import com.tlc.sql.common.api.DataContainer;
import com.tlc.sql.common.api.Row;
import com.tlc.sql.common.api.dml.Criteria;
import com.tlc.sql.common.api.dml.Table;
import com.tlc.sql.common.api.dml.WhereClause;
import com.tlc.sync.service.oauth.model.provider.EmailProvider;
import com.tlc.sync.service.oauth.service.EmailSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tlc.crm.email.notification.email.api.model.config.EmailSyncConfig.ConfigState.INACTIVE;
import static com.tlc.email.common.model.config.FolderType.ARCHIVE;
import static com.tlc.email.common.model.config.FolderType.DRAFT;
import static com.tlc.email.common.model.config.FolderType.INBOX;
import static com.tlc.email.common.model.config.FolderType.SENT;
import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;

/**
 * <p>
 * Provides the mail related services
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailLinkerServiceImpl implements EmailLinkerService, SecureUserTaskProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailLinkerServiceImpl.class);

    private final EmailReferenceManager emailReferenceManager;
    private final EmailSyncManager emailSyncManager;
    private final EmailLinkerManager emailLinkerManager;
    private final EmailSettingsService emailSettingsService;
    private final CoreMailManager coreMailManager;
    private final OutboundEmailLinkerService outboundEmailLinkerService;
    private final InboundEmailLinkerService inboundEmailLinkerService;
    private final ContactManager contactManager;
    private final EmailRequestPublisher<TenantSyncRequest> tenantEmailRequestPublisher;
    private final ContactConverter contactConverter;
    private final LifeCycleStatusManager lifeCycleStatusManager;
    private final WorkflowEmailTrackerManager workflowEmailTrackerManager;
    private final WorkflowEmailPublisher workflowEmailPublisher;
    private final ContactCreatedEventHandler contactCreatedEventHandler;

    private EmailLinkerServiceImpl() {
        emailReferenceManager = EmailReferenceManager.getInstance();
        emailSyncManager = EmailSyncManager.getInstance();
        emailLinkerManager = EmailLinkerManager.getInstance();
        emailSettingsService = EmailSettingsService.getInstance();
        coreMailManager = CoreMailManager.getInstance();
        outboundEmailLinkerService = OutboundEmailLinkerService.getInstance();
        inboundEmailLinkerService = InboundEmailLinkerService.getInstance();
        contactManager = ContactManager.getInstance();
        tenantEmailRequestPublisher = EmailTenantSyncRequestPublisher.getInstance();
        contactConverter = ContactConverter.getInstance();
        lifeCycleStatusManager = LifeCycleStatusManager.getInstance();
        workflowEmailTrackerManager = WorkflowEmailTrackerManager.getInstance();
        workflowEmailPublisher = WorkflowEmailPublisher.getInstance();
        contactCreatedEventHandler = ContactCreatedEventHandler.getInstance();
    }

    private static class Instance {
        private static final EmailLinkerServiceImpl INSTANCE = new EmailLinkerServiceImpl();
    }

    public static EmailLinkerServiceImpl getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void syncSmartBccAccount() {
        final EmailSyncService emailSyncService = EmailProvider.OUTLOOK.getSyncService();
        final Map<String, com.tlc.sync.service.oauth.model.EmailAddress> accounts = emailSyncService.fetchAccounts();

        for (final Map.Entry<String, com.tlc.sync.service.oauth.model.EmailAddress> entry : accounts.entrySet()) {
            final TenantConfiguration tenantConfiguration = new TenantConfiguration();
            final SyncAddressInfo emailAddress = new SyncAddressInfo(entry.getValue().getEmailId());
            final JsonObject stateData = Json.object();

            emailAddress.setConsentType(ConsentType.TENANT);
            tenantConfiguration.setSyncEmailAddress(emailAddress);
            tenantConfiguration.setFolderTypes(List.of(INBOX));
            tenantConfiguration.setEmailUserId(entry.getKey());
            stateData.put(EmailLinkerFields.IS_TENANT, true);

            tenantEmailRequestPublisher.publish(new TenantSyncRequest.Builder(tenantConfiguration).stateData(new StateData(stateData)).build());
        }
    }

    /**
     * <p>
     * Loads the retrieved mails into database
     * </p>
     *
     * @param syncJobType  Represents the sync job type
     * @param syncSettings Represents the collection of {@link EmailSyncSettings}
     * @param threadHolder Represents the {@link ThreadHolder}
     */
    public void loadRetrievedEmails(final SyncJobType syncJobType, final Collection<EmailSyncSettings> syncSettings,
                                    final ThreadHolder threadHolder) {
        final boolean autoSync = syncJobType.isAutoSync();

        for (final EmailSyncSettings syncSetting : syncSettings) {

            if (Objects.nonNull(syncSetting)) {
                final EmailAddress emailAddress = syncSetting.getAddress();
                final long orgId = syncSetting.orgId();
                final Long userId = syncSetting.getUserId();
                final Long syncId = syncSetting.id();

                try {
                    secureProcess(orgId, userId, () -> {
                        final Collection<EntityEmail> notifiableMails = new ArrayList<>();
                        final long startTime = System.currentTimeMillis();
                        LOGGER.debug("Email sync: Retrieved emails from the provider are ready to load into database, orgId: {}, userId: {}", orgId, userId);
                        final Collection<Thread> threads = ThreadCodec.getInstance()
                                .cloneThreads(threadHolder.getThreads());

                        for (final Thread thread : threads) {

                            for (final Email email : thread.getEmails()) {

                                if (Optional.ofNullable(email.getSender()).isEmpty()) {
                                    email.setSender(new SyncAddressInfo(emailAddress.id(), emailAddress.getEmailAddress()));
                                }
                            }
                        }

                        final DataContainer targetContainer = DataContainer.create();
                        final EmailSyncConfig emailSyncConfig = syncSetting.getEmailSyncConfig();
                        final short relatedContactState = emailSyncConfig.getRelatedContactState();
                        final Collection<EmailAddress> emailAddresses = relatedContactState == ConnectionStatus.ACTIVE.getId() ?
                                emailSyncManager.getEmailAddressesByOrgId(orgId) : List.of();
                        final DataContainer entityContainer = emailReferenceManager.getMessageIdContainer(orgId, userId);

                        loadRetrievedMails(orgId, userId, syncId, threads, relatedContactState, emailAddresses,
                                entityContainer, targetContainer, syncSetting, notifiableMails, syncJobType);
                        emailSyncManager.loadSyncedEmailIntoContainer(syncJobType, syncSetting, targetContainer);
                        contactCreatedEventHandler.triggerEvents(orgId, userId, contactManager.getContactIds(targetContainer));
                        LOGGER.debug("Email sync: Retrieved emails are successfully loaded into database in {} milliseconds, orgId: {}, userId: {}, syncId: {}",
                                System.currentTimeMillis() - startTime, orgId, userId, syncId);

                        if (autoSync && !notifiableMails.isEmpty()) {
                            final EventData eventData = new SecureEventData(orgId, userId);

                            eventData.setValues(notifiableMails);
                            eventData.setAction(Action.CREATE);
                            NotificationEventListener.getInstance().processPostEvent(eventData);
                        }
                        LOGGER.debug("Email sync: Email successfully synchronized in {} milliseconds, orgId: {}, userId: {}, syncId: {}",
                                System.currentTimeMillis() - startTime, orgId, userId, syncId);

                    });
                } catch (final Throwable throwable) {
                    LoggerUtil.logError(LOGGER, throwable, "Email sync: Failed occurred while loading email data into the container Org ID: {}, User ID, Sync ID: {}", orgId, userId, syncId);

                    throw ErrorCode.get(com.tlc.crm.email.notification.email.status.EmailErrorCodes.FAILED_TO_LOAD_EMAILS, throwable);
                }
            }
        }
    }

    /**
     * <p>
     * Returns the folder ID based on the email's message category.
     * </p>
     *
     * @param messageCategory The category of the email message
     * @return The corresponding folder ID, or null if no match is found
     */
    private Short getFolderId(final Email.MessageCategory messageCategory) {
        return switch (messageCategory) {
            case INBOUND -> INBOX.getId();
            case OUTBOUND -> SENT.getId();
            case DRAFT -> DRAFT.getId();
            case ARCHIVE -> ARCHIVE.getId();
            default -> null;
        };
    }

    /**
     * <p>
     * Loads the retrieved emails for an organization id
     * </p>
     *
     * @param orgId               Represents the org id
     * @param userId              Represents the user id
     * @param syncId              Represents the sync id
     * @param relatedContactState Represents the related contact state
     * @param emailAddresses      Which contains the collection of {@link EmailAddress}
     * @param entityContainer     Holds entities relevant to the emails being processed
     * @param targetContainer     Holds target entities where the processed emails will be stored
     * @param emailSyncSettings   Represents the {@link EmailSyncSettings}
     * @param notifiableMails     Which contains the collection of {@link EntityEmail}
     * @param syncJobType         Represents the sync job type
     */
    private void loadRetrievedMails(final Long orgId, final Long userId, final Long syncId, final Collection<Thread> threads,
                                    final short relatedContactState, final Collection<EmailAddress> emailAddresses,
                                    final DataContainer entityContainer, final DataContainer targetContainer,
                                    final EmailSyncSettings emailSyncSettings, final Collection<EntityEmail> notifiableMails,
                                    final SyncJobType syncJobType) {
        final Table messageTable = emailReferenceManager.getExternalMessageTable();
        final Collection<Short> folderIds = emailSyncSettings.getEmailSyncConfig().getFolderIds();
        final boolean autoSync = syncJobType.isAutoSync();
        final Collection<String> blockedAddresses = emailSettingsService.getBlockedAddresses(orgId, userId).stream()
                .map(UserBlockedEmailAddress::getEmailId).filter(Objects::nonNull).map(EmailReferenceFormatter::removeAsterisks).collect(Collectors.toSet());

        if (!threads.isEmpty()) {

            for (final Thread thread : threads) {
                final Collection<Email> emails = thread.getEmails();

                for (final Email composeEmail : emails) {
                    final Optional<Short> optionalFolderId = Optional.ofNullable(getFolderId(composeEmail.getMessageCategory()));

                    if (optionalFolderId.isPresent() && folderIds.contains(optionalFolderId.get())) {
                        final Email email = composeEmail.get();
                        final Collection<Attachment> attachments = getAttachments(email.getAttachments());
                        final FolderType folderType = getFolderType(email.getMessageCategory());

                        if (Objects.nonNull(folderType) && (relatedContactState == INACTIVE.getId() || isEmailAddressRelatedToRecipients(email, emailAddresses))) {
                            final boolean isPermittedEmail = isPermittedEmail(email, blockedAddresses);
                            final EmailMetaData metaData = email.getMetaData();
                            final Predicate<Email.MessageCategory> isMessageRowPresent = category -> Objects.isNull(entityContainer.getRow(messageTable,
                                    new WhereClause(Criteria.eq(messageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), metaData.id()))
                                            .and(Criteria.eq(messageTable.getColumn(OMEMEXTERNALMESSAGE.USER_ID), userId))));

                            switch (folderType) {
                                case SENT -> {

                                    if (isMessageRowPresent.test(OUTBOUND)) {
                                        processRetrievedEmails(orgId, userId, syncId, true, attachments, OUTBOUND,
                                                thread, targetContainer, email);
                                    } else if (autoSync && isExistingMessageDraft(orgId, userId, email)) {
                                        processRetrievedEmails(orgId, userId, syncId, false, attachments, OUTBOUND,
                                                thread, targetContainer, email);
                                    }
                                }
                                case INBOX -> {

                                    if (isMessageRowPresent.test(INBOUND)) {

                                        if (isPermittedEmail) {
                                            processRetrievedEmails(orgId, userId, syncId, true, attachments, INBOUND,
                                                    thread, targetContainer, email);
                                        }

                                        if (isPermittedEmail && (autoSync || SyncStatus.SYNCED == emailSyncSettings.getSyncStatus())) {
                                            final Table conversationTable = coreMailManager.getConversationsTable();
                                            final Row conversationRow = targetContainer.getRow(conversationTable, new WhereClause(Criteria.eq(conversationTable.getColumn(OMEMCONVERSATIONS.CONVERSATION_ID), email.id())));

                                            if (Objects.nonNull(conversationRow)) {
                                                final EntityEmail entityEmail = new EntityEmail();

                                                entityEmail.setOrgId(orgId);
                                                entityEmail.setUserId(userId);
                                                entityEmail.setSubject(email.getSubject());
                                                entityEmail.setSender(email.getSender());
                                                entityEmail.setMetaData(metaData);
                                                entityEmail.setCrmId(conversationRow.getPKValue());

                                                notifiableMails.add(entityEmail);
                                            }
                                        }
                                    }
                                }
                                case DRAFT -> {
                                    if (isMessageRowPresent.test(Email.MessageCategory.DRAFT)) {
                                        processRetrievedEmails(
                                                orgId, userId, syncId, true,
                                                attachments, OUTBOUND, thread, targetContainer, email
                                        );
                                    } else if (autoSync) {
                                        processRetrievedEmails(
                                                orgId, userId, syncId, false,
                                                attachments, OUTBOUND, thread, targetContainer, email
                                        );
                                    }
                                }
                                case ARCHIVE -> {
                                    final WhereClause whereClause = new WhereClause(Criteria.eq(messageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_ID), metaData.id()));
                                    final Row row = entityContainer.getRow(messageTable, whereClause);

                                    if (Objects.isNull(row)) {
                                        email.setArchived(true);

                                        if (Objects.equals(emailSyncSettings.getAddress().getEmailAddress(), email.getSender().getEmailAddress())) {
                                            processRetrievedEmails(orgId, userId, syncId, true, attachments, OUTBOUND,
                                                    thread, targetContainer, email);
                                        } else if (isPermittedEmail) {
                                            processRetrievedEmails(orgId, userId, syncId, true, attachments, INBOUND,
                                                    thread, targetContainer, email);
                                        }
                                    } else {
                                        final Row messageIdRow = entityContainer.getRow(messageTable, whereClause.and(Criteria.in(
                                                messageTable.getColumn(OMEMEXTERNALMESSAGE.MESSAGE_CATEGORY), List.of(OUTBOUND.getId(), INBOUND.getId()))));

                                        if (Objects.nonNull(messageIdRow)) {
                                            coreMailManager.updateMessageState(orgId, userId, row.get(OMEMEXTERNALMESSAGE.CONVERSATION_ID), targetContainer);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * <p>
     * Determines whether the given {@link Email} requires an update based on its message category.
     * An update is required if the email itself is marked as a draft or if the persisted category is a draft.
     * </p>
     *
     * @param orgId  The organization ID.
     * @param userId The user ID associated with the email.
     * @param email  The {@link Email} to evaluate.
     * @return {@code true} if the email is or was categorized as a draft and needs to be updated; otherwise, {@code false}.
     */
    private boolean isExistingMessageDraft(final Long orgId, final Long userId, final Email email) {
        final Optional<MessageCategory> optionalMessageCategory = coreMailManager.getMessageCategory(orgId, userId, email);

        return optionalMessageCategory.isPresent() && MessageCategory.DRAFT == optionalMessageCategory.get();
    }

    /**
     * <p>
     * Gets the collection of {@link Attachment} from the collection of {@link EmailAttachment}
     * </p>
     *
     * @param emailAttachments Represents the collection of {@link EmailAttachment}
     * @return the collection of {@link Attachment}
     */
    private Collection<Attachment> getAttachments(final Collection<EmailAttachment> emailAttachments) {
        final Collection<Attachment> attachments = new ArrayList<>();

        if (Objects.nonNull(emailAttachments)) {
            for (final EmailAttachment emailAttachment : emailAttachments) {
                final Attachment attachment = new Attachment();

                attachment.setId(emailAttachment.id());
                attachment.setFileName(emailAttachment.getFileName());
                attachment.setFileSize(emailAttachment.getFileSize());
                attachment.setMediaType(emailAttachment.getMediaType());
                attachment.setExtension(emailAttachment.getExtension());
                attachment.setInputStream(emailAttachment.getInputStream());

                attachments.add(attachment);
            }
        }

        return attachments;
    }

    /**
     * <p>
     * Gets the {@link FolderType} for the given {@link Email.MessageCategory}
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
     * Checks the retrieve mails email addresses are related to the recipients
     * </p>
     *
     * @param email          Represents the {@link EntityEmail}
     * @param emailAddresses Represents the collection of {@link EmailAddress}
     */
    private boolean isEmailAddressRelatedToRecipients(final Email email,
                                                      final Collection<EmailAddress> emailAddresses) {
        if (Objects.isNull(emailAddresses) || emailAddresses.isEmpty()) {
            return false;
        }
        final Collection<String> relatedAddresses = emailAddresses.stream().map(EmailAddress::getEmailAddress)
                .map(this::normalizeEmailAddress).filter(Objects::nonNull).collect(Collectors.toSet());
        final EmailRecipients recipients = email.getEmailRecipients();

        if (Objects.isNull(recipients)) {
            return isRelatedAddress(email.getSender(), relatedAddresses);
        }

        return Stream.of(recipients.toRecipients(), recipients.ccRecipients(), recipients.bccRecipients())
                       .filter(Objects::nonNull).flatMap(Collection::stream).filter(Objects::nonNull).anyMatch(recipient ->
                        relatedAddresses.contains(normalizeEmailAddress(recipient.getEmailAddress()))) || isRelatedAddress(email.getSender(), relatedAddresses);
    }

    /**
     * <p>
     * Checks whether the given address is present in the related addresses
     * </p>
     *
     * @param address          Represents the {@link EmailAddress} to verify, may be null
     * @param relatedAddresses Represents the normalized related addresses
     * @return true, if the address is related
     */
    private boolean isRelatedAddress(final EmailAddress address, final Collection<String> relatedAddresses) {
        return Objects.nonNull(address) && relatedAddresses.contains(normalizeEmailAddress(address.getEmailAddress()));
    }

    /**
     * <p>
     * Normalizes the email address by trimming the whitespace and converting it to lower case
     * </p>
     *
     * @param emailAddress Represents the email address to normalize
     * @return the normalized email address, or null if the address or its value is null
     */
    private String normalizeEmailAddress(final String emailAddress) {
        return Objects.isNull(emailAddress) ? null : emailAddress.trim().toLowerCase();
    }

    /**
     * <p>
     * Checks whether the email sender is blocked or not and permits if unblocked
     * </p>
     *
     * @param email            Represents the {@link Email}
     * @param blockedAddresses Represents the blocked addresses
     * @return true, if not blocked
     */
    public boolean isPermittedEmail(final Email email, final Collection<String> blockedAddresses) {
        if (Objects.nonNull(blockedAddresses) && (blockedAddresses.stream().anyMatch(emailAddress ->
                email.getSender().getEmailAddress().contains(emailAddress)))) {

            return !email.getMetaData().isInitialMessage();
        }

        return true;
    }

    /**
     * <p>
     * Processes the retrieved emails for a given organization and user
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents a user id
     * @param syncId          Represents the sync id
     * @param attachments     Represents the {@link Attachment}
     * @param messageCategory Represents the {@link Email.MessageCategory}
     * @param thread          Represents the {@link Email}
     * @param targetContainer Holds the updated information of email defined
     */
    private void processRetrievedEmails(final long orgId, final long userId, final Long syncId, final boolean isCreateEvent,
                                        final Collection<Attachment> attachments, final Email.MessageCategory messageCategory,
                                        final Thread thread, final DataContainer targetContainer, final Email email) {
        final DataContainer entityContainer = emailLinkerManager.getDataContainer(orgId, userId, thread.id(), false);
        final EntityThread entityThread = composeThread(orgId, userId, syncId, thread, attachments, email, targetContainer);

        emailLinkerManager.loadThreadAndConversationId(entityThread, entityContainer, targetContainer);

        switch (messageCategory) {
            case OUTBOUND ->
                    outboundEmailLinkerService.loadRetrievedMails(isCreateEvent, entityThread, entityContainer, targetContainer);
            case INBOUND -> {
                inboundEmailLinkerService.loadRetrievedMails(entityThread, entityContainer, targetContainer);

                try {
                    final EntityEmail entityEmail = entityThread.getComposedEmail();
                    final EmailMetaData metaData = entityEmail.getMetaData();
                    final boolean isInitialMessage = metaData.isInitialMessage();
                    final Long threadId = entityEmail.getCrmId();
                    final boolean updatedInitialMessage = workflowEmailTrackerManager.updateInitialMessage(orgId, threadId, isInitialMessage);

                    if (updatedInitialMessage) {
                        workflowEmailPublisher.publish(entityEmail.getConversationId(), threadId, null, isInitialMessage);
                    }
                } catch (Exception exception) {
                    LOGGER.error("Email: Failed occurred at the inbound service. OrgID: {}, Reason: {}", orgId, exception.getMessage(), exception);
                }
            }
        }
    }

    /**
     * <p>
     * Composes a new thread based on retrieve email message data
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param syncId          Represents the sync id
     * @param attachments     Which contains the {@link Collection} of attachment
     * @param targetContainer Represents the target container
     * @return the {@link EntityThread}
     */
    private EntityThread composeThread(final long orgId, final long userId, final Long syncId, final Thread thread,
                                       final Collection<Attachment> attachments, final Email email,
                                       final DataContainer targetContainer) {
        final EntityThread entityThread = new EntityThread();

        if (Objects.nonNull(email)) {
            final Email.MessageCategory messageCategory = email.getMessageCategory();
            final EntityEmail entityEmail = EntityEmail.getMailBasedOnCategory(messageCategory);
            final boolean archived = email.isArchived();

            if (archived) {
                email.setMessageCategory(messageCategory);
            }

            if (Objects.nonNull(attachments) && !attachments.isEmpty()) {
                entityEmail.setEntityAttachments(attachments);
            }
            entityEmail.setVariant(new EmailVariant(syncId, EmailVariant.Type.SYNC_EMAIL));

            if (messageCategory == INBOUND && email instanceof InboundEmail inboundEmail) {
                entityEmail.setRead(inboundEmail.isRead());
            }
            final EmailRecipients emailRecipients = email.getEmailRecipients();
            final ContactRecipients contactRecipients = new ContactRecipients();

            if (Objects.nonNull(emailRecipients)) {
                final Map<String, Contact> contacts = getContacts(orgId, emailRecipients.getAllEmailAddresses(), targetContainer);
                final Consumer<AddressType> contactRecipientsConsumer = (addressType) -> {
                    final Map<AddressType, Collection<EmailAddress>> addressMap = emailRecipients.get();

                    if (Objects.nonNull(addressMap)) {
                        final Collection<EmailAddress> emailAddresses = addressMap.get(addressType);

                        if (Objects.nonNull(emailAddresses) && !emailAddresses.isEmpty()) {
                            final Collection<String> addresses = emailAddresses.stream().map(EmailAddress::getEmailAddress).filter(Objects::nonNull).collect(Collectors.toSet());

                            for (final String address : addresses) {
                                if (contacts.containsKey(address)) {
                                    contactRecipients.addContact(addressType, contacts.get(address));
                                }
                            }
                        }
                    }
                };
                contactRecipientsConsumer.accept(AddressType.TO);
                contactRecipientsConsumer.accept(AddressType.BCC);
                contactRecipientsConsumer.accept(AddressType.CC);

                if (contactRecipients.hasRecipients()) {
                    entityEmail.setContactRecipients(contactRecipients);
                }
                entityEmail.setRecipients(List.of(emailRecipients));
            }

            entityEmail.setId(email.id());
            entityEmail.setOrgId(orgId);
            entityEmail.setUserId(userId);
            entityEmail.setContent(email.getContent());
            entityEmail.setSubject(email.getSubject());
            entityEmail.setSender(email.getSender());
            entityEmail.setMessageCategory(messageCategory);
            entityEmail.setMetaData(email.getMetaData());
            entityEmail.setArchived(email.isArchived());
            entityEmail.setSyncedState(true);

            if (messageCategory == OUTBOUND) {
                final EntityOutboundEmail outboundEmail = entityEmail.get();

                outboundEmail.setOutboundType(OutboundEmail.OutboundType.SEND);
            }
            entityThread.setEntityEmails(List.of(entityEmail));
        }
        entityThread.setOrgId(orgId);
        entityThread.setUserId(userId);
        entityThread.setHasAttachments(thread.hasAttachments());
        entityThread.setHasDrafts(thread.hasDrafts());
        entityThread.setHasActiveMessages(thread.hasActiveMessages());
        entityThread.setArchived(thread.isArchived());
        entityThread.setStarred(thread.isStarred());
        entityThread.setId(thread.id());

        return entityThread;
    }

    /**
     * <p>
     * Retrieves a map of {@link Contact} instances for the given collection of email addresses
     * by combining results from the database and the provided {@link DataContainer}.
     * </p>
     *
     * @param orgId           The organization ID used to scope the database lookup
     * @param emailAddresses  The collection of email address strings to resolve
     * @param targetContainer The {@link DataContainer} used as a fallback for emails not found in the database
     * @return a {@link Map} of email address strings.
     */

    private Map<String, Contact> getContacts(final Long orgId, final Collection<String> emailAddresses, final DataContainer targetContainer) {
        final Map<String, Contact> contacts = contactManager.fetchContactsByEmails(orgId, emailAddresses);
        final Collection<String> notInDbEmails = emailAddresses.stream()
                .filter(email -> !contacts.containsKey(email))
                .collect(Collectors.toSet());

        if (!notInDbEmails.isEmpty() && !targetContainer.isEmpty()) {
            contacts.putAll(contactManager.getContacts(notInDbEmails, targetContainer));
        }

        return contacts;
    }

    /**
     * <p>
     * Creates the new contact for the non-existing contact which points to the respective email.
     * </p>
     *
     * @param orgId           Represents an organization id
     * @param userId          Represents the user id
     * @param entityEmail     Represents the {@link EntityEmail}
     * @param isInbound       Indicates whether the email is inbound (only the sender is created as contact)
     *                        or outbound (TO, CC and BCC recipients are created as contacts)
     * @param targetContainer Holds the updated record information into the target container
     */
    public void createNewContactForRecipients(final Long orgId, final Long userId, final EntityEmail entityEmail,
                                              final boolean isInbound, final DataContainer targetContainer) {
        final EmailRecipients recipients = entityEmail.getEmailRecipients();
        final Map<AddressType, Collection<EmailAddress>> addressTypeCollectionMap =
                Optional.ofNullable(recipients)
                        .map(EmailRecipients::get)
                        .orElseGet(Collections::emptyMap);
        final Map<AddressType, Collection<String>> emailAddresses = new EnumMap<>(AddressType.class);
        final BiConsumer<AddressType, Collection<EmailAddress>> mapEmailAddressesToType = (addressType, addresses) -> {
            if (Objects.nonNull(addresses) && !addresses.isEmpty()) {
                emailAddresses.put(addressType, addresses.stream().map(EmailAddress::getEmailAddress).toList());
            }
        };

        if (isInbound) {
            final EmailAddress sender = entityEmail.getSender();

            if (Objects.nonNull(sender) && Objects.nonNull(sender.getEmailAddress())) {
                emailAddresses.put(AddressType.FROM, List.of(sender.getEmailAddress()));
            }
        } else {
            mapEmailAddressesToType.accept(AddressType.TO, addressTypeCollectionMap.get(AddressType.TO));
            mapEmailAddressesToType.accept(AddressType.BCC, addressTypeCollectionMap.get(AddressType.BCC));
            mapEmailAddressesToType.accept(AddressType.CC, addressTypeCollectionMap.get(AddressType.CC));
        }

        final EmailLinkerSettings emailLinkerSettings = emailSettingsService.getEmailLinkerSettings(orgId, userId);
        final boolean isContactCreationEnabled = Objects.nonNull(emailLinkerSettings) && emailLinkerSettings.isContactCreationEnabled();
        final Map<AddressType, Collection<String>> filteredEmails = new HashMap<>();

        if (isContactCreationEnabled) {
            final Map<AddressType, Collection<String>> newEmails = contactManager.getUnassociatedEmails(orgId, emailAddresses);

            if (!newEmails.isEmpty()) {
                final Table emailTable = coreMailManager.getContactEmailAddressTable();
                final Table contactTable = contactManager.getTable();

                for (final Map.Entry<AddressType, Collection<String>> addresses : newEmails.entrySet()) {
                    final Collection<String> emails = addresses.getValue();
                    final AddressType addressType = addresses.getKey();

                    if (!emails.isEmpty()) {
                        final Collection<String> mailAddresses = new HashSet<>();
                        final Map<String, Row> emailRows = targetContainer.getRows(emailTable, new WhereClause(Criteria.in(emailTable.getColumn(OMETEMAIL.VALUE), emails)))
                                .collect(Collectors.toMap(row -> row.get(OMETEMAIL.VALUE), Function.identity()));

                        if (emailRows.isEmpty()) {
                            mailAddresses.addAll(emails);
                        } else {
                            emails.forEach(emailAddress -> {
                                final Row emailRow = emailRows.get(emailAddress);

                                if (Objects.nonNull(emailRow)) {
                                    final Long contactEmailId = emailRow.get(emailTable.getPKColumn());

                                    if (Objects.nonNull(contactEmailId)) {
                                        final ContactRecipients contactRecipients = entityEmail.getContactRecipients();

                                        if (Objects.nonNull(contactRecipients)) {
                                            contactRecipients.addContact(addressType, contactEmailId);
                                        }
                                        final Optional<Row> optionalContactRow = Optional.ofNullable(targetContainer.getRow(contactTable,
                                                new WhereClause(Criteria.eq(contactTable.getColumn(MCOCONTACT.PRIMARY_EMAIL), contactEmailId))));

                                        if (optionalContactRow.isEmpty()) {
                                            mailAddresses.add(emailAddress);
                                        }
                                    } else {
                                        mailAddresses.add(emailAddress);
                                    }
                                }
                            });
                        }

                        if (!mailAddresses.isEmpty()) {
                            filteredEmails.put(addressType, mailAddresses);
                        }
                    }
                }
            }

            if (!filteredEmails.isEmpty()) {
                final Long lifecycleStageId = emailLinkerSettings.getLifecycleStageId();
                final Optional<Long> optionalLifecycleStatusId = Optional.ofNullable(emailLinkerSettings.getLifecycleStatusId());

                final ContactRecipients contactRecipients = contactConverter.buildContactsFromEmailAddresses(orgId, userId, filteredEmails,
                        lifecycleStageId, optionalLifecycleStatusId.orElseGet(() -> lifeCycleStatusManager.getDefaultStatusId(orgId, lifecycleStageId)), entityEmail);

                contactManager.createContactsUsingEmailAndLifeCycleStage(contactRecipients, targetContainer);
            }
        }
    }
}