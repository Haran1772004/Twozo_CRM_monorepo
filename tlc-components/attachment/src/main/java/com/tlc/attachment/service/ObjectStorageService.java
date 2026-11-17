package com.tlc.attachment.service;

import com.tlc.attachment.storage.model.ObjectCountRequest;
import com.tlc.attachment.storage.model.ObjectDownloadRequest;
import com.tlc.attachment.storage.model.ObjectDownloadResponse;
import com.tlc.attachment.storage.model.ObjectExistsRequest;
import com.tlc.attachment.storage.model.ObjectListRequest;
import com.tlc.attachment.storage.model.ObjectListResponse;
import com.tlc.attachment.storage.model.ObjectUploadResponse;
import com.tlc.attachment.storage.model.ObjectUploadRequest;
import com.tlc.attachment.storage.model.ObjectVersionDeleteRequest;
import com.tlc.attachment.storage.model.PreSignedUrlRequest;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * An interface which provides the object storage services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface ObjectStorageService {

    /**
     * Uploads contents from a file as object in bucket.
     *
     * @param uploadRequest Refers the {@link ObjectUploadRequest}.
     * @return {@link ObjectUploadResponse}.
     */
    ObjectUploadResponse uploadObject(final ObjectUploadRequest uploadRequest);

    /**
     * Downloads the data of an object to a file.
     *
     * @param downloadRequest Refers the {@link ObjectDownloadRequest}.
     * @return {@link ObjectDownloadResponse} of object.
     */
    ObjectDownloadResponse downloadObject(final ObjectDownloadRequest downloadRequest);

    /**
     * Checks whether an object with the specified name exists.
     *
     * @param objectExistsRequest The {@code ObjectExistsRequest} containing the necessary details to check the object exists or not.
     * @return {@code true} if the object exists, {@code false} otherwise.
     */
    boolean doesObjectExists(final ObjectExistsRequest objectExistsRequest);

    /**
     * Generates a pre-signed URL based on the provided request details.
     *
     * @param preSignedUrlRequest The request object containing the necessary details
     *                            to generate the pre-signed URL.
     * @return the generated pre-signed URL.
     */
    URL getPresignedUrl(final PreSignedUrlRequest preSignedUrlRequest);

    /**
     * Deletes a specific version of an object in the storage system.
     * The request should contain the necessary details such as the object key (name) and the version ID to identify the
     * specific version of the object to be deleted.
     *
     * @param versionDeleteRequest The request containing the details of the object version to delete.
     */
    void deleteObjectVersion(final ObjectVersionDeleteRequest versionDeleteRequest);

    /**
     * Counts objects matching the optional prefix and metadata filter asynchronously.
     *
     * @param countRequest the count request
     * @return matching object count
     */
    CompletableFuture<Long> countObjectsAsync(final ObjectCountRequest countRequest);

    /**
     * Checks object existence asynchronously, optionally matching metadata filter values.
     *
     * @param objectExistsRequest the existence request
     * @return {@code true} when the object exists and matches the filter
     */
    CompletableFuture<Boolean> doesObjectExistAsync(final ObjectExistsRequest objectExistsRequest);

    /**
     * Lists object summaries matching the optional prefix and metadata filter asynchronously.
     *
     * @param listRequest the list request
     * @return object summaries without content
     */
    CompletableFuture<ObjectListResponse> listObjectsAsync(final ObjectListRequest listRequest);

    /**
     * Deletes an object version asynchronously.
     *
     * @param versionDeleteRequest the delete request
     * @return completion future
     */
    CompletableFuture<Void> deleteObjectVersionAsync(final ObjectVersionDeleteRequest versionDeleteRequest);

    /**
     * Downloads an object asynchronously.
     *
     * @param downloadRequest the download request
     * @return download response
     */
    CompletableFuture<ObjectDownloadResponse> downloadObjectAsync(final ObjectDownloadRequest downloadRequest);
}
