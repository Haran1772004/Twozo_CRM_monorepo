package com.tlc.email.server.model;

import com.tlc.email.common.model.config.EmailConfiguration;
import com.tlc.email.common.model.config.FolderType;
import com.tlc.email.common.model.type.Email;
import com.tlc.email.server.internal.protocols.imap.ImapClient;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Store;

import java.util.Collection;

/**
 * <p>
 * Represents email provider-related data, including authentication details,
 * email configuration, folder type, and email collection.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class EmailProviderDataStore {

    private Collection<Email> emails;
    private String accessToken;
    private EmailConfiguration emailReceiverConfiguration;
    private FolderType folderType;
    private boolean hasNextLink;
    private Message[] messageIds;
    private Store store;
    private Folder folder;
    private ImapClient imapClient;

    private boolean includeMessageIdsOnly = false;

    public EmailProviderDataStore(final String accessToken, final EmailConfiguration emailReceiverConfiguration) {
        this.accessToken = accessToken;
        this.emailReceiverConfiguration = emailReceiverConfiguration;
    }

    public EmailProviderDataStore() {
    }

    public EmailConfiguration getEmailReceiverConfiguration() {
        return emailReceiverConfiguration;
    }

    public FolderType getFolderType() {
        return folderType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setEmailReceiverConfiguration(final EmailConfiguration emailReceiverConfiguration) {
        this.emailReceiverConfiguration = emailReceiverConfiguration;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public void setFolderType(final FolderType folderType) {
        this.folderType = folderType;
    }

    public boolean hasNextLink() {
        return hasNextLink;
    }

    public void setNextLink(final boolean hasNextLink) {
        this.hasNextLink = hasNextLink;
    }

    public Collection<Email> getEmails() {
        return emails;
    }

    public void setEmails(final Collection<Email> emails) {
        this.emails = emails;
    }

    public final <T extends EmailProviderDataStore> T get() {
        return (T) this;
    }

    public Message[] getMessages() {
        return messageIds;
    }

    public void setMessages(final Message[] messageIds) {
        this.messageIds = messageIds;
    }

    public ImapClient getImapClient() {
        return imapClient;
    }

    public void setImapClient(final ImapClient imapClient) {
        this.imapClient = imapClient;
    }

    public boolean includeMessageIdsOnly() {
        return includeMessageIdsOnly;
    }

    public void setIncludeMessageIdsOnly(final boolean includeMessageIdsOnly) {
        this.includeMessageIdsOnly = includeMessageIdsOnly;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public Folder getFolder() {
        return folder;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }
}