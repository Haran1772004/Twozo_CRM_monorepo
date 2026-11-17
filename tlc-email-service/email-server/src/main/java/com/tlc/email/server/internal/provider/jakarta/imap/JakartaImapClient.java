package com.tlc.email.server.internal.provider.jakarta.imap;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.util.LoggerUtil;
import com.tlc.email.common.internal.status.EmailErrorCodes;
import com.tlc.email.common.model.EmailMetaData;
import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.internal.converter.EmailMimeMessageConverter;
import com.tlc.email.server.internal.protocols.AuthCredentials;
import com.tlc.email.server.internal.protocols.imap.ImapClient;
import com.tlc.email.server.internal.protocols.models.AuthType;
import com.tlc.email.server.internal.protocols.models.BasicAuthCredentials;
import com.tlc.email.server.internal.protocols.models.MessageFlagsInfo;
import com.tlc.email.server.internal.provider.jakarta.ImapFolderResolver;
import com.tlc.email.server.internal.provider.jakarta.models.EmailFolder;
import com.tlc.email.server.model.Protocol;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.HeaderTerm;
import jakarta.mail.search.MessageIDTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.ReceivedDateTerm;
import jakarta.mail.search.SearchTerm;
import org.eclipse.angus.mail.imap.IMAPStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * Implements {@link ImapClient} using Jakarta Mail API for IMAP-based operations.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class JakartaImapClient implements ImapClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(JakartaImapClient.class);
    private static final String AUTH = "AUTH=";
    private static final String MESSAGE_ID = "Message-ID";

    private final String host;
    private final Session session;
    private final AuthCredentials authCredentials;
    private final EmailMimeMessageConverter emailMimeMessageConverter;
    private final ImapFolderResolver imapFolderResolver;

    /**
     * <p>
     * Constructs a new JakartaImapClient with the given Jakarta Mail session.
     * </p>
     *
     * @param host            Represents the host
     * @param session         The {@link Session} configured for IMAP access.
     * @param authCredentials Represents the auth credentials
     */
    public JakartaImapClient(final String host, final Session session, final AuthCredentials authCredentials) {
        this.authCredentials = authCredentials;
        this.host = host;
        this.session = session;
        this.emailMimeMessageConverter = EmailMimeMessageConverter.getInstance();

        try (final Store store = getConnectedStore()) {
            this.imapFolderResolver = new ImapFolderResolver.Builder()
                    .store(store)
                    .build();
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.SERVER_CONNECT_ACTION_FAILED);
        }
    }

    /**
     * <p>
     * Creates and saves the given {@link Email} as a draft in the IMAP Drafts folder.
     * </p>
     *
     * @param email The {@link Email} to be saved as a draft.
     */
    @Override
    public void draft(final Email email) {

        try (final Store store = getConnectedStore()) {
            final MimeMessage message = emailMimeMessageConverter.convert(email, session);

            saveDraft(message, store);
            loadMetaData(email, message);
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to draft email: {}, trace: {}", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.DRAFT_ACTION_FAILED, exception);
        }
    }

    /**
     * <p>
     * Moves or updates messages from the specified source folder to the target folder
     * based on the provided message IDs.
     * </p>
     *
     * @param sourceFolder The {@link EmailFolder} from which the messages will be moved.
     * @param targetFolder The {@link EmailFolder} to which the messages will be moved.
     * @param messageIds   A collection of message IDs representing the emails to be updated.
     */
    @Override
    public void update(final EmailFolder sourceFolder, final EmailFolder targetFolder, final Collection<String> messageIds) {
        Store store = null;
        Folder currentFolder = null;
        Folder destinationFolder = null;

        try {
            store = getConnectedStore();
            currentFolder = store.getFolder(imapFolderResolver.getFolderName(sourceFolder));

            if (!currentFolder.isOpen()) {
                currentFolder.open(Folder.READ_WRITE);
            }
            destinationFolder = getFolderOrCreateFolder(store, targetFolder);
            final Message[] messages = search(currentFolder, messageIds);

            copyMessages(messages, currentFolder, destinationFolder);

            for (final Message message : messages) {
                setDeleteFlag(message);
            }
            currentFolder.expunge();
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to update email: {}, trace: {}", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.DRAFT_ACTION_FAILED, exception);
        } finally {

            try {
                if (Objects.nonNull(currentFolder) && currentFolder.isOpen()) {
                    currentFolder.close();
                }

                if (Objects.nonNull(destinationFolder) && destinationFolder.isOpen()) {
                    destinationFolder.close();
                }

                if (Objects.nonNull(store)) {
                    store.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * <p>
     * Deletes messages identified by the given message IDs.
     * </p>
     *
     * @param emailFolder Email folder name.
     * @param messageIds  Message identifiers to delete.
     */
    @Override
    public void delete(final String emailFolder, final Collection<String> messageIds) {
        Store store = null;
        Folder folder = null;

        try {
            store = getConnectedStore();
            folder = store.getFolder(emailFolder);

            if (!folder.exists()) {
                return;
            }
            folder.open(Folder.READ_WRITE);

            for (final Message message : search(folder, messageIds)) {
                message.setFlag(Flags.Flag.DELETED, true);
            }
            folder.expunge();
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to delete messages", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.DELETE_ACTION_FAILED, exception);
        } finally {

            try {
                if (Objects.nonNull(folder) && folder.isOpen()) {
                    folder.close();
                }

                if (Objects.nonNull(store)) {
                    store.close();
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * <p>
     * Retrieves all message IDs from the specified email folder that fall within
     * the given time interval.
     * </p>
     *
     * @param emailFolder   Represents the email folder to search.
     * @param emailInterval Represents the time interval used to filter messages.
     * @return A collection of {@code Message-ID} values matching the filter criteria.
     */
    @Override
    public Collection<String> getAllMessageIds(final EmailFolder emailFolder, final EmailInterval emailInterval) {
        return getMessages(emailFolder, getFilter(emailFolder, emailInterval));
    }

    /**
     * <p>
     * Retrieves all messages from the specified folder within the given time interval.
     * </p>
     *
     * @param folder        Represents the mail folder to fetch messages from.
     * @param emailFolder   Represents the email folder context.
     * @param emailInterval Represents the time interval used to filter messages.
     * @return An array of {@link Message} objects.
     */
    @Override
    public Message[] getAllMessages(final Folder folder, EmailFolder emailFolder, EmailInterval emailInterval) {
        return fetchMessages(folder, emailFolder, emailInterval);
    }

    /**
     * <p>
     * Converts the given array of {@link Message} objects into {@link Email} objects.
     * </p>
     *
     * @param messages An array of {@link Message} objects to be converted.
     * @param store    The connected {@link Store} instance.
     * @param folder   The folder containing the messages.
     * @return A collection of {@link Email} objects.
     */
    @Override
    public Collection<Email> getEmails(final Message[] messages, final Store store, final Folder folder) {
        final Collection<Email> emails = new HashSet<>();

        try {
            if (!folder.exists()) {
                return emails;
            }

            if (!folder.isOpen()) {
                folder.open(Folder.READ_ONLY);
            }

            for (final Message message : messages) {
                emails.add(emailMimeMessageConverter.convert((MimeMessage) message, store));
            }

        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to fetch messages", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.MESSAGE_FETCH_ACTION_FAILED, exception);
        }

        return emails;
    }

    /**
     * <p>
     * Retrieves all message from the specified email folder for the given message ids.
     * </p>
     *
     * @param emailFolder Represents the message ids to be fetched.
     * @param messageIds  Represents the time interval used to filter messages.
     * @return A collection of {@code Message-ID} values matching the filter criteria.
     */
    @Override
    public Collection<Email> getMessages(final EmailFolder emailFolder, final Collection<String> messageIds) {
        final Collection<Email> emptyResult = Collections.emptyList();

        if (Objects.isNull(messageIds) || messageIds.isEmpty()) {
            return emptyResult;
        }

        try (final Store store = getConnectedStore();
             final Folder folder = getFolder(store, emailFolder)) {

            if (!folder.exists()) {
                return emptyResult;
            }
            folder.open(Folder.READ_ONLY);

            final MessageIDTerm[] terms = messageIds.stream()
                    .map(MessageIDTerm::new)
                    .toArray(MessageIDTerm[]::new);

            final Message[] messages = folder.search(new OrTerm(terms));
            final Collection<Email> emails = new ArrayList<>(messages.length);

            for (final Message message : messages) {
                emails.add(emailMimeMessageConverter.convert((MimeMessage) message, store));
            }

            return emails;
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to fetch messages by message IDs", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.MESSAGE_FETCH_ACTION_FAILED, exception);
        }
    }

    /**
     * <p>
     * Opens and returns a connected IMAP {@link Store} instance using the configured session
     * and authentication credentials.
     * </p>
     *
     * @return A connected {@link Store} instance.
     */
    @Override
    public Store getStore() {

        try {
            return getConnectedStore();
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.SERVER_CONNECT_ACTION_FAILED);
        }
    }

    /**
     * <p>
     * Determines the authentication mechanisms supported by the specified mail server using the provided credentials.
     * </p>
     *
     * @param authCredentials Represents the credentials used to authenticate with the server.
     * @return A collection of {@link AuthType} values supported by the server.
     */
    @Override
    public Collection<AuthType> getAuthTypes(final String host, final int port, final AuthCredentials authCredentials) {
        final Collection<AuthType> authTypes = new HashSet<>();

        try {
            if (!(authCredentials instanceof BasicAuthCredentials credentials)) {
                throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_AUTH_TYPE);
            }

            try (final IMAPStore store = (IMAPStore) session.getStore(Protocol.IMAP.get())) {
                store.connect(host, port, credentials.username(), credentials.password());

                for (final AuthType authType : AuthType.values()) {

                    if (store.hasCapability(AUTH + authType.name())) {
                        authTypes.add(authType);
                    }
                }
            }
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta Mail: Failed to retrieve server auth data: {}", exception.getMessage());

            throw ErrorCode.get(EmailErrorCodes.FAIL_TO_RETRIEVE_SERVER_AUTH_DATA, exception);
        }

        return authTypes;
    }

    /**
     * <p>
     * Retrieves the email corresponding to the given Message-ID from the system folders.
     * </p>
     *
     * @param messageId The Message-ID of the email to fetch.
     * @return The {@link Email} object if found, otherwise {@code null}.
     */
    @Override
    public Email getMessage(final String messageId) {
        final SearchTerm searchTerm = new HeaderTerm(MESSAGE_ID, messageId);
        final Collection<String> supportedFolders = imapFolderResolver.getFolderNamesExcluding(List.of(EmailFolder.SPAM));

        try (final Store store = getConnectedStore()) {
            final Folder defaultFolder = store.getDefaultFolder();
            final Folder[] folders = defaultFolder.list("*");

            for (final Folder folder : folders) {

                if (!supportedFolders.contains(folder.getName())) {
                    continue;
                }

                if (!folder.isOpen()) {
                    folder.open(Folder.READ_ONLY);
                }
                final Message[] messages = folder.search(searchTerm);

                if (messages.length > 0) {
                    final MimeMessage message = (MimeMessage) messages[0];
                    final Email email = emailMimeMessageConverter.convert(message, store);

                    email.isActive(!Objects.equals(folder.getName(), imapFolderResolver.getFolderName(EmailFolder.TRASH)));

                    if (Objects.equals(folder.getName(), imapFolderResolver.getFolderName(EmailFolder.ARCHIVE))) {
                        email.setArchived(true);
                    }

                    return email;
                }
            }
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta Mail: Failed to fetch message for Message-ID {}", messageId);
        }

        return null;
    }

    /**
     * <p>
     * Retrieves the flag information for a specific message identified by Message-ID
     * within the given email folder.
     * </p>
     *
     * @param emailFolder The {@link EmailFolder} containing the message.
     * @param messageId   The Message-ID of the email to retrieve flags for.
     * @return The {@link MessageFlagsInfo} object containing flags, or {@code null} if not found.
     */
    @Override
    public MessageFlagsInfo getLabelInfo(final EmailFolder emailFolder, final String messageId) {
        MessageFlagsInfo messageFlagsInfo = null;

        try (final Store store = getConnectedStore();
             final Folder folder = getFolder(store, emailFolder)) {

            if (!folder.isOpen()) {
                folder.open(Folder.READ_ONLY);
            }
            final SearchTerm searchTerm = new HeaderTerm(MESSAGE_ID, messageId);
            final Message[] messages = folder.search(searchTerm);

            if (messages.length == 0) {
                return null;
            }
            messageFlagsInfo = new MessageFlagsInfo(messages[0].getFlags());
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to get message for the message Id: {}, trace: {}", exception.getMessage(), exception);
        }

        return messageFlagsInfo;
    }

    /**
     * <p>
     * Retrieves message IDs from the given folder within the specified time interval.
     * </p>
     *
     * @param folder        Represents the mail folder to fetch messages from.
     * @param emailFolder   Represents the email folder context.
     * @param emailInterval Represents the time interval used to filter messages.
     * @return A collection of Message-ID values.
     */

    @Override
    public Collection<String> getMessageIds(Folder folder, EmailFolder emailFolder, EmailInterval emailInterval) {
        final Message[] messages = getAllMessages(folder, emailFolder, emailInterval);
        final Collection<String> messageIds = new HashSet<>();

        try {
            for (final Message message : messages) {
                messageIds.add(((MimeMessage) message).getMessageID());
            }
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_CONNECTION_CONFIGURATION);
        }

        return messageIds;
    }

    /**
     * <p>
     * Retrieves the {@link Folder} for the given {@link EmailFolder}.
     * </p>
     *
     * @param store       The connected {@link Store}.
     * @param emailFolder The enum representing the folder to retrieve.
     * @return The {@link Folder} instance.
     */
    @Override
    public Folder getFolder(final Store store, final EmailFolder emailFolder) {
        try {
            Objects.requireNonNull(store);
            Objects.requireNonNull(emailFolder);

            return store.getFolder(imapFolderResolver.getFolderName(emailFolder));
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.INVALID_CONNECTION_CONFIGURATION);

        }
    }

    /**
     * <p>
     * Tests the connectivity to the email server using the provided configuration.
     * This method actually establishes a connection to verify credentials and server accessibility.
     * </p>
     *
     * @return {@code true} if the connection was successful; {@code false} otherwise
     */
    @Override
    public boolean testConnection() {

        try {
            getConnectedStore().close();

            return true;
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * <p>
     * Searches the specified email folder using the given IMAP search criteria
     * and returns the Message-IDs of all matching emails.
     * </p>
     *
     * @param emailFolder Represents the email folder to search.
     * @param searchTerm  Represents the IMAP search filter.
     * @return A collection of Message-ID strings for emails that match the specified search criteria.
     */

    private Collection<String> getMessages(final EmailFolder emailFolder, final SearchTerm searchTerm) {
        final Collection<String> messageIds = new ArrayList<>();

        try (
                final Store store = getConnectedStore();
                final Folder folder = store.getFolder(imapFolderResolver.getFolderName(emailFolder))
        ) {
            folder.open(Folder.READ_ONLY);
            final Message[] messages = folder.search(searchTerm);

            for (final Message message : messages) {
                messageIds.add(((MimeMessage) message).getMessageID());
            }
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to fetch messages", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.MESSAGE_FETCH_ACTION_FAILED, exception);
        }

        return messageIds;
    }

    /**
     * <p>
     * Builds an IMAP {@link SearchTerm} based on the provided email folder
     * and time interval.
     * </p>
     *
     * @param emailFolder   Represents the email folder context.
     * @param emailInterval Represents the time interval for filtering messages.
     * @return A composed {@link SearchTerm} for IMAP search operations.
     */
    private SearchTerm getFilter(final EmailFolder emailFolder, final EmailInterval emailInterval) {
        final SearchTerm searchTerm = new AndTerm(
                new ReceivedDateTerm(ComparisonTerm.GE, new Date(emailInterval.startTime())),
                new ReceivedDateTerm(ComparisonTerm.LE, new Date(emailInterval.endTime()))
        );

        if (emailFolder.isDraft()) {
            return new AndTerm(new FlagTerm(new Flags(Flags.Flag.DRAFT), false), searchTerm);
        }

        return searchTerm;
    }

    /**
     * <p>
     * Sets the flag for deleting message
     * </p>
     *
     * @param message Refers to the message
     */
    private void setDeleteFlag(final Message message) {

        try {
            if (!message.isExpunged()) {
                message.setFlag(Flags.Flag.DELETED, true);
            }
        } catch (Exception exception) {
            throw ErrorCode.get(EmailErrorCodes.MESSAGE_DELETION_FAILED, exception);
        }
    }

    /**
     * <p>
     * Copies the messages from source folder to destination folder
     * </p>
     *
     * @param messages          Represents the collection of email messages
     * @param sourceFolder      Represents the source folder
     * @param destinationFolder Represents the destination folder
     */
    private void copyMessages(final Message[] messages, final Folder sourceFolder, final Folder destinationFolder) {
        try {
            sourceFolder.copyMessages(messages, destinationFolder);
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to copy messages", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.MESSAGE_COPY_ACTION_FAILED, exception);
        }
    }

    /**
     * <p>
     * Saves the given {@link MimeMessage} as a draft in the IMAP Drafts folder.
     * </p>
     *
     * @param message The {@link MimeMessage} to be saved.
     * @param store   The connected {@link Store} instance.
     */
    private void saveDraft(final MimeMessage message, final Store store) {

        try (final Folder draftFolder = store.getFolder(imapFolderResolver.getFolderName(EmailFolder.DRAFT))) {

            if (!draftFolder.exists()) {
                draftFolder.create(Folder.HOLDS_MESSAGES);
            }
            draftFolder.open(Folder.READ_WRITE);
            message.setFlag(Flags.Flag.DRAFT, true);

            draftFolder.appendMessages(new Message[]{message});
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to save draft: {}, trace: {}", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.DRAFT_ACTION_FAILED, exception);
        }
    }

    /**
     * <p>
     * Loads metadata from the {@link MimeMessage} into the {@link EmailMetaData} object.
     * </p>
     *
     * @param email   The email whose metadata will be updated.
     * @param message Represent the {@link MimeMessage}.
     * @throws RuntimeException If loading metadata fails.
     */
    private void loadMetaData(final Email email, final MimeMessage message) {
        final EmailMetaData emailMetaData = email.getMetaData();

        try {
            if (Objects.nonNull(emailMetaData)) {
                emailMetaData.setMessageId(message.getMessageID());
                emailMetaData.setDate(message.getSentDate().getTime());
            }
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to load meta data: {}, trace: {}", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.FAILED_TO_LOAD_METADATA, exception);
        }
    }

    /**
     * <p>
     * Searches for messages in the given folder using their Message-ID values.
     * </p>
     *
     * @param folder     The IMAP folder to search.
     * @param messageIds Collection of message IDs to match.
     * @return An array of matched {@link Message} objects.
     */
    private Message[] search(final Folder folder, final Collection<String> messageIds) {

        try {
            final Collection<MessageIDTerm> terms = new ArrayList<>();

            for (final String messageId : messageIds) {
                terms.add(new MessageIDTerm(messageId));
            }

            return folder.search(new OrTerm(terms.toArray(new SearchTerm[0])));
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Search action failed: {}, trace: {}", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.SEARCH_FAILED, exception);
        }
    }

    /**
     * <p>
     * Retrieves the {@link Folder} for the given {@link EmailFolder}. If the folder does not exist, it is created.
     * </p>
     *
     * @param store       The connected {@link Store}.
     * @param emailFolder The enum representing the folder to retrieve.
     * @return The {@link Folder} instance.
     */
    private Folder getFolderOrCreateFolder(final Store store, final EmailFolder emailFolder) throws MessagingException {
        final Folder folder = getFolder(store, emailFolder);

        if (!folder.exists()) {
            folder.create(Folder.HOLDS_MESSAGES);
        }

        return folder;
    }

    /**
     * <p>
     * Retrieves all message from the specified email folder for the given message ids.
     * </p>
     *
     * @param folder        Represent the folder.
     * @param emailFolder   Represents the message ids to be fetched.
     * @param emailInterval Represents the time interval used to filter messages.
     * @return A collection of {@code Message-ID} values matching the filter criteria.
     */
    private Message[] fetchMessages(final Folder folder, final EmailFolder emailFolder, final EmailInterval emailInterval) {

        try {
            if (!folder.isOpen()) {
                folder.open(Folder.READ_ONLY);
            }

            return folder.search(getFilter(emailFolder, emailInterval));
        } catch (Exception exception) {
            LoggerUtil.logError(LOGGER, exception, "Jakarta mail: Failed to fetch messages by message IDs", exception.getMessage(), exception);

            throw ErrorCode.get(EmailErrorCodes.MESSAGE_FETCH_ACTION_FAILED, exception);
        }
    }

    /**
     * <p>
     * Opens and returns a connected IMAP {@link Store}.
     * </p>
     *
     * @return A connected {@link Store} instance.
     */
    private Store getConnectedStore() throws MessagingException {
        final Store store = session.getStore();

        if (authCredentials.authType().isPasswordAuth()) {
            final BasicAuthCredentials basicAuthCredentials = (BasicAuthCredentials) authCredentials;

            store.connect(host, basicAuthCredentials.username(), basicAuthCredentials.password());
        }

        return store;
    }
}