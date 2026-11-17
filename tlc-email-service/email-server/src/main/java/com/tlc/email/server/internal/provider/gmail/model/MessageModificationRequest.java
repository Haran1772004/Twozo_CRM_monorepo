package com.tlc.email.server.internal.provider.gmail.model;

import java.util.List;

/**
 * <p>
 * Represents a request to modify a Gmail message by adding or removing labels.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class MessageModificationRequest implements GmailRequest {

    private final String messageId;
    private final List<String> addLabelIds;
    private final List<String> removeLabelIds;

    private MessageModificationRequest(final Builder builder) {
        this.messageId = builder.messageId;
        this.addLabelIds = builder.addLabelIds;
        this.removeLabelIds = builder.removeLabelIds;
    }

    public String getMessageId() {
        return messageId;
    }

    public List<String> getAddLabelIds() {
        return addLabelIds;
    }

    public List<String> getRemoveLabelIds() {
        return removeLabelIds;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * Builder class for {@link MessageModificationRequest}.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static final class Builder {

        private String messageId;
        private List<String> addLabelIds;
        private List<String> removeLabelIds;

        private Builder() {
        }

        public Builder messageId(final String messageId) {
            this.messageId = messageId;

            return this;
        }

        public Builder addLabelIds(final List<String> addLabelIds) {
            this.addLabelIds = addLabelIds;

            return this;
        }

        public Builder removeLabelIds(final List<String> removeLabelIds) {
            this.removeLabelIds = removeLabelIds;

            return this;
        }

        public MessageModificationRequest build() {
            return new MessageModificationRequest(this);
        }
    }
}