package com.tlc.email.server.internal.provider.gmail.model;

import com.tlc.commons.code.ErrorCode;
import com.tlc.email.common.internal.status.EmailErrorCodes;

/**
 * <p>
 * Represents a Gmail label.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class Label {

    private final String id;
    private final Name name;

    private Label(final Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
    }

    public String getId() {
        return id;
    }

    public Name getName() {
        return name;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * Builder class for constructing {@link Label} instances.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static final class Builder {

        private String id;
        private Name name;

        private Builder() {
        }

        public Builder id(final String id) {
            this.id = id;

            return this;
        }

        public Builder name(final Name name) {
            this.name = name;

            return this;
        }

        public Label build() {
            return new Label(this);
        }
    }

    /**
     * <p>
     * Enum representing predefined Gmail label names.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public enum Name {

        INBOX("INBOX"),
        SENT("SENT"),
        DRAFT("DRAFT"),
        TRASH("TRASH"),
        UNREAD("UNREAD"),
        SPAM("SPAM"),
        STARRED("STARRED"),
        ;

        private final String value;

        Name(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        /**
         * <p>
         * Attempts to get a {@code Name} enum constant from a string Case-insensitive.
         * </p>
         *
         * @param label the label string.
         * @return an {@code Optional<Name>} corresponding to the label.
         */
        public static Name get(final String label) {
            return switch (label.toLowerCase()) {
                case "inbox" -> INBOX;
                case "sent" -> SENT;
                case "trash" -> TRASH;
                case "unread" -> UNREAD;
                case "draft" -> DRAFT;
                case "starred" -> STARRED;
                case "spam" -> SPAM;
                default -> throw ErrorCode.get(EmailErrorCodes.UNSUPPORTED_LABEL);
            };
        }
    }

    /**
     * <p>
     * Enum defining filter behavior for labels.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public enum FilterBehaviour {

        INCLUDE("INCLUDE"),
        EXCLUDE("EXCLUDE");

        private final String value;

        FilterBehaviour(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}