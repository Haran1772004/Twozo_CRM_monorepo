package com.tlc.email.server.internal.provider.gmail.model;

import java.util.Collection;
import java.util.Objects;

import com.tlc.email.server.internal.provider.gmail.model.Label.Name;

/**
 * <p>
 * Represents information about labels associated with a specific Gmail message.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class MessageLabelInfo {

    private final String messageId;
    private final Collection<Name> labelList;

    private MessageLabelInfo(final Builder builder) {
        this.messageId = builder.messageId;
        this.labelList = builder.labelList;
    }

    public String getMessageId() {
        return messageId;
    }

    public Collection<Name> getLabelList() {
        return labelList;
    }

    public boolean isDraft() {
        return Objects.nonNull(labelList) && labelList.contains(Name.DRAFT);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * Builder class for constructing {@link MessageLabelInfo} instances.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static final class Builder {
        private String messageId;
        private Collection<Name> labelList;

        private Builder() {
        }

        public Builder messageId(final String messageId) {
            this.messageId = messageId;
            return this;
        }

        public Builder labelList(final Collection<Name> labelList) {
            this.labelList = labelList;
            return this;
        }

        public MessageLabelInfo build() {
            return new MessageLabelInfo(this);
        }
    }
}