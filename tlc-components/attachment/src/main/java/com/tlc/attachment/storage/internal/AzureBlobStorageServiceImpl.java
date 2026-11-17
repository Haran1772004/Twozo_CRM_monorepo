package com.tlc.attachment.storage.internal;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.models.UserDelegationKey;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.common.sas.SasProtocol;

import com.tlc.attachment.service.ObjectStorageService;
import com.tlc.attachment.storage.model.ObjectCountRequest;
import com.tlc.attachment.storage.model.ObjectDownloadRequest;
import com.tlc.attachment.storage.model.ObjectDownloadResponse;
import com.tlc.attachment.storage.model.ObjectExistsRequest;
import com.tlc.attachment.storage.model.ObjectListRequest;
import com.tlc.attachment.storage.model.ObjectListResponse;
import com.tlc.attachment.storage.model.ObjectStorageType;
import com.tlc.attachment.storage.model.ObjectUploadRequest;
import com.tlc.attachment.storage.model.ObjectUploadResponse;
import com.tlc.attachment.storage.model.ObjectVersionDeleteRequest;
import com.tlc.attachment.storage.model.PreSignedUrlHeader;
import com.tlc.attachment.storage.model.PreSignedUrlRequest;
import com.tlc.attachment.storage.model.StorageVariant;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.attachment.storage.internal.config.AzureBlobStorageConfig;
import com.tlc.attachment.storage.internal.config.AzureBlobStorageConfig.AuthType;

import com.tlc.commons.code.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

/**
 * Manage the services to interact with Azure Blob Storage, including
 * creating containers, uploading, downloading, listing, and deleting blobs.
 *
 * <p>This implementation can work with both Azure's real cloud storage and Azurite (local emulator),
 * depending on the connection configuration.</p>
 *
 * <p><span style="color:red"><strong>Note:</strong> Versioning and advanced access policies may not be supported
 * in emulator mode (Azurite).</p>
 *
 * @author YourName
 * @since 1.0
 */

public final class AzureBlobStorageServiceImpl implements ObjectStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureBlobStorageServiceImpl.class);

    private final BlobServiceClient blobServiceClient;
    private final Map<StorageVariant, BlobContainerClient> blobContainers;
    private final AuthType authType;

    public AzureBlobStorageServiceImpl(final AzureBlobStorageConfig azureBlobStorageConfig) {
        this.blobServiceClient = azureBlobStorageConfig.getBlobServiceClient();
        this.blobContainers = azureBlobStorageConfig.getBlobContainers();
        this.authType = azureBlobStorageConfig.getAuthType();
    }

    /**
     * {@inheritDoc}
     *
     * @param uploadRequest Refers the {@link ObjectUploadRequest}.
     * @return {@link ObjectUploadResponse}.
     */
    @Override
    public ObjectUploadResponse uploadObject(final ObjectUploadRequest uploadRequest) {
        final BlobContainerClient containerClient = blobContainers.get(uploadRequest.storageVariant());
        final String blobName = uploadRequest.getKeyName();
        final InputStream inputStream = uploadRequest.getInputStream();
        final BlockBlobClient blockBlobClient = containerClient.getBlobClient(blobName).getBlockBlobClient();

        try (final BufferedInputStream bufferedInputStream = inputStream instanceof final BufferedInputStream bis ? bis
                : new BufferedInputStream(inputStream)) {
            final BlockBlobItem blockBlobItem = blockBlobClient.upload(bufferedInputStream, bufferedInputStream.available(), true);

            return new ObjectUploadResponse(blockBlobItem.getVersionId(), blobName, null, ObjectStorageType.AZURE_BLOB);
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while uploading the blob - {}, Reason: {}", uploadRequest.getKeyName(), exception.getMessage());
            throw ErrorCode.get(StorageErrorCodes.OBJECT_UPLOAD_FAILED, "i18n.attachment.object.upload.failed", exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param downloadRequest Refers the {@link ObjectDownloadRequest}.
     * @return {@link ObjectDownloadResponse} of the blob.
     */
    @Override
    public ObjectDownloadResponse downloadObject(final ObjectDownloadRequest downloadRequest) {
        try {
            final BlobContainerClient containerClient = blobContainers.get(downloadRequest.storageVariant());
            final BlockBlobClient blockBlobClient = containerClient.getBlobClient(downloadRequest.objectKeyName())
                    .getVersionClient(downloadRequest.versionId())
                    .getBlockBlobClient();

            return new ObjectDownloadResponse(blockBlobClient.downloadContent().toStream());
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while downloading the blob - {}, Reason: {}, Stack: ", downloadRequest.objectKeyName(), exception.getMessage(), exception);
            throw ErrorCode.get(StorageErrorCodes.OBJECT_DOWNLOAD_FAILED, "i18n.attachment.object.download.failed", exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param objectExistsRequest The {@code ObjectExistsRequest} containing the necessary details to check the blob exists or not.
     * @return {@code true} if the blob exists, {@code false} otherwise.
     */
    @Override
    public boolean doesObjectExists(final ObjectExistsRequest objectExistsRequest) {
        try {
            final BlobContainerClient containerClient = blobContainers.get(objectExistsRequest.storageVariant());
            final BlockBlobClient blockBlobClient = containerClient.getBlobClient(objectExistsRequest.objectName()).getBlockBlobClient();

            return blockBlobClient.exists();
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while to check the blob existence - {}, Reason: {}, Stack: ", objectExistsRequest.objectName(), exception.getMessage(), exception);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param sasSignatureRequest The request containing the necessary details to generate the SAS token.
     * @return The generated SAS token URL.
     */
    @Override
    public URL getPresignedUrl(final PreSignedUrlRequest sasSignatureRequest) {
        final OffsetDateTime offsetDateTime = OffsetDateTime.now(ZoneOffset.UTC);
        final String blobName = sasSignatureRequest.keyName();

        try {
            final Map<String, String> requiredParameters = sasSignatureRequest.getRequiredParameters();

            final BlobSasPermission permission = new BlobSasPermission().setReadPermission(true);
            final BlobServiceSasSignatureValues sasValues = new BlobServiceSasSignatureValues(offsetDateTime.plusMinutes(sasSignatureRequest.duration()), permission)
                    .setStartTime(offsetDateTime.minusMinutes(1))
                    .setProtocol(SasProtocol.HTTPS_HTTP);

            if (Objects.nonNull(requiredParameters) && !requiredParameters.isEmpty()) {
                sasValues.setContentType(requiredParameters.get(PreSignedUrlHeader.RESPONSE_CONTENT_TYPE));
                sasValues.setContentDisposition(requiredParameters.get(PreSignedUrlHeader.RESPONSE_CONTENT_DISPOSITION));
            }
            final BlockBlobClient blockBlobClient = blobContainers.get(StorageVariant.GENERAL).getBlobClient(blobName)
                    .getVersionClient(sasSignatureRequest.versionId())
                    .getBlockBlobClient();
            final String sasToken = switch (authType) {
                case MANAGED_IDENTITY -> {
                    final UserDelegationKey userDelegationKey = blobServiceClient.getUserDelegationKey(offsetDateTime, offsetDateTime.plusHours(1));

                    yield blockBlobClient.generateUserDelegationSas(sasValues, userDelegationKey);
                }
                case CONNECTION_STRING -> blockBlobClient.generateSas(sasValues);
            };

            return new URL(String.join("&", blockBlobClient.getBlobUrl(), sasToken));
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while generating a SAS Token for the blob - {}, Reason: {}, Stack: ", blobName, exception.getMessage(), exception);
            throw ErrorCode.get(StorageErrorCodes.PRE_SIGNED_URL_GENERATION_FAILED, "i18n.attachment.object.url.generation.failed", exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param versionDeleteRequest The request containing the details of the blob version to delete.
     */
    @Override
    public void deleteObjectVersion(final ObjectVersionDeleteRequest versionDeleteRequest) {
        final String versionId = versionDeleteRequest.versionId();
        try {
            final BlockBlobClient blockBlobClient = blobContainers.get(versionDeleteRequest.storageVariant())
                    .getBlobClient(versionDeleteRequest.keyName())
                    .getVersionClient(versionId)
                    .getBlockBlobClient();

            blockBlobClient.deleteIfExists();
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while delete a blob version of Id: {}, Reason: {}, Stack: ", versionId, exception.getMessage(), exception);
            throw ErrorCode.get(StorageErrorCodes.OBJECT_VERSION_DELETE_FAILED, "i18n.attachment.object.version.delete.failed", exception);
        }
    }

    @Override
    public CompletableFuture<Long> countObjectsAsync(final ObjectCountRequest countRequest) {
        return CompletableFuture.failedFuture(ErrorCode.get(StorageErrorCodes.OBJECT_COUNT_ACTION_FAILED));
    }

    @Override
    public CompletableFuture<Boolean> doesObjectExistAsync(final ObjectExistsRequest objectExistsRequest) {
        return CompletableFuture.supplyAsync(() -> doesObjectExists(objectExistsRequest), ObjectStorageAsyncExecutor.get());
    }

    @Override
    public CompletableFuture<ObjectListResponse> listObjectsAsync(final ObjectListRequest listRequest) {
        return CompletableFuture.failedFuture(ErrorCode.get(StorageErrorCodes.OBJECT_LIST_ACTION_FAILED));
    }

    @Override
    public CompletableFuture<Void> deleteObjectVersionAsync(final ObjectVersionDeleteRequest versionDeleteRequest) {
        return CompletableFuture.runAsync(() -> deleteObjectVersion(versionDeleteRequest), ObjectStorageAsyncExecutor.get());
    }

    @Override
    public CompletableFuture<ObjectDownloadResponse> downloadObjectAsync(final ObjectDownloadRequest downloadRequest) {
        return CompletableFuture.supplyAsync(() -> downloadObject(downloadRequest), ObjectStorageAsyncExecutor.get());
    }
}
