package com.tlc.attachment.storage.internal.config;

import com.tlc.commons.util.Configurations;
import io.minio.MinioClient;

/**
 * Configures the object storage service client to perform bucket and object operations.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class MinIOStorageConfig {

    private final MinioClient minioClient;
    private final String bucketName;

    public MinIOStorageConfig(final Configurations configurations) {
        final String endpoint = String.format("http://%s", configurations.getConfig("server"));
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(configurations.getConfig("accessKey"),
                        configurations.getConfig("secretKey")).build();
        this.bucketName = configurations.getConfig("bucket");
    }

    /**
     * Gets the {@link MinioClient} object.
     *
     * @return {@link MinioClient}.
     */
    public MinioClient getClient() {
        return minioClient;
    }

    /**
     * Gets the {@link MinIOStorageConfig} bucket name.
     *
     * @return The name of the bucket.
     */
    public String getBucketName() {
        return bucketName;
    }

}
