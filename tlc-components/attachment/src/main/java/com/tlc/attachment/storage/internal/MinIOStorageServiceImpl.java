package com.tlc.attachment.storage.internal;

import com.tlc.attachment.service.ObjectStorageService;
import com.tlc.attachment.storage.model.ObjectCountRequest;
import com.tlc.attachment.storage.model.ObjectDownloadRequest;
import com.tlc.attachment.storage.model.ObjectExistsRequest;
import com.tlc.attachment.storage.model.ObjectListRequest;
import com.tlc.attachment.storage.model.ObjectListResponse;
import com.tlc.attachment.storage.model.ObjectStorageType;
import com.tlc.attachment.storage.internal.config.MinIOStorageConfig;
import com.tlc.attachment.storage.model.ObjectUploadRequest;
import com.tlc.attachment.storage.model.ObjectDownloadResponse;
import com.tlc.attachment.storage.model.ObjectUploadResponse;
import com.tlc.attachment.storage.model.ObjectVersionDeleteRequest;
import com.tlc.attachment.storage.model.PreSignedUrlRequest;
import com.tlc.attachment.storage.status.StorageErrorCodes;

import com.tlc.commons.code.ErrorCode;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectResponse;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketVersioningArgs;
import io.minio.ObjectWriteResponse;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.http.Method;
import io.minio.messages.VersioningConfiguration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.Objects;

/**
 * Provides the object storage services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class MinIOStorageServiceImpl implements ObjectStorageService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinIOStorageServiceImpl.class);

    private final MinioClient minioClient;
    private final String bucketName;

    MinIOStorageServiceImpl(final MinIOStorageConfig config) {
        this.minioClient = config.getClient();
        this.bucketName = config.getBucketName();
    }

    /**
     * Checks whether the bucket exists or not.
     *
     * @return True if bucket exists otherwise returns false.
     */
    private boolean isBucketExist() {
        try {
            return minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception exception) {
            throw ErrorCode.get(StorageErrorCodes.BUCKET_EXISTS_CHECK_ACTION_FAILED);
        }
    }

    /**
     * Creates the bucket to store the objects.
     */
    private void createBucket() {
        try {
            this.minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            this.setBucketVersioning();
        } catch (Exception exception) {
            throw ErrorCode.get(StorageErrorCodes.BUCKET_CREATION_FAILED);
        }
    }

    /**
     * Constructs a bucket VersioningConfiguration with given enable status.
     */
    private void setBucketVersioning() {
        try {
            this.minioClient.setBucketVersioning(SetBucketVersioningArgs.builder().bucket(bucketName)
                    .config(new VersioningConfiguration(VersioningConfiguration.Status.ENABLED, null)).build());
        } catch (Exception exception) {
            throw ErrorCode.get(StorageErrorCodes.BUCKET_VERSIONING_FAILED);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param downloadRequest Refers the {@link ObjectDownloadRequest}.
     * @return {@link GetObjectResponse} of object.
     */
    @Override
    public ObjectDownloadResponse downloadObject(final ObjectDownloadRequest downloadRequest) {
        if (isBucketExist()) {
            try {
                final InputStream inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName)
                        .versionId(downloadRequest.versionId()).object(downloadRequest.objectKeyName()).build());
                return new ObjectDownloadResponse(inputStream);
            } catch (Exception exception) {
                LOGGER.error("Storage: Encountered an issue while downloading the object - {}, Reason: {}", downloadRequest.objectKeyName(), exception.getMessage());
                throw ErrorCode.get(StorageErrorCodes.OBJECT_DOWNLOAD_FAILED, "i18n.attachment.object.download.failed");
            }
        } else {
            throw ErrorCode.get(StorageErrorCodes.BUCKET_NOT_FOUND);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param uploadRequest File content in stream.
     * @return {@link ObjectUploadResponse}.
     */
    @Override
    public ObjectUploadResponse uploadObject(final ObjectUploadRequest uploadRequest) {
        if (!isBucketExist()) {
            createBucket();
        }

        try (final InputStream inputStream = uploadRequest.getInputStream()) {
            final String objectKeyName = uploadRequest.getKeyName();
            final PreSignedUrlRequest preSignedUrlRequest = uploadRequest.getPreSignedUrlRequest();
            final ObjectWriteResponse writeResponse = minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKeyName)
                    .stream(inputStream, inputStream.available(), -1)
                    .build());

            URL presignedUrl = null;

            if (Objects.nonNull(preSignedUrlRequest)) {
                try {
                    final String urlString = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                            .bucket(bucketName)
                            .method(Method.GET)
                            .object(objectKeyName)
                            .expiry(preSignedUrlRequest.duration(), preSignedUrlRequest.timeUnit())
                            .build());

                    presignedUrl = new URL(urlString);
                } catch (Exception exception) {
                    LOGGER.error("Storage: Encountered an issue while generating a pre-signed URL for the object - {}, Reason: {}", objectKeyName, exception.getMessage());
                    throw ErrorCode.get(StorageErrorCodes.PRE_SIGNED_URL_GENERATION_FAILED, "i18n.attachment.object.url.generation.failed", exception);
                }
            }

            return new ObjectUploadResponse(writeResponse.versionId(), writeResponse.object(), presignedUrl, ObjectStorageType.MINIO);
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while uploading the object - {}, Reason: {}", uploadRequest.getKeyName(), exception.getMessage());
            throw ErrorCode.get(StorageErrorCodes.OBJECT_UPLOAD_FAILED, "i18n.attachment.object.upload.failed", exception);
        }
    }

    /**
     * Checks whether an object with the specified name exists.
     *
     * @param objectExistsRequest The {@code ObjectExistsRequest} containing the necessary details to check the object exists or not.
     * @return {@code true} if the object exists, {@code false} otherwise.
     */
    @Override
    public boolean doesObjectExists(final ObjectExistsRequest objectExistsRequest) {
        try {
            final StatObjectResponse statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName).object(objectExistsRequest.objectName()).build());

            return Objects.nonNull(statObjectResponse);
        } catch (Exception exception) {
            throw ErrorCode.get(StorageErrorCodes.OBJECT_NOT_FOUND, "i18n.attachment.object.exist.check.failed");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param preSignedUrlRequest The request object containing the necessary details
     *                            to generate the pre-signed URL.
     * @return the generated pre-signed URL.
     */
    @Override
    public URL getPresignedUrl(final PreSignedUrlRequest preSignedUrlRequest) {
        final String objectKeyName = preSignedUrlRequest.keyName();

        try {
            final String urlString = minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .method(Method.GET)
                    .object(objectKeyName)
                    .versionId(preSignedUrlRequest.versionId())
                    .expiry(preSignedUrlRequest.duration(), preSignedUrlRequest.timeUnit())
                    .build());

            return new URL(urlString);
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
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(versionDeleteRequest.keyName())
                    .versionId(versionDeleteRequest.versionId())
                    .build());
        } catch (Exception exception) {
            LOGGER.error("Storage: Encountered an issue while delete a object version, Reason: {}", exception.getMessage());
            throw ErrorCode.get(StorageErrorCodes.OBJECT_VERSION_DELETE_FAILED, "i18n.attachment.object.version.delete.failed");
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
