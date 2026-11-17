package com.tlc.email.server.internal.provider.outlook;

import com.tlc.email.common.model.EmailAttachment;
import com.tlc.email.common.model.address.EmailAddress;
import com.tlc.email.common.model.address.SyncAddressInfo;

import com.tlc.email.server.internal.field.EmailHeaderFields;

import java.util.ArrayList;
import java.util.Collection;;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * A model which represents the {@link EmailGraphMessage}.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 */
public class EmailGraphMessage {

    private boolean isRead;
    private boolean isDraft;
    private boolean hasAttachments;
    private Long sentDateTime;
    private Long receivedDateTime;
    private String id;
    private String subject;
    private String body;
    private String internetMessageId;
    private String parentFolderId;
    private String bodyPreview;
    private String conversationId;
    private SyncAddressInfo from;
    private EmailAddress replyTo;
    private Collection<EmailAddress> toRecipients;
    private Collection<EmailAddress> ccRecipients;
    private Collection<EmailAddress> bccRecipients;
    private Collection<EmailAttachment> emailAttachments;
    private Map<String, String> internetMessageHeaders;

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public Collection<EmailAddress> getToRecipients() {
        return toRecipients;
    }

    public Collection<EmailAddress> getCcRecipients() {
        return ccRecipients;
    }

    public Collection<EmailAddress> getBccRecipients() {
        return bccRecipients;
    }

    public Map<String, String> getInternetMessageHeaders() {
        return internetMessageHeaders;
    }

    public boolean isRead() {
        return isRead;
    }

    public boolean isDraft() {
        return isDraft;
    }

    public void setDraft(final boolean draft) {
        isDraft = draft;
    }

    public boolean hasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(final boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    public SyncAddressInfo getFrom() {
        return from;
    }

    public void setFrom(final SyncAddressInfo from) {
        this.from = from;
    }

    public Long getSentDateTime() {
        return sentDateTime;
    }

    public void setSentDateTime(final Long sentDateTime) {
        this.sentDateTime = sentDateTime;
    }

    public Long getReceivedDateTime() {
        return receivedDateTime;
    }

    public void setReceivedDateTime(final Long receivedDateTime) {
        this.receivedDateTime = receivedDateTime;
    }

    public EmailAddress getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(final EmailAddress replyTo) {
        this.replyTo = replyTo;
    }

    public void setSubject(final String subject) {
        this.subject = subject;
    }

    public void setBody(final String body) {
        this.body = body;
    }

    public String getInternetMessageId() {
        return internetMessageId;
    }

    public void setInternetMessageId(final String internetMessageId) {
        this.internetMessageId = internetMessageId;
    }

    public void setToRecipients(final Collection<EmailAddress> toRecipients) {
        this.toRecipients = toRecipients;
    }

    public void setCcRecipients(final Collection<EmailAddress> ccRecipients) {
        this.ccRecipients = ccRecipients;
    }

    public void setBccRecipients(final Collection<EmailAddress> bccRecipients) {
        this.bccRecipients = bccRecipients;
    }

    public Collection<EmailAddress> bccRecipients(final Map<String, String> headers) {
        final String bccHeader = Objects.nonNull(headers) ? headers.get(EmailHeaderFields.BCC) : null;

        return Objects.isNull(bccHeader) ? Collections.emptyList() : List.of(new EmailAddress(bccHeader.replaceAll("[<>]", "")));
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setInternetMessageHeaders(final Map<String, String> internetMessageHeaders) {
        this.internetMessageHeaders = internetMessageHeaders;
    }

    public void setRead(final boolean read) {
        isRead = read;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(final String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public String getBodyPreview() {
        return bodyPreview;
    }

    public void setBodyPreview(final String bodyPreview) {
        this.bodyPreview = bodyPreview;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(final String conversationId) {
        this.conversationId = conversationId;
    }

    /**
     * <p>
     * Sets the {@link EmailAttachment}.
     * </p>
     *
     * @param emailAttachments Collection of {@link EmailAttachment}
     */
    public void setEmailAttachments(final Collection<EmailAttachment> emailAttachments) {
        this.emailAttachments = emailAttachments;
    }

    /**
     * <p>
     * Gets the {@link EmailAttachment}
     * </p>
     *
     * @return Collection of {@link EmailAttachment}
     */
    public Collection<EmailAttachment> getEmailAttachments() {
        return emailAttachments;
    }

    /**
     * <p>
     * Adds the {@link EmailAttachment} in the message.
     * </p>
     *
     * @param emailAttachment Entity of {@link EmailAttachment}
     */
    public void addAttachment(final EmailAttachment emailAttachment) {
        this.emailAttachments = Objects.isNull(emailAttachment) ? new ArrayList<>() : this.emailAttachments;
        this.emailAttachments.add(emailAttachment);
    }
}
