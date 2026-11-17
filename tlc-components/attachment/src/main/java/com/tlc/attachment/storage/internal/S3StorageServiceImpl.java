package com.tlc.attachment.storage.internal;

import com.tlc.attachment.service.ObjectStorageService;
import com.tlc.attachment.storage.model.MetaData;
import com.tlc.attachment.storage.model.ObjectCountRequest;
import com.tlc.attachment.storage.model.ObjectDownloadRequest;
import com.tlc.attachment.storage.model.ObjectDownloadResponse;
import com.tlc.attachment.storage.model.ObjectExistsRequest;
import com.tlc.attachment.storage.model.ObjectListRequest;
import com.tlc.attachment.storage.model.ObjectListResponse;
import com.tlc.attachment.storage.model.ObjectStorageType;
import com.tlc.attachment.storage.model.ObjectUploadResponse;
import com.tlc.attachment.storage.model.ObjectUploadRequest;
import com.tlc.attachment.storage.model.ObjectVersionDeleteRequest;
import com.tlc.attachment.storage.model.PreSignedUrlHeader;
import com.tlc.attachment.storage.model.PreSignedUrlRequest;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import com.tlc.attachment.storage.internal.config.S3StorageConfig;

import com.tlc.commons.code.ErrorCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Provides the object storage services of AWS-S3 client to perform object related operations.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class S3StorageServiceImpl implements ObjectStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3StorageServiceImpl.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final S3Presigner s3Presigner;

    public S3StorageServiceImpl(final S3StorageConfig config) {
        this.s3Client = config.getClient();
        this.s3Presigner = config.getS3Presigner();
        this.bucketName = config.getBucketName();
    }

    /**
     * {@inheritDoc}
     *
     * @param uploadRequest
     * @return {@link ObjectUploadResponse}.
     */
    @Override
    public ObjectUploadResponse uploadObject(final ObjectUploadRequest uploadRequest) {
        try (final InputStream inputStream = uploadRequest.getInputStream()) {
            final String objectKeyName = uploadRequest.getKeyName();
            final MetaData customMetaData = uploadRequest.metaData();
            final PutObjectRequest.Builder putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKeyName);

            if (Objects.nonNull(customMetaData)) {
                putObjectRequest.metadata(customMetaData.getMetaData());
            }

            final PutObjectResponse putObjectResult = s3Client.putObject(putObjectRequest.build(), RequestBody.fromInputStream(inputStream, inputStream.available()));

            return new ObjectUploadResponse(putObjectResult.versionId(), objectKeyName, null, ObjectStorageType.AMAZON_S3);
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while uploading the object - {}, Reason: {}", uploadRequest.getKeyName(), exception.getMessage());
            throw ErrorCode.get(StorageErrorCodes.OBJECT_UPLOAD_FAILED, "i18n.attachment.object.upload.failed");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param downloadRequest Refers the {@code ObjectDownloadRequest}.
     * @return {@link ObjectDownloadResponse}.
     */
    @Override
    public ObjectDownloadResponse downloadObject(final ObjectDownloadRequest downloadRequest) {
        try {
            final GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(downloadRequest.objectKeyName())
                    .versionId(downloadRequest.versionId())
                    .build();

            try (final ResponseInputStream<GetObjectResponse> s3ClientObject = s3Client.getObject(getObjectRequest)) {
                return new ObjectDownloadResponse(new ByteArrayInputStream(s3ClientObject.readAllBytes()));
            }
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while downloading the object - {}, Reason: {}, Stack: ", downloadRequest.objectKeyName(), exception.getMessage(), exception);
            throw ErrorCode.get(StorageErrorCodes.OBJECT_DOWNLOAD_FAILED, "i18n.attachment.object.download.failed");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param objectExistsRequest The {@code ObjectExistsRequest} containing the necessary details to check the object exists or not.
     * @return {@code true} if the object exists, {@code false} otherwise.
     */
    @Override
    public boolean doesObjectExists(final ObjectExistsRequest objectExistsRequest) {
        try {
            final HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectExistsRequest.objectName())
                    .build());

            return metadataMatches(headObjectResponse.metadata(), objectExistsRequest.metadataFilter());
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param preSignedUrlRequest the request object containing the necessary details
     *                            to generate the presigned URL
     * @return the generated presigned URL
     */
    @Override
    public URL getPresignedUrl(final PreSignedUrlRequest preSignedUrlRequest) {
        final Map<String, String> requiredParameters = preSignedUrlRequest.getRequiredParameters();
        final String objectKeyName = preSignedUrlRequest.keyName();

        try {
            final GetObjectRequest.Builder getObjectRequestBuilder = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKeyName)
                    .versionId(preSignedUrlRequest.versionId());

            getObjectRequestBuilder.responseContentType(requiredParameters.get(PreSignedUrlHeader.RESPONSE_CONTENT_TYPE));
            getObjectRequestBuilder.responseContentDisposition(requiredParameters.get(PreSignedUrlHeader.RESPONSE_CONTENT_DISPOSITION));

            final GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getObjectRequestBuilder.build())
                    .signatureDuration(Duration.ofMinutes(preSignedUrlRequest.duration()))
                    .build();

            return s3Presigner.presignGetObject(presignRequest).url();
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while generating a pre-signed URL for the object - {}, Reason: {}", objectKeyName, exception.getMessage());
            throw ErrorCode.get(StorageErrorCodes.PRE_SIGNED_URL_GENERATION_FAILED, "i18n.attachment.object.url.generation.failed");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param versionDeleteRequest The request containing the details of the object version to delete.
     */
    @Override
    public void deleteObjectVersion(final ObjectVersionDeleteRequest versionDeleteRequest) {
        final String versionId = versionDeleteRequest.versionId();

        try {
            final DeleteObjectRequest.Builder deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(versionDeleteRequest.keyName());

            if (Objects.nonNull(versionId) && !versionId.isEmpty()) {
                deleteObjectRequest.versionId(versionId);
            }

            s3Client.deleteObject(deleteObjectRequest.build());
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while delete a object version of Id: {}, Reason: {}", versionId, exception.getMessage());
            LOGGER.debug("Storage: Stack: ", exception);
            throw ErrorCode.get(StorageErrorCodes.OBJECT_VERSION_DELETE_FAILED, "i18n.attachment.object.version.delete.failed");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Long> countObjectsAsync(final ObjectCountRequest countRequest) {
        return CompletableFuture.supplyAsync(
                () -> S3ObjectMetadataScanner.countObjects(s3Client, bucketName, countRequest),
                ObjectStorageAsyncExecutor.get()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Boolean> doesObjectExistAsync(final ObjectExistsRequest objectExistsRequest) {
        return CompletableFuture.supplyAsync(() -> doesObjectExists(objectExistsRequest), ObjectStorageAsyncExecutor.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ObjectListResponse> listObjectsAsync(final ObjectListRequest listRequest) {
        return CompletableFuture.supplyAsync(
                () -> S3ObjectMetadataScanner.listObjects(s3Client, bucketName, listRequest),
                ObjectStorageAsyncExecutor.get()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<Void> deleteObjectVersionAsync(final ObjectVersionDeleteRequest versionDeleteRequest) {
        return CompletableFuture.runAsync(() -> deleteObjectVersion(versionDeleteRequest), ObjectStorageAsyncExecutor.get());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompletableFuture<ObjectDownloadResponse> downloadObjectAsync(final ObjectDownloadRequest downloadRequest) {
        return CompletableFuture.supplyAsync(() -> downloadObject(downloadRequest), ObjectStorageAsyncExecutor.get());
    }

    private static boolean metadataMatches(final Map<String, String> metadata, final Map<String, String> metadataFilter) {
        if (Objects.isNull(metadataFilter) || metadataFilter.isEmpty()) {
            return true;
        }

        if (Objects.isNull(metadata) || metadata.isEmpty()) {
            return false;
        }

        for (final Map.Entry<String, String> entry : metadataFilter.entrySet()) {
            if (!Objects.equals(entry.getValue(), metadata.get(entry.getKey()))) {
                return false;
            }
        }

        return true;
    }
}
