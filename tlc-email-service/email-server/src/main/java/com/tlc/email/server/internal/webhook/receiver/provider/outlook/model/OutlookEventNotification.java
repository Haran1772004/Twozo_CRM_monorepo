package com.tlc.email.server.internal.webhook.receiver.provider.outlook.model;

import java.util.Objects;

/**
 * Responsible for webhook notification.
 *
 * @author Kowsalya SP
 * @version 1.0
 */
public class OutlookEventNotification {

    private static final String USERS_PATH_PREFIX = "Users('";
    private static final String MESSAGES_PATH_PREFIX = "messages('";
    private static final String MESSAGES_PATH_SEGMENT = "/Messages/";
    private static final String USERS_PATH_SEGMENT = "Users/";
    private static final String CLOSING_PATH_SUFFIX = "')";

    private String subscriptionId;
    private String expirationDateTime;
    private ChangeType changeType;
    private String resource;
    private EncryptedContent encryptedContent;
    private String tenantId;
    private String messageId;
    private String userId;

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(final String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getExpirationDateTime() {
        return expirationDateTime;
    }

    public void setExpirationDateTime(final String expirationDateTime) {
        this.expirationDateTime = expirationDateTime;
    }

    public ChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(final ChangeType changeType) {
        this.changeType = changeType;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(final String resource) {
        this.resource = resource;
    }

    public EncryptedContent getEncryptedContent() {
        return encryptedContent;
    }

    public void setEncryptedContent(final EncryptedContent encryptedContent) {
        this.encryptedContent = encryptedContent;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(final String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {

        if (Objects.isNull(userId)) {
            processResourcePath();
        }

        return userId;
    }

    public String getMessageId() {

        if (Objects.isNull(messageId)) {
            processResourcePath();
        }

        return messageId;
    }

    /**
     * Extracts and assigns the user ID and message ID from the provided Microsoft Graph resource path.
     */
    private void processResourcePath() {
        if (Objects.isNull(resource) || resource.isEmpty()) {
            return;
        }

        if (resource.contains(USERS_PATH_PREFIX) && resource.contains(MESSAGES_PATH_PREFIX)) {
            this.userId = extract(resource, USERS_PATH_PREFIX, CLOSING_PATH_SUFFIX);
            this.messageId = extract(resource, MESSAGES_PATH_PREFIX, CLOSING_PATH_SUFFIX);
        } else if (resource.contains(USERS_PATH_SEGMENT) && resource.contains(MESSAGES_PATH_SEGMENT)) {
            this.userId = extract(resource, USERS_PATH_SEGMENT, MESSAGES_PATH_SEGMENT);
            this.messageId = resource.substring(resource.indexOf(MESSAGES_PATH_SEGMENT) + 10);
        }
    }

    /**
     * Extracts a substring from the given text that lies between the specified start and end tokens.
     *
     * @param text       the original string from which to extract the substring
     * @param startToken the token marking the beginning of the substring to extract
     * @param endToken   the token marking the end of the substring to extract
     * @return the extracted substring, or an empty string if the tokens are not found or are in the wrong order
     */
    private String extract(final String text, final String startToken, final String endToken) {
        int start = text.indexOf(startToken);
        if (start == -1) return null;

        start += startToken.length();
        int end = text.indexOf(endToken, start);
        if (end == -1) return null;

        return text.substring(start, end);
    }

    /**
     * Represents the change type.
     *
     * @author Kowsalya SP
     * @version 1.0
     */
    public enum ChangeType {
        CREATED,
        UPDATED,
        DELETED;

        public boolean isCreated() {
            return CREATED.equals(this);
        }

        public boolean isDeleted() {
            return DELETED.equals(this);
        }
    }
}