package com.tlc.attachment.storage.internal.config;

import com.tlc.commons.util.Configurations;

import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.net.URI;
import java.util.Objects;

/**
 * Configures the object storage service client to perform bucket and object operations of AWS-S3.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class S3StorageConfig {

    private static final String CUSTOM_AUTH_METHOD = "CUSTOM";

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;

    public S3StorageConfig(final Configurations configurations) {
        final String regionKey = Objects.requireNonNull(configurations.getConfig("region"));
        final Region region = Region.of(regionKey);
        final S3ClientBuilder clientBuilder = S3Client.builder();
        final Configurations localstackConfig = configurations.getNestedConfigs("localstack");
        final AwsCredentialsProvider credentialsProvider = getCredentialsProvider(configurations);
        final int maxConnections = configurations.optInt("httpClient.maxConnection", 0);
        final int connectionTimeOut = configurations.optInt("httpClient.connectionTimeout", 0);
        final int acquisitionTimeout = configurations.optInt("httpClient.acquisitionTimeout", 0);

        clientBuilder.region(region);
        clientBuilder.httpClient(ApacheHttpClient.builder()
                .maxConnections(maxConnections)
                .connectionTimeout(Duration.ofSeconds(connectionTimeOut))
                .connectionAcquisitionTimeout(Duration.ofSeconds(acquisitionTimeout))
                .build());

        if ("true".equals(localstackConfig.optConfig("enabled", ""))) {
            clientBuilder.endpointOverride(URI.create(localstackConfig.getConfig("server")))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        } else {
            clientBuilder.credentialsProvider(credentialsProvider);
        }
        this.s3Client = clientBuilder.build();
        this.s3Presigner = S3Presigner.builder()
                .credentialsProvider(credentialsProvider)
                .region(region)
                .build();
        this.bucketName = Objects.requireNonNull(configurations.getConfig("bucket").trim());
    }

    /**
     * Gets the {@link S3Client} instance.
     *
     * @return {@link S3Client}.
     */
    public S3Client getClient() {
        return s3Client;
    }

    /**
     * Gets the {@link S3Presigner} instance.
     *
     * @return {@link S3Presigner}.
     */
    public S3Presigner getS3Presigner() {
        return s3Presigner;
    }

    /**
     * Gets the {@link MinIOStorageConfig} bucket name.
     *
     * @return The name of the bucket.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Gets the credentials provider to create S3 client.
     *
     * @param configurations Which contains the storage configurations.
     * @return the {@link AwsCredentialsProvider}.
     */
    private AwsCredentialsProvider getCredentialsProvider(final Configurations configurations) {
        final boolean isCustomAuthMethod = configurations.optConfig("authMethod", CUSTOM_AUTH_METHOD).equalsIgnoreCase(CUSTOM_AUTH_METHOD);

        if (isCustomAuthMethod) {
            final AwsBasicCredentials credentials = AwsBasicCredentials.create(Objects.requireNonNull(configurations.getConfig("accessKey")),
                    Objects.requireNonNull(configurations.getConfig("secretKey")));
            return StaticCredentialsProvider.create(credentials);
        } else {
            return DefaultCredentialsProvider.create();
        }
    }
}
