package com.tlc.email.server.internal.provider.gmail.model;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;

/**
 * <p>
 * Represents a Gmail history entry that tracks changes like messages added, deleted or labels modified in a user's mailbox.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class History {

    private final BigInteger id;

    private final Collection<String> addedMessages;
    private final Collection<String> deletedMessages;
    private final Collection<String> labelModifiedMessages;

    private History(final Builder builder) {
        this.id = builder.id;
        this.addedMessages = builder.addedMessages;
        this.deletedMessages = builder.deletedMessages;
        this.labelModifiedMessages = builder.labelModifiedMessages;
    }

    public BigInteger getId() {
        return id;
    }

    public Collection<String> getDeletedMessages() {
        return deletedMessages;
    }

    public Collection<String> getLabelModifiedMessages() {
        return labelModifiedMessages;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Collection<String> getAddedMessages() {
        return addedMessages;
    }

    /**
     * <p>
     * Builder class for constructing {@link History} instances.
     * </p>
     *
     * @author Mohamed Yasar k
     * @version 1.0
     */
    public static final class Builder {

        private BigInteger id;

        private Collection<String> addedMessages;
        private Collection<String> deletedMessages;
        private Collection<String> labelModifiedMessages;

        public Builder id(final BigInteger id) {
            this.id = id;

            return this;
        }

        public Builder deletedMessages(final Collection<String> deletedMessages) {
            this.deletedMessages = deletedMessages;

            return this;
        }

        public Builder addedMessages(final Collection<String> addedMessages) {
            this.addedMessages = addedMessages;

            return this;
        }

        public Builder labelModifiedMessages(final Collection<String> labelModifiedMessages) {
            this.labelModifiedMessages = labelModifiedMessages;

            return this;
        }

        public History build() {
            return new History(this);
        }
    }

    /**
     * <p>
     * Enum representing types of Gmail history events.
     * </p>
     *
     * @author Mohamed Yasar k
     * @version 1.0
     */
    public enum Type {

        MESSAGE_ADDED("messageAdded"),
        MESSAGE_DELETED("messageDeleted"),
        LABEL_ADDED("labelAdded"),
        LABEL_DELETED("labelRemoved"),
        ;

        private final String value;

        Type(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static List<String> getValues() {
            return List.of(
                    MESSAGE_ADDED.value,
                    MESSAGE_DELETED.value,
                    LABEL_ADDED.value,
                    LABEL_DELETED.value
            );
        }
    }
}