package com.tlc.cdc.core.transaction;

import com.tlc.attachment.service.ObjectStorageService;
import com.tlc.attachment.service.provider.StorageServiceProvider;
import com.tlc.attachment.storage.model.ObjectUploadRequest;
import com.tlc.attachment.storage.model.ObjectUploadResponse;
import com.tlc.attachment.storage.model.StorageVariant;
import com.tlc.cdc.core.models.CdcEventRecord;
import com.tlc.cdc.core.publisher.CdcAggregateEventPublisher;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonArray;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.util.RandomIdGenerator;

import java.io.ByteArrayInputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Processes CDC (Change Data Capture) events at the transaction level
 * by grouping, correlating, and finalizing related change events.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
public final class CdcTransactionProcessor {

    private static final String CDC_EVENT_STRING = "CDC_EVENT";
    private static final long SIZE_THRESHOLD = 1024 * 1024; // 1MB

    private final ConcurrentMap<Long, ConcurrentLinkedQueue<CdcEventRecord>> txnBuffer;
    private final ConcurrentMap<Long, AtomicLong> txnSizeMap;
    private final CdcAggregateEventPublisher cdcAggregateEventPublisher;
    private final ObjectStorageService objectStorageService;
    private final RandomIdGenerator randomIdGenerator;

    private CdcTransactionProcessor() {
        this.txnBuffer = new ConcurrentHashMap<>();
        this.txnSizeMap = new ConcurrentHashMap<>();
        this.cdcAggregateEventPublisher = CdcAggregateEventPublisher.getInstance();
        this.objectStorageService = StorageServiceProvider.get().getObjectStorageService();
        this.randomIdGenerator = RandomIdGenerator.getInstance();
    }

    private static class Instance {
        private static final CdcTransactionProcessor INSTANCE = new CdcTransactionProcessor();
    }

    /**
     * Gets the {@link CdcTransactionProcessor} instance.
     *
     * @return {@link CdcTransactionProcessor}.
     */
    public static CdcTransactionProcessor getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Adds a CDC event record to the transaction aggregation buffer.
     *
     * @param cdcEventRecord The incoming CDC event record to be aggregated.
     */
    public void addEvent(final CdcEventRecord cdcEventRecord) {
        final Long txnId = cdcEventRecord.transactionId();

        this.txnBuffer.computeIfAbsent(txnId, k -> new ConcurrentLinkedQueue<>()).offer(cdcEventRecord);
        final long eventSize = cdcEventRecord.payload().getBytes().length;

        txnSizeMap.computeIfAbsent(txnId, k -> new AtomicLong(0)).addAndGet(eventSize);

        if (txnSizeMap.get(txnId).get() >= SIZE_THRESHOLD) {
            publishTransaction(txnId);
        }
    }

    /**
     * Aggregates all buffered CDC events for a completed transaction
     * and publishes a single enriched event downstream.
     *
     * <p>After successful publication, the transaction buffer is cleared to prevent memory buildup.</p>
     * <p>This is called by CdcEventDispatcherScheduler every 5 seconds and also when size threshold is reached.</p>
     */
    public void aggregateAndPublish() {
        for (final Long transactionId : txnBuffer.keySet().stream().sorted().toList()) {
            publishTransaction(transactionId);
        }
    }

    /**
     * Publishes a single transaction's aggregated events.
     *
     * @param transactionId The transaction ID to publish
     */
    private void publishTransaction(final Long transactionId) {
        final ConcurrentLinkedQueue<CdcEventRecord> cdcEventRecords = txnBuffer.remove(transactionId);

        if (cdcEventRecords == null || cdcEventRecords.isEmpty()) {
            return;
        }

        final JsonArray jsonArray = Json.array();

        for (final CdcEventRecord cdcEventRecord : cdcEventRecords) {
            jsonArray.put(cdcEventRecord.payload());
        }
        cdcEventRecords.clear();

        final JsonObject jsonObject = Json.object().put("event", jsonArray);
        int size = jsonObject.getBytes().length;

        if (size >= SIZE_THRESHOLD) {
            final String key = randomIdGenerator.generateUniqueId();
            final ObjectUploadRequest uploadRequest = new ObjectUploadRequest.Builder(new ByteArrayInputStream(jsonObject.getBytes()), String.format("%s/%s/%s", CDC_EVENT_STRING, transactionId, key))
                    .withStorageVariant(StorageVariant.EVENT_STREAM)
                    .build();
            final ObjectUploadResponse uploadResponse = objectStorageService.uploadObject(uploadRequest);
            final JsonObject refJson = Json.object();

            refJson.put("refKey", key);
            refJson.put("objName", uploadResponse.objectName());
            refJson.put("versionId", uploadResponse.versionId());

            cdcAggregateEventPublisher.publish(transactionId, refJson);
        } else {
            cdcAggregateEventPublisher.publish(transactionId, jsonObject);
        }

        txnSizeMap.remove(transactionId);
    }
}
