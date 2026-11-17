package com.tlc.email.server.internal.provider.gmail.model;

import java.math.BigInteger;

/**
 * <p>
 * Represents the response returned from the Gmail Watch API.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class WatchResponse implements GmailResponse {

    private final Long expiration;
    private final BigInteger historyId;

    private WatchResponse(final Builder builder) {
        this.expiration = builder.expiration;
        this.historyId = builder.historyId;
    }

    public Long getExpiration() {
        return expiration;
    }

    public BigInteger getHistoryId() {
        return historyId;
    }

    public static WatchResponse.Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * Builder class for constructing {@link WatchResponse} instances in a fluent manner.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static final class Builder {

        private Long expiration;
        private BigInteger historyId;

        private Builder() {
        }

        public Builder expiration(final Long expiration) {
            this.expiration = expiration;

            return this;
        }

        public Builder historyId(final BigInteger historyId) {
            this.historyId = historyId;

            return this;
        }

        public WatchResponse build() {
            return new WatchResponse(this);
        }
    }
}