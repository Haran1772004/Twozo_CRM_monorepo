package com.tlc.email.server.internal.provider.gmail.model;

import com.tlc.email.common.model.config.FolderType;

/**
 * <p>
 * Represents a request for listing Gmail messages.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public final class ListRequest implements GmailRequest {

    private final String labelId;
    private final String userId;
    private final String query;
    private final String pageToken;
    private final Long maxResults;
    private final FolderType folderType;

    private ListRequest(final Builder builder) {
        this.labelId = builder.labelId;
        this.userId = builder.userId;
        this.query = builder.query;
        this.pageToken = builder.pageToken;
        this.maxResults = builder.maxResults;
        this.folderType = builder.folderType;
    }

    public String getLabelId() {
        return labelId;
    }

    public String getUserId() {
        return userId;
    }

    public String getQuery() {
        return query;
    }

    public String getPageToken() {
        return pageToken;
    }

    public Long getMaxResults() {
        return maxResults;
    }

    public FolderType getFolderType() {
        return folderType;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * Builder class for constructing {@link ListRequest} instances.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static final class Builder {

        private String labelId;
        private String userId;
        private String query;
        private String pageToken;
        private Long maxResults;
        private FolderType folderType;

        private Builder() {
        }

        public Builder labelId(final String labelId) {
            this.labelId = labelId;

            return this;
        }

        public Builder userId(final String userId) {
            this.userId = userId;

            return this;
        }

        public Builder query(final String query) {
            this.query = query;

            return this;
        }

        public Builder pageToken(final String pageToken) {
            this.pageToken = pageToken;

            return this;
        }

        public Builder maxResults(final Long maxResults) {
            this.maxResults = maxResults;

            return this;
        }

        public Builder folderType(final FolderType folderType) {
            this.folderType = folderType;

            return this;
        }

        public ListRequest build() {
            return new ListRequest(this);
        }
    }
}