package com.tlc.email.server.internal.provider.jakarta;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.EmailStateTransitionInfo;
import com.tlc.email.common.model.address.SyncAddressInfo;
import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.config.EmailReceiverConfiguration;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.type.DraftEmail;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.common.request.UpdateRequest;
import com.tlc.email.common.service.EmailTrackingService;
import com.tlc.email.server.internal.manager.MailBoxConfManager;
import com.tlc.email.server.internal.protocols.imap.ImapClient;
import com.tlc.email.server.internal.protocols.models.MailBoxConfiguration;
import com.tlc.email.server.internal.provider.jakarta.models.EmailFolder;
import com.tlc.email.server.internal.provider.jakarta.provider.JakartaImapClientFactory;
import com.tlc.email.server.internal.provider.jakarta.provider.JakartaSmtpClientFactory;
import com.tlc.email.server.model.EmailProviderDataStore;
import com.tlc.email.server.service.EmailProviderService;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.tlc.email.common.model.type.Email.MessageCategory.INBOUND;
import static com.tlc.email.common.model.type.Email.MessageCategory.OUTBOUND;


/**
 * <p>
 * Service implementation for sending, receiving, updating, and deleting emails
 * using SMTP and IMAP protocols via Jakarta connection helpers.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class JakartaMailService implements EmailProviderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JakartaMailService.class);

    private static int BATCH_SIZE;

    private final MailBoxConfManager mailBoxConfManager;
    private final JakartaImapClientFactory jakartaImapConnectionHelper;
    private final JakartaSmtpClientFactory jakartaSmtpConnectionHelper;
    private final EmailTrackingService emailTrackingService;

    private static class Instance {
        private static final JakartaMailService INSTANCE = new JakartaMailService();
    }

    /**
     * <p>
     * Gets the singleton instance of {@link JakartaMailService}.
     * </p>
     *
     * @return the {@link JakartaMailService} instance
     */
    public static JakartaMailService getInstance() {
        return JakartaMailService.Instance.INSTANCE;
    }

    /**
     * <p>
     * Private constructor initializing mailbox manager and connection helpers.
     * Also sets the default batch size for message retrieval.
     * </p>
     */
    private JakartaMailService() {
        this.mailBoxConfManager = MailBoxConfManager.getInstance();
        this.jakartaSmtpConnectionHelper = JakartaSmtpClientFactory.getInstance();
        this.jakartaImapConnectionHelper = JakartaImapClientFactory.getInstance();
        this.emailTrackingService = EmailTrackingService.getInstance();

        BATCH_SIZE = 10;
    }

    /**
     * <p>
     * Sends an email via SMTP or saves it as a draft via IMAP based on its category.
     * </p>
     *
     * @param email the {@link Email} object to be sent or drafted
     */
    @Override
    public void send(final Email email) {
        final Optional<MailBoxConfiguration> optionalMailBoxConfiguration = mailBoxConfManager.get(email.getSender().getEmailAddress());

        if (optionalMailBoxConfiguration.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_CONFIGURATION_NOT_FOUND);
        }
        final MailBoxConfiguration mailBoxConfiguration = optionalMailBoxConfiguration.get();

        if (email.getMessageCategory().isDraft()) {
            final ImapClient imapClient = jakartaImapConnectionHelper.createClient(mailBoxConfiguration);
            final DraftEmail.DraftAction draftAction = ((DraftEmail) email).getDraftAction();
            final EmailMetaData emailMetaData = email.getMetaData();

            switch (draftAction) {
                case CREATE -> draft(email, imapClient);
                case UPDATE -> {
                    try (final Store store = imapClient.getStore()) {

                        imapClient.delete(
                                new ImapFolderResolver.Builder()
                                        .store(store)
                                        .build().getFolderName(EmailFolder.DRAFT), List.of(emailMetaData.getMessageId())
                        );
                        draft(email, imapClient);
                    } catch (Exception exception) {
                        throw ErrorCode.get(EmailErrorCodes.DRAFT_ACTION_FAILED);
                    }
                }
                case SEND -> {
                    try (final Store store = imapClient.getStore()) {
                        final String messageId = emailMetaData.getMessageId();
                        final String folderName = new ImapFolderResolver.Builder()
                                .store(store)
                                .build().getFolderName(EmailFolder.DRAFT);

                        if (Objects.nonNull(messageId) && Objects.nonNull(imapClient.getMessage(messageId))) {
                            imapClient.delete(folderName, List.of(messageId));
                        }
                        jakartaSmtpConnectionHelper.createClient(mailBoxConfiguration).send(email);
                    } catch (Exception exception) {
                        throw ErrorCode.get(EmailErrorCodes.EMAIL_SEND_FAILED);
                    }
                }
            }
        } else {
            jakartaSmtpConnectionHelper.createClient(mailBoxConfiguration).send(email);
        }
    }

    /**
     * <p>
     * Performs the delete action for the provided email and message ids.
     * </p>
     *
     * @param emailStateTransitionInfo Represent the email sate transition info.
     * @param emailAddress          Refers the email address.
     */
    @Override
    public void delete(final EmailStateTransitionInfo emailStateTransitionInfo, final String emailAddress) {
        final String id = emailStateTransitionInfo.getOldMessageId();
        final Optional<MailBoxConfiguration> optionalMailBoxConfiguration = mailBoxConfManager.get(emailAddress);

        if (optionalMailBoxConfiguration.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_CONFIGURATION_NOT_FOUND);
        }
        final MailBoxConfiguration mailBoxConfiguration = optionalMailBoxConfiguration.get();
        final ImapClient imapClient = jakartaImapConnectionHelper.createClient(mailBoxConfiguration);

        try (final Store store = imapClient.getStore()) {
            imapClient.delete(
                    new ImapFolderResolver.Builder()
                            .store(store)
                            .build().getFolderName(get(emailStateTransitionInfo.getMessageCategory())), List.of(id));
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_DELETE_FAILED);
        }
    }

    /**
     * <p>
     * Updates email states in the mailbox, moving them to appropriate folders.
     * </p>
     *
     * @param updateRequest            the {@link UpdateRequest} object
     * @param emailStateTransitionInfo a collection of {@link EmailStateTransitionInfo} representing state changes
     */
    @Override
    public void update(final UpdateRequest updateRequest, final Collection<EmailStateTransitionInfo> emailStateTransitionInfo) {
        final SyncAddressInfo syncAddressInfo = updateRequest.getSyncEmailAddress();
        final Optional<MailBoxConfiguration> optionalMailBoxConfiguration = mailBoxConfManager.get(syncAddressInfo.getEmailAddress());

        if (optionalMailBoxConfiguration.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_CONFIGURATION_NOT_FOUND);
        }
        final MailBoxConfiguration mailBoxConfiguration = optionalMailBoxConfiguration.get();
        final Email.State state = emailStateTransitionInfo.stream().map(EmailStateTransitionInfo::getNewState).findFirst().orElseGet(() -> null);

        final Collection<String> inboundMessageIds = new ArrayList<>();
        final Collection<String> outboundMessageIds = new ArrayList<>();
        final ImapClient imapClient = jakartaImapConnectionHelper.createClient(mailBoxConfiguration);

        for (final EmailStateTransitionInfo currentMessageTransition : emailStateTransitionInfo) {
            final Email.MessageCategory messageCategory = currentMessageTransition.getMessageCategory();
            final String messageId = currentMessageTransition.getOldMessageId();

            switch (messageCategory) {
                case INBOUND -> inboundMessageIds.add(messageId);
                case OUTBOUND -> outboundMessageIds.add(messageId);
            }
        }

        if (Objects.nonNull(state)) {

            if (!inboundMessageIds.isEmpty()) {
                update(inboundMessageIds, resolveSourceFolder(state, INBOUND),
                        resolveDestinationFolder(state, INBOUND), state, imapClient);
            }

            if (!outboundMessageIds.isEmpty()) {
                update(outboundMessageIds, resolveSourceFolder(state, OUTBOUND),
                        resolveDestinationFolder(state, OUTBOUND), state, imapClient);
            }
            updateRequest.setState(state);
        }
    }

    /**
     * <p>
     * Loads emails from the provider data store in batches based on {@link #BATCH_SIZE}.
     * </p>
     *
     * @param dataStore the {@link EmailProviderDataStore} to populate
     */
    @Override
    public void loadEmailProviderData(final EmailProviderDataStore dataStore) {
        final Message[] messages = dataStore.getMessages();

        if (Objects.isNull(messages) || messages.length == 0) {
            dataStore.setEmails(Collections.emptySet());
            dataStore.setNextLink(false);

            try {
                dataStore.getFolder().close();
                dataStore.getStore().close();

                return;
            } catch (Exception exception) {
                LoggerUtil.logError(LOGGER, exception, "Jakarta: Folder and store close action failed, message: {}, trace: {}", exception.getMessage(), exception);

                throw ErrorCode.get(EmailErrorCodes.EMAIL_SEND_FAILED);
            }
        }
        final List<Message> remainingMessages = new ArrayList<>(Arrays.asList(messages));
        final Collection<Message> batchMessages = new ArrayList<>(remainingMessages.subList(0, Math.min(BATCH_SIZE, remainingMessages.size())));

        remainingMessages.removeAll(batchMessages);

        dataStore.setMessages(remainingMessages.toArray(new Message[0]));
        dataStore.setNextLink(!remainingMessages.isEmpty());
        final ImapClient imapClient = dataStore.getImapClient();
        final Collection<Email> emails = imapClient.getEmails(batchMessages.toArray(new Message[0]), dataStore.getStore(), dataStore.getFolder());

        dataStore.setEmails(getMessages(imapClient, getFolder(dataStore.getFolderType()), emails));
    }

    /**
     * <p>
     * Retrieves emails for a given {@link EmailConfiguration}.
     * </p>
     *
     * @param configuration the {@link EmailConfiguration} representing mailbox settings
     * @return a populated {@link EmailProviderDataStore}
     */
    @Override
    public EmailProviderDataStore receive(final EmailConfiguration configuration) {
        final SyncAddressInfo emailAddress = configuration.getSyncEmailAddress();
        final EmailReceiverConfiguration emailReceiverConfiguration = configuration.get();
        final EmailProviderDataStore emailProviderDataStore = new EmailProviderDataStore();
        final FolderType folderType = emailReceiverConfiguration.getFirstFolderType();
        final Optional<MailBoxConfiguration> optionalMailBoxConfiguration = mailBoxConfManager.get(emailAddress.id());

        if (optionalMailBoxConfiguration.isEmpty()) {
            throw ErrorCode.get(EmailErrorCodes.EMAIL_CONFIGURATION_NOT_FOUND);
        }
        final MailBoxConfiguration mailBoxConfiguration = optionalMailBoxConfiguration.get();
        final ImapClient imapClient = jakartaImapConnectionHelper.createClient(mailBoxConfiguration);

        emailProviderDataStore.setIncludeMessageIdsOnly(configuration.isIncludeMessageIds());
        emailProviderDataStore.setFolderType(folderType);
        emailProviderDataStore.setImapClient(imapClient);

        return getRetrievedMessages(emailProviderDataStore, imapClient, folderType, emailReceiverConfiguration);
    }

    /**
     * <p>
     * Creates a draft email by populating the tracking ID and saving the email
     * in the draft folder using the provided IMAP client.
     * </p>
     *
     * @param email      The email to be saved as a draft
     * @param imapClient The IMAP client used to store the draft email
     */
    private void draft(final Email email, final ImapClient imapClient) {
        emailTrackingService.populateTrackingId(email);
        imapClient.draft(email);
    }


    /**
     * <p>
     * Maps {@link Email.MessageCategory} to the corresponding {@link EmailFolder}.
     * </p>
     *
     * @param messageCategory the category of the email
     * @return the corresponding {@link EmailFolder}
     */
    private EmailFolder get(final Email.MessageCategory messageCategory) {
        return switch (messageCategory) {
            case INBOUND -> EmailFolder.INBOX;
            case OUTBOUND -> EmailFolder.SENT;
            case DRAFT -> EmailFolder.DRAFT;
            case ARCHIVE -> EmailFolder.ARCHIVE;
            default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_ACTION);
        };
    }

    /**
     * <p>
     * Retrieves emails from the specified folder using the given IMAP client.
     * </p>
     *
     * @param imapClient  The IMAP client used to fetch the messages
     * @param emailFolder The folder from which messages are retrieved
     * @param emails      Represent the emails to be fetched
     * @return the collection of retrieved {@link Email} messages
     */
    private Collection<Email> getMessages(final ImapClient imapClient, final EmailFolder emailFolder, final Collection<Email> emails) {

        if (emailFolder.isDraft()) {

            for (final Email email : emails) {
                final String content = email.getContent();
                final Optional<String> optionalTrackingId = emailTrackingService.getTrackingId(content);

                if (optionalTrackingId.isPresent()) {
                    email.getMetaData().setTrackingId(optionalTrackingId.get());
                } else {
                    imapClient.delete(
                            new ImapFolderResolver.Builder()
                                    .store(imapClient.getStore())
                                    .build().getFolderName(EmailFolder.DRAFT),
                            List.of(email.getMetaData().getMessageId())
                    );
                    draft(email, imapClient);
                }
            }
        }

        return emails;
    }

    /**
     * <p>
     * Retrieves email messages from specified folders
     * </p>
     *
     * @param emailProviderDataStore Represents the email
     * @param imapClient             Represents the {@link ImapClient}
     * @param folderType             Represents the folder type
     * @param configuration          Refers to the {@link EmailConfiguration}
     * @return the {@link Collection} of {@link Email}
     */
    private EmailProviderDataStore getRetrievedMessages(final EmailProviderDataStore emailProviderDataStore, final ImapClient imapClient,
                                                        final FolderType folderType, final EmailReceiverConfiguration configuration) {
        final Map<FolderType, EmailInterval> emailIntervalMap = configuration.getEmailIntervals();

        if (emailIntervalMap.containsKey(folderType)) {
            final Store store = imapClient.getStore();
            final Folder folder = imapClient.getFolder(store, getFolder(folderType));

            emailProviderDataStore.setStore(store);
            emailProviderDataStore.setFolder(folder);
            emailProviderDataStore.setMessages(imapClient.getAllMessages(folder, getFolder(folderType), emailIntervalMap.get(folderType)));
        }

        if (emailProviderDataStore.includeMessageIdsOnly()) {
            return emailProviderDataStore;
        }
        loadEmailProviderData(emailProviderDataStore);

        return emailProviderDataStore;
    }

    /**
     * <p>
     * Maps {@link FolderType} to {@link EmailFolder}.
     * </p>
     *
     * @param folderType the folder type
     * @return the corresponding {@link EmailFolder}
     */
    private EmailFolder getFolder(final FolderType folderType) {
        return switch (folderType) {
            case INBOX -> EmailFolder.INBOX;
            case SENT -> EmailFolder.SENT;
            case DRAFT -> EmailFolder.DRAFT;
            case ARCHIVE -> EmailFolder.ARCHIVE;
        };
    }

    /**
     * <p>
     * Updates the messages based on the folder selection and state
     * </p>
     *
     * @param messageIds        Represents the message ids
     * @param sourceFolder      Represents the source folder
     * @param destinationFolder Represents the destination folder
     * @param state             Represents the state to be updated
     * @param imapClient        Represents the IMAP client
     */
    private void update(final Collection<String> messageIds, final EmailFolder sourceFolder,
                        final EmailFolder destinationFolder, final Email.State state, final ImapClient imapClient) {
        if (Objects.equals(state, Email.State.ARCHIVE) || Objects.equals(state, Email.State.ACTIVE) ||
                Objects.equals(state, Email.State.UNARCHIVE) || Objects.equals(state, Email.State.INACTIVE)) {
            imapClient.update(sourceFolder, destinationFolder, messageIds);
        } else {
            throw ErrorCode.get(EmailErrorCodes.ARCHIVE_DELETION_FAILED);
        }
    }

    /**
     * <p>
     * Resolves the source folder based on the email state and message category.
     * </p>
     *
     * @param state           the target state for the update operation
     * @param messageCategory the message category (INBOUND / OUTBOUND)
     * @return the source {@link EmailFolder} to use for the IMAP update
     */
    private EmailFolder resolveSourceFolder(final Email.State state, final Email.MessageCategory messageCategory) {
        return switch (state) {
            case UNARCHIVE -> EmailFolder.ARCHIVE;
            case ACTIVE -> EmailFolder.TRASH;
            case ARCHIVE, INACTIVE -> EmailFolder.getCategoryBasedFolder(messageCategory);
            default -> throw ErrorCode.get(EmailErrorCodes.FOLDER_NOT_FOUND);
        };
    }

    /**
     * <p>
     * Resolves the target folder based on the email state and message category.
     * </p>
     *
     * @param state           the target state for the update operation
     * @param messageCategory the message category (INBOUND / OUTBOUND)
     * @return the target {@link EmailFolder} to use for the IMAP update
     */
    private EmailFolder resolveDestinationFolder(final Email.State state, final Email.MessageCategory messageCategory) {
        return switch (state) {
            case ARCHIVE -> EmailFolder.ARCHIVE;
            case INACTIVE -> EmailFolder.TRASH;
            case UNARCHIVE, ACTIVE -> EmailFolder.getCategoryBasedFolder(messageCategory);
            default -> throw ErrorCode.get(EmailErrorCodes.FOLDER_NOT_FOUND);
        };
    }
}