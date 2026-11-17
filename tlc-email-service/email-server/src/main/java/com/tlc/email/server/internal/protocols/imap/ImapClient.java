package com.tlc.email.server.internal.protocols.imap;

import com.tlc.email.common.model.config.EmailInterval;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.internal.protocols.AuthCredentials;
import com.tlc.email.server.internal.protocols.models.AuthType;
import com.tlc.email.server.internal.protocols.models.MessageFlagsInfo;
import com.tlc.email.server.internal.provider.MailBoxClient;
import com.tlc.email.server.internal.provider.jakarta.models.EmailFolder;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Store;

import java.util.Collection;

/**
 * <p>
 * Represents a client capable of interacting with an IMAP server to perform operations such as
 * creating drafts, updating messages between folders, and deleting messages.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public interface ImapClient extends MailBoxClient {

    /**
     * <p>
     * Creates and saves the provided {@link Email} as a draft in the IMAP Drafts folder.
     * </p>
     *
     * @param email The {@link Email} object to be saved as a draft.
     */
    void draft(final Email email);

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
    void update(final EmailFolder sourceFolder, final EmailFolder targetFolder, final Collection<String> messageIds);

    /**
     * <p>
     * Deletes messages identified by the given message IDs from the specified folder.
     * </p>
     *
     * @param emailFolder The name of the folder from which the messages will be deleted.
     * @param messageIds  A collection of message IDs representing the emails to delete.
     */
    void delete(final String emailFolder, final Collection<String> messageIds);

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
    Collection<String> getAllMessageIds(final EmailFolder emailFolder, final EmailInterval emailInterval);

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
    Message[] getAllMessages(final Folder folder, EmailFolder emailFolder, EmailInterval emailInterval);

    /**
     * <p>
     * Converts an array of {@link Message} objects into a collection of {@link Email} objects.
     * </p>
     *
     * @param messages The array of {@link Message} objects to convert.
     * @param store    The connected {@link Store} instance containing the messages.
     * @param folder   The {@link Folder} where the messages reside.
     * @return A collection of {@link Email} objects converted from the messages.
     */
    Collection<Email> getEmails(final Message[] messages, final Store store, final Folder folder);

    /**
     * <p>
     * Retrieves all message from the specified email folder for the given message ids.
     * </p>
     *
     * @param emailFolder Represents the message ids to be fetched.
     * @param messageIds  Represents the time interval used to filter messages.
     * @return A collection of {@code Message-ID} values matching the filter criteria.
     */
    Collection<Email> getMessages(final EmailFolder emailFolder, final Collection<String> messageIds);

    /**
     * <p>
     * Determines the authentication mechanisms supported by the specified mail server using the provided credentials.
     * </p>
     *
     * @param authCredentials Represents the credentials used to authenticate with the server.
     * @return A collection of {@link AuthType} values supported by the server.
     */
    Collection<AuthType> getAuthTypes(final String host, final int port, final AuthCredentials authCredentials);

    /**
     * <p>
     * Retrieves a single email by its {@code Message-ID}.
     * </p>
     *
     * @param messageId The {@code Message-ID} of the email to fetch.
     * @return The {@link Email} object corresponding to the {@code Message-ID}, or {@code null} if not found.
     */
    Email getMessage(final String messageId);

    /**
     * <p>
     * Retrieves label and flag information for a given email in a folder.
     * </p>
     *
     * @param emailFolder The {@link EmailFolder} where the message resides.
     * @param messageId   The {@code Message-ID} of the email.
     * @return A {@link MessageFlagsInfo} object containing the flags, or {@code null} if the message is not found.
     */
    MessageFlagsInfo getLabelInfo(final EmailFolder emailFolder, final String messageId);

    /**
     * <p>
     * Opens and returns a connected IMAP {@link Store} instance.
     * </p>
     *
     * @return A connected {@link Store}.
     */
    Store getStore();

    /**
     * <p>
     * Retrieves the {@link Folder} instance for the specified {@link EmailFolder}.
     * </p>
     *
     * @param store       The connected {@link Store}.
     * @param emailFolder The folder to retrieve.
     * @return The {@link Folder} instance.
     */
    Folder getFolder(final Store store, final EmailFolder emailFolder);

    /**
     * <p>
     * Retrieves the {@code Message-ID}s of emails in the specified folder within a given time interval.
     * </p>
     *
     * @param folder        The {@link Folder} to search.
     * @param emailFolder   The {@link EmailFolder} representing the logical folder.
     * @param emailInterval The {@link EmailInterval} representing the time range for filtering messages.
     * @return A collection of {@code Message-ID} strings.
     */
    Collection<String> getMessageIds(final Folder folder, EmailFolder emailFolder, EmailInterval emailInterval);

    /**
     * <p>
     * Tests the connectivity to the email server using the provided configuration.
     * This method actually establishes a connection to verify credentials and server accessibility.
     * </p>
     *
     * @return {@code true} if the connection was successful; {@code false} otherwise
     */
    boolean testConnection();
}