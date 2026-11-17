package com.tlc.attachment.storage.internal;

import com.tlc.attachment.storage.model.ObjectCountRequest;
import com.tlc.attachment.storage.model.ObjectListRequest;
import com.tlc.attachment.storage.model.ObjectListResponse;
import com.tlc.attachment.storage.model.ObjectSummary;
import com.tlc.commons.code.ErrorCode;
import com.tlc.attachment.storage.status.StorageErrorCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Scans an S3 bucket by key and filters objects using user-defined metadata (x-amz-meta-* headers).
 *
 * <p>S3 does not expose object count or metadata-filtered listings in a single API call.
 * {@code ListObjectsV2} returns keys only; metadata requires {@code HeadObject} per key.
 * Bucket-level metrics (for example CloudWatch {@code NumberOfObjects}) and S3 Inventory are
 * not suitable for live, metadata-filtered counts. This scanner therefore paginates
 * {@code ListObjectsV2} and issues {@code HeadObject} for each candidate key.
 *
 * <p>Required IAM permissions match existing upload/download flows:
 * {@code s3:ListBucket} on the bucket and {@code s3:GetObject} on object keys
 * ({@code HeadObject} is authorized via {@code s3:GetObject}).
 *
 * @author sundar
 * @version 1.0
 */
final class S3ObjectMetadataScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3ObjectMetadataScanner.class);

    private static final int LIST_PAGE_SIZE = 1000;

    private S3ObjectMetadataScanner() {
    }

    /**
     * Counts objects in the bucket whose user metadata matches {@code request.metadataFilter()}.
     *
     * <p>There is no S3 API that returns a metadata-filtered object count directly. This method
     * walks every key under the optional prefix via paginated {@code ListObjectsV2} and increments
     * the count only when {@link #matchesMetadataFilter} succeeds for that key.
     *
     * @param s3Client   configured S3 client
     * @param bucketName target bucket
     * @param request    optional key prefix and required metadata filter
     * @return number of objects matching the filter
     */
    static long countObjects(final S3Client s3Client, final String bucketName, final ObjectCountRequest request) {
        long count = 0;
        String continuationToken = null;
        boolean truncated = true;

        while (truncated) {
            final ListObjectsV2Response listResponse = listPage(s3Client, bucketName, request.prefix(), continuationToken);

            for (final S3Object s3Object : listResponse.contents()) {
                if (matchesMetadataFilter(s3Client, bucketName, s3Object.key(), request.metadataFilter())) {
                    count++;
                }
            }

            truncated = Boolean.TRUE.equals(listResponse.isTruncated());
            continuationToken = listResponse.nextContinuationToken();
        }

        return count;
    }

    /**
     * Returns up to {@code request.maxKeys()} object summaries whose metadata matches the filter.
     *
     * <p>Listing is paginated internally with {@code ListObjectsV2}; each candidate key is
     * resolved via {@link #toSummaryIfMatches}. The response includes a continuation token when
     * more matching objects may remain in the bucket.
     *
     * @param s3Client   configured S3 client
     * @param bucketName target bucket
     * @param request    prefix, metadata filter, max keys (capped at {@link ObjectListRequest#MAX_KEYS_LIMIT}), and optional continuation token
     * @return matching summaries and pagination hint for the next page
     */
    static ObjectListResponse listObjects(final S3Client s3Client, final String bucketName, final ObjectListRequest request) {
        final int maxKeys = Math.min(Math.max(request.maxKeys(), 1), ObjectListRequest.MAX_KEYS_LIMIT);
        final List<ObjectSummary> summaries = new ArrayList<>(maxKeys);
        String continuationToken = request.continuationToken();
        boolean truncated = true;

        while (truncated && summaries.size() < maxKeys) {
            final ListObjectsV2Response listResponse = listPage(s3Client, bucketName, request.prefix(), continuationToken);

            for (final S3Object s3Object : listResponse.contents()) {
                if (summaries.size() >= maxKeys) {
                    break;
                }

                final ObjectSummary summary = toSummaryIfMatches(s3Client, bucketName, s3Object.key(), request.metadataFilter());

                if (Objects.nonNull(summary)) {
                    summaries.add(summary);
                }
            }

            continuationToken = listResponse.nextContinuationToken();
            truncated = Boolean.TRUE.equals(listResponse.isTruncated());
        }

        final boolean hasMore = truncated && Objects.nonNull(continuationToken);
        return new ObjectListResponse(List.copyOf(summaries), hasMore ? continuationToken : null, hasMore);
    }

    /**
     * Fetches one page of object keys from S3 using {@code ListObjectsV2}.
     *
     * @param s3Client          configured S3 client
     * @param bucketName        target bucket
     * @param prefix            optional key prefix; omitted when null or empty
     * @param continuationToken optional pagination token from a prior page
     * @return raw list response for the page
     */
    private static ListObjectsV2Response listPage(
            final S3Client s3Client,
            final String bucketName,
            final String prefix,
            final String continuationToken
    ) {
        try {
            final ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .maxKeys(LIST_PAGE_SIZE);

            if (Objects.nonNull(prefix) && !prefix.isEmpty()) {
                builder.prefix(prefix);
            }

            if (Objects.nonNull(continuationToken) && !continuationToken.isEmpty()) {
                builder.continuationToken(continuationToken);
            }

            return s3Client.listObjectsV2(builder.build());
        } catch (Exception exception) {
            LOGGER.error("Storage: Failed to list objects in bucket {}, reason: {}", bucketName, exception.getMessage());
            throw ErrorCode.get(StorageErrorCodes.OBJECT_LIST_ACTION_FAILED, exception);
        }
    }

    /**
     * Loads metadata for a single key and returns an {@link ObjectSummary} when the filter matches.
     *
     * <p>Uses {@code HeadObject} only (no object body download). Missing or inaccessible objects
     * are skipped with a debug log rather than failing the entire scan.
     *
     * @param s3Client        configured S3 client
     * @param bucketName      target bucket
     * @param objectKey       object key to inspect
     * @param metadataFilter  required metadata key/value pairs
     * @return summary when metadata matches; {@code null} when the object does not match or cannot be read
     */
    private static ObjectSummary toSummaryIfMatches(
            final S3Client s3Client,
            final String bucketName,
            final String objectKey,
            final Map<String, String> metadataFilter
    ) {
        try {
            final HeadObjectResponse headObjectResponse = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build());

            final Map<String, String> metadata = headObjectResponse.metadata();

            if (!metadataMatches(metadata, metadataFilter)) {
                return null;
            }

            return new ObjectSummary(
                    objectKey,
                    headObjectResponse.versionId(),
                    headObjectResponse.lastModified().toEpochMilli(),
                    metadata
            );
        } catch (Exception exception) {
            LOGGER.debug("Storage: Skipping object {} during metadata scan, reason: {}", objectKey, exception.getMessage());
            return null;
        }
    }

    /**
     * Returns whether the object at {@code objectKey} satisfies {@code metadataFilter}.
     *
     * @param s3Client       configured S3 client
     * @param bucketName     target bucket
     * @param objectKey      object key to test
     * @param metadataFilter metadata key/value pairs that must all match
     * @return {@code true} when a matching summary can be built for the key
     */
    private static boolean matchesMetadataFilter(
            final S3Client s3Client,
            final String bucketName,
            final String objectKey,
            final Map<String, String> metadataFilter
    ) {
        return Objects.nonNull(toSummaryIfMatches(s3Client, bucketName, objectKey, metadataFilter));
    }

    /**
     * Tests whether {@code metadata} contains every entry in {@code metadataFilter} with equal values.
     *
     * <p>An empty or null filter matches any metadata map (including null/empty metadata).
     *
     * @param metadata       user metadata from {@code HeadObject}
     * @param metadataFilter required key/value pairs
     * @return {@code true} when all filter entries are present with matching values
     */
    private static boolean metadataMatches(final Map<String, String> metadata, final Map<String, String> metadataFilter) {
        if (Objects.isNull(metadataFilter) || metadataFilter.isEmpty()) {
            return true;
        }

        if (Objects.isNull(metadata) || metadata.isEmpty()) {
            return false;
        }

        for (final Map.Entry<String, String> entry : metadataFilter.entrySet()) {
            final String actualValue = metadata.get(entry.getKey());

            if (!Objects.equals(entry.getValue(), actualValue)) {
                return false;
            }
        }

        return true;
    }
}
