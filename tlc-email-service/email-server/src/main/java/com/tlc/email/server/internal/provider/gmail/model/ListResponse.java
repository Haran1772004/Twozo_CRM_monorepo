package com.tlc.email.server.internal.provider.gmail.model;

import com.tlc.email.common.model.type.Email;

import java.util.Collection;

/**
 * <p>
 * Represents the response returned after fetching a list of Gmail messages.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class ListResponse implements GmailResponse {

    private final String nextPageToken;
    private final Collection<Email> messages;

    private ListResponse(final Builder builder) {
        this.nextPageToken = builder.nextPageToken;
        this.messages = builder.messages;
    }

    public String getNextPageToken() {
        return nextPageToken;
    }

    public Collection<Email> getMessages() {
        return messages;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * Builder class for {@link ListResponse}.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static final class Builder {

        private String nextPageToken;
        private Collection<Email> messages;

        private Builder() {
        }

        public Builder nextPageToken(final String nextPageToken) {
            this.nextPageToken = nextPageToken;

            return this;
        }

        public Builder messages(final Collection<Email> messages) {
            this.messages = messages;

            return this;
        }

        public ListResponse build() {
            return new ListResponse(this);
        }
    }
}