package com.tlc.email.server.internal.storage;

import com.tlc.attachment.service.ObjectStorageService;
import com.tlc.attachment.storage.internal.S3StorageServiceImpl;
import com.tlc.attachment.storage.internal.config.S3StorageConfig;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.email.server.fields.InboundEmailReceiverFields;

import java.util.Map;

/**
 * Provides inbound-bucket object storage access for inbound email processing.
 *
 * @author sundar
 * @version 1.0
 */
public final class InboundEmailStorageService {

    private static final String STATE_SEPARATOR = "_";

    private final ObjectStorageService objectStorageService;
    private final String bucketName;

    private InboundEmailStorageService() {
        final Configurations inboundConfigurations = ConfHolder.getInstance()
                .getProperties("storage")
                .getNestedConfigs("inbound");
        this.objectStorageService = new S3StorageServiceImpl(new S3StorageConfig(inboundConfigurations));
        this.bucketName = inboundConfigurations.getConfig("bucket").trim();
    }

    private static class Instance {
        private static final InboundEmailStorageService INSTANCE = new InboundEmailStorageService();
    }

    public static InboundEmailStorageService getInstance() {
        return Instance.INSTANCE;
    }

    public ObjectStorageService getObjectStorageService() {
        return objectStorageService;
    }

    public String getBucketName() {
        return bucketName;
    }

    /**
     * Builds the metadata filter for objects awaiting delivery to the given environment,
     * e.g. {@code state=send_requested_dev}.
     *
     * @return metadata filter keyed by {@link InboundEmailReceiverFields#STATE}
     */
    public Map<String, String> getSendRequestedMetadataFilter() {
        return Map.of(
                InboundEmailReceiverFields.STATE,
                InboundEmailReceiverFields.STATE_SEND_REQUESTED
        );
    }
}