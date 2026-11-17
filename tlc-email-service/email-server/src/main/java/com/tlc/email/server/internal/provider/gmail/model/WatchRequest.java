package com.tlc.email.server.internal.provider.gmail.model;

import java.util.List;

/**
 * <p>
 * Represents the Gmail Watch API request payload.
 * </p>
 *
 * @author Mohamed Yasar K
 * @version 1.0
 */
public class WatchRequest implements GmailRequest {

    private final String pubSubTopic;
    private final List<String> labelIds;
    private final String labelFilterBehavior;

    private WatchRequest(final Builder builder) {
        this.pubSubTopic = builder.pubSubTopic;
        this.labelIds = builder.labelIds;
        this.labelFilterBehavior = builder.labelFilterBehavior;
    }

    public String getPubSubTopic() {
        return pubSubTopic;
    }

    public List<String> getLabelIds() {
        return labelIds;
    }

    public String getLabelFilterBehavior() {
        return labelFilterBehavior;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * <p>
     * Builder class for constructing {@link WatchRequest} instances.
     * </p>
     *
     * @author Mohamed Yasar K
     * @version 1.0
     */
    public static final class Builder {

        private String pubSubTopic;
        private List<String> labelIds;
        private String labelFilterBehavior;

        private Builder() {
        }

        public Builder pubSubTopic(final String pubSubTopic) {
            this.pubSubTopic = pubSubTopic;

            return this;
        }

        public Builder labelIds(final List<String> labelIds) {
            this.labelIds = labelIds;

            return this;
        }

        public Builder labelFilterBehaviour(final String labelFilterBehavior) {
            this.labelFilterBehavior = labelFilterBehavior;

            return this;
        }

        public WatchRequest build() {
            return new WatchRequest(this);
        }
    }
}