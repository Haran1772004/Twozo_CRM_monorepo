package com.tlc.email.server.model;

import com.tlc.email.common.model.config.EmailConfiguration;

import com.tlc.email.server.internal.provider.gmail.GmailClient;
import jakarta.mail.Message;

/**
 * Handles email data retrieved from Gmail. Extends the base functionality to accommodate
 * Gmail-specific attributes and processing.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see EmailProviderDataStore
 */
public final class GmailDataStore extends EmailProviderDataStore {

    private Message[] messages;
    private String pageToken;
    private String filterQuery;
    private int batchSize;
    private GmailClient gmailClient;

    public GmailDataStore(final String accessToken, final EmailConfiguration emailReceiverConfiguration) {
        super(accessToken, emailReceiverConfiguration);
    }

    public GmailDataStore(final EmailConfiguration emailReceiverConfiguration) {
        super(null, emailReceiverConfiguration);
    }

    public Message[] getMessages() {
        return messages;
    }

    public void setMessages(final Message[] messages) {
        this.messages = messages;
    }

    public String getPageToken() {
        return pageToken;
    }

    public void setPageToken(final String pageToken) {
        this.pageToken = pageToken;
    }

    public String getFilterQuery() {
        return filterQuery;
    }

    public void setFilterQuery(final String filterQuery) {
        this.filterQuery = filterQuery;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(final int batchSize) {
        this.batchSize = batchSize;
    }

    public GmailClient getGmailClient() {
        return gmailClient;
    }

    public void setGmailClient(final GmailClient gmailClient) {
        this.gmailClient = gmailClient;
    }
}
