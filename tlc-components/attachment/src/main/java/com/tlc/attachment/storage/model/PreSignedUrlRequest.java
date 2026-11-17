package com.tlc.attachment.storage.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents a request to generate a pre-signed URL.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class PreSignedUrlRequest {

    /**
     * The version ID of the object for which the presigned URL is requested.
     */
    private final String versionId;

    /**
     * The key name of the object under which it is located for which the pre-signed URL is requested.
     */
    private final String keyName;

    /**
     * The duration for which the pre-signed URL should be valid.
     */
    private final int duration;

    /**
     * The time unit of the duration for which the pre-signed URL should be valid.
     */
    private final TimeUnit timeUnit;

    /**
     * The additional parameters to be added in the pre-signed url.
     */
    private final Map<String, String> requiredParameters;

    private PreSignedUrlRequest(final Builder builder) {
        this.duration = builder.duration;
        this.timeUnit = builder.timeUnit;
        this.keyName = builder.keyName;
        this.versionId = builder.versionId;
        this.requiredParameters = builder.requiredParameters;
    }

    public String versionId() {
        return versionId;
    }

    public String keyName() {
        return keyName;
    }

    public int duration() {
        return duration;
    }

    public TimeUnit timeUnit() {
        return timeUnit;
    }

    public Map<String, String> getRequiredParameters() {
        return requiredParameters;
    }

    /**
     * Argument builder of {@link PreSignedUrlRequest}.
     */
    public static class Builder {

        private final int duration;
        private final TimeUnit timeUnit;

        private String versionId;
        private String keyName;
        private Map<String, String> requiredParameters;

        public Builder(final int duration, final TimeUnit timeUnit) {
            this.duration = duration;
            this.timeUnit = Objects.requireNonNull(timeUnit);
            this.requiredParameters = new HashMap<>();
        }

        public Builder withVersionId(final String versionId) {
            this.versionId = versionId;
            return this;
        }

        public Builder withKeyName(final String keyName) {
            this.keyName = keyName;
            return this;
        }

        public Builder withRequiredParameters(final Map<String, String> requiredParameters) {
            if (Objects.nonNull(requiredParameters)) {
                this.requiredParameters.putAll(requiredParameters);
            }
            return this;
        }

        public PreSignedUrlRequest build() {
            return new PreSignedUrlRequest(this);
        }
    }
}
