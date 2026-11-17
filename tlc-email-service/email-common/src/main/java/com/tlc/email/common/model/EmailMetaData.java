package com.tlc.email.common.model;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * A model that represents the email metadata.
 * </p>
 *
 * @author AswiniN
 * @version 1.0
 */
public final class EmailMetaData implements EmailModel {

    private static final String EMPTY_RETURN_PATH = "<>";
    private static final String CURRENT_MAILBOX_PATTERN = "for\\s+<?([^>\\s;]+)>?";

    private boolean isInitialMessage;
    private Long id;
    private Long date;
    private String messageId;
    private String references;
    private String inReplyTo;
    private String externalReference;
    private String returnPath;
    private String conversationThreadId;
    private String replyToAddress;
    private String parentFolderId;
    private String senderName;
    private String internalDraftId;
    private String internalMessageId;
    private String trackingId;
    private String received;
    private String currentMailboxId;
    private String senderId;
    private String autoForwardingAddress;

    public boolean isInitialMessage() {
        return isInitialMessage;
    }

    public void setInitialMessage(final boolean initialMessage) {
        isInitialMessage = initialMessage;
    }

    public void setInitialMessage() {
        isInitialMessage = Objects.isNull(inReplyTo) && Objects.isNull(references);
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(final Long date) {
        this.date = date;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(final String messageId) {
        this.messageId = messageId;
    }

    public String getReferences() {
        return references;
    }

    public void setReferences(final String references) {
        this.references = references;
    }

    public String getInReplyTo() {
        return inReplyTo;
    }

    public void setInReplyTo(final String inReplyTo) {
        this.inReplyTo = inReplyTo;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(final String externalReference) {
        this.externalReference = externalReference;
    }

    public String getReturnPath() {
        return returnPath;
    }

    public void setReturnPath(final String returnPath) {
        this.returnPath = Objects.equals(returnPath, EMPTY_RETURN_PATH) ? null : returnPath;
    }

    public String getConversationThreadId() {
        return conversationThreadId;
    }

    public void setConversationThreadId(final String conversationThreadId) {
        this.conversationThreadId = conversationThreadId;
    }

    public String getReplyToAddress() {
        return replyToAddress;
    }

    public void setReplyToAddress(final String replyToAddress) {
        this.replyToAddress = replyToAddress;
    }

    public String getParentFolderId() {
        return parentFolderId;
    }

    public void setParentFolderId(final String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(final String senderName) {
        this.senderName = senderName;
    }

    @Override
    public Long id() {
        return id;
    }

    public String getInternalDraftId() {
        return internalDraftId;
    }

    public void setInternalDraftId(final String internalDraftId) {
        this.internalDraftId = internalDraftId;
    }

    public void setInternalMessageId(final String internalMessageId) {
        this.internalMessageId = internalMessageId;
    }

    public String getInternalMessageId() {
        return internalMessageId;
    }

    public String getTrackingId() {
        return trackingId;
    }

    public void setTrackingId(final String trackingId) {
        this.trackingId = trackingId;
    }

    public String getReceived() {
        return received;
    }

    public void setReceived(final String received) {
        this.received = received;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(final String senderId) {
        this.senderId = senderId;
    }

    public String getCurrentMailboxId() {
        return Objects.isNull(currentMailboxId) ? getCurrentMailBox() : currentMailboxId;
    }

    /**
     * <p>
     * This method parses the Received header and retrieves the email address
     * following the "for" field, which corresponds to the mailbox this
     * particular copy of the email is being delivered to.
     * </p>
     *
     * <p>
     * Example Received header:
     * <pre>
     * Received: from mail.example.com (mail.example.com [192.0.2.1])
     *           by smtp.gmail.com with SMTP id abc123
     *           for &lt;user1@gmail.com&gt;;
     *           Mon, 22 Sep 2025 13:45:00 +0530 (IST)
     * </pre>
     * This method will return "user1@gmail.com".
     * </p>
     *
     * @return the current mailbox address if present; otherwise, {@code null}
     */
    private String getCurrentMailBox() {
        if (Objects.nonNull(received)) {
            final Pattern pattern = Pattern.compile(CURRENT_MAILBOX_PATTERN);
            final Matcher matcher = pattern.matcher(received);

            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        return null;
    }

    public void setCurrentMailboxId(final String currentMailboxId) {
        this.currentMailboxId = currentMailboxId;
    }

    public String getAutoForwardingAddress() {
        return autoForwardingAddress;
    }

    public void setAutoForwardingAddress(final String autoForwardingAddress) {
        this.autoForwardingAddress = autoForwardingAddress;
    }

    /**
     * Refers to the {@link Headers} of Type.
     */
    public enum Headers {

        MESSAGE_ID("Message-Id"),
        IN_REPLY_TO("In-Reply-To"),
        REFERENCES("References"),
        RETURN_PATH("Return-Path"),
        SENDER_ID("X-SID-PRA"),
        BCC("Bcc"),
        RECEIVED("received"),
        RESEND_FROM("Resent-From"),
        DELIVERED_TO("Delivered-To"),
        FORWARDING_LOOP("X-MS-Exchange-ForwardingLoop")
        ;

        private final String name;

        Headers(final String name) {
            this.name = Objects.requireNonNull(name);
        }

        public String getName() {
            return name;
        }

        /**
         * <p>
         * Gets the required header name
         * </p>
         *
         * @param name The header name
         * @return {@link Headers}
         */
        static Headers get(final String name) {
            return switch (name) {
                case "In-Reply-To" -> IN_REPLY_TO;
                case "References" -> REFERENCES;
                case "Return-Path" -> RETURN_PATH;
                case "Bcc" -> BCC;
                case "received" -> RECEIVED;
                default -> null;
            };
        }
    }
}