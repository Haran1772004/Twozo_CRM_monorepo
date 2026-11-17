package com.tlc.attachment.storage.internal.config;

import com.tlc.attachment.storage.model.StorageVariant;
import com.tlc.commons.util.Configurations;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;

import com.azure.identity.DefaultAzureCredentialBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures the Azure Blob Storage client to perform container and blob operations.
 * Supports both Azure cloud and Azurite local development environment.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class AzureBlobStorageConfig {

    private final BlobServiceClient blobServiceClient;
    private final Map<StorageVariant, BlobContainerClient> blobContainers = new HashMap<>();
    private final AuthType authType;

    public AzureBlobStorageConfig(final Configurations configurations) {
        final Configurations azuriteConfigs = configurations.getNestedConfigs("azurite");
        final String accountName = configurations.getConfig("accountName");
        final String accountKey = configurations.optConfig("accountKey", "");
        final Collection<String> containers = configurations.getValues("containers");
        final BlobServiceClientBuilder blobServiceClientBuilder = new BlobServiceClientBuilder();

        if (Boolean.parseBoolean(azuriteConfigs.optConfig("enabled", "false"))) {
            this.authType = AuthType.CONNECTION_STRING;
            final String endpoint = azuriteConfigs.getConfig("endpoint");
            final String connectionString = String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=%s;",
                    accountName, accountKey, endpoint);

            blobServiceClientBuilder.connectionString(connectionString);
        } else {
            this.authType = AuthType.valueOf(configurations.getConfig("authType").toUpperCase());
            final String endpoint = String.format(configurations.getConfig("endpoint"), accountName);

            if (AuthType.MANAGED_IDENTITY == authType) {
                blobServiceClientBuilder.endpoint(endpoint).credential(new DefaultAzureCredentialBuilder().build());
            } else {
                blobServiceClientBuilder.connectionString(endpoint);
            }
        }
        this.blobServiceClient = blobServiceClientBuilder.buildClient();

        for (final String container : containers) {
            final String[] variantContainer = container.split(":");

            this.blobContainers.put(StorageVariant.valueOf(variantContainer[1]), blobServiceClient.getBlobContainerClient(variantContainer[0]));
        }
    }

    /**
     * Gets the {@link BlobServiceClient} instance.
     *
     * @return {@link BlobServiceClient}.
     */
    public BlobServiceClient getBlobServiceClient() {
        return blobServiceClient;
    }

    /**
     * Gets the {@link BlobContainerClient} instance grouped by the {@code StorageVariant}.
     *
     * @return The grouped {@link BlobContainerClient}.
     */
    public Map<StorageVariant, BlobContainerClient> getBlobContainers() {
        return blobContainers;
    }

    /**
     * Gets the {@link AuthType}.
     *
     * @return {@link AuthType}.
     */
    public AuthType getAuthType() {
        return authType;
    }

    /**
     * Represents the type of authentication used to build a {@link BlobServiceClient}.
     */
    public enum AuthType {

        /**
         * The client was built using an Azure Storage connection string.
         * <p>
         * A connection string typically contains {@code AccountName}, {@code AccountKey},
         * and optionally {@code EndpointSuffix} or {@code SharedAccessSignature}.
         * </p>
         */
        CONNECTION_STRING,

        /**
         * The client was built using a Managed Identity or another Azure Active Directory token credential.
         * <p>
         * Commonly used when running in Azure services like Virtual Machines, App Services.
         * </p>
         */
        MANAGED_IDENTITY,
        ;
    }

}