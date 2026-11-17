package com.tlc.attachment.storage.model;

import java.io.InputStream;
import java.util.Objects;

/**
 * Represents a request for uploading an object, containing the input stream,
 * key name, and pre-signed URL.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class ObjectUploadRequest {

    private final InputStream inputStream;
    private final String keyName;
    private final PreSignedUrlRequest preSignedUrlRequest;
    private final MetaData metaData;
    private final StorageVariant storageVariant;

    private ObjectUploadRequest(final Builder builder) {
        this.inputStream = Objects.requireNonNull(builder.inputStream);
        this.keyName = Objects.requireNonNull(builder.keyName);
        this.preSignedUrlRequest = builder.preSignedUrlRequest;
        this.metaData = builder.metaData;
        this.storageVariant = builder.variant;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String getKeyName() {
        return keyName;
    }

    public PreSignedUrlRequest getPreSignedUrlRequest() {
        return preSignedUrlRequest;
    }

    public MetaData metaData() {
        return metaData;
    }

    public StorageVariant storageVariant() {
        return storageVariant;
    }

    /**
     * Argument builder of {@link ObjectUploadRequest}.
     */
    public static class Builder {

        private final InputStream inputStream;
        private final String keyName;

        private PreSignedUrlRequest preSignedUrlRequest;
        private MetaData metaData;
        private StorageVariant variant;

        public Builder(final InputStream inputStream, final String keyName) {
            this.inputStream = Objects.requireNonNull(inputStream);
            this.keyName = Objects.requireNonNull(keyName);
            this.variant = StorageVariant.GENERAL;
        }

        public Builder withPreSignedUrlRequest(final PreSignedUrlRequest preSignedUrlRequest) {
            this.preSignedUrlRequest = preSignedUrlRequest;
            return this;
        }

        public Builder withMetaData(final MetaData metaData) {
            this.metaData = metaData;
            return this;
        }

        public Builder withStorageVariant(final StorageVariant storageVariant) {
            this.variant = storageVariant;
            return this;
        }

        public ObjectUploadRequest build() {
            return new ObjectUploadRequest(this);
        }
    }
}
