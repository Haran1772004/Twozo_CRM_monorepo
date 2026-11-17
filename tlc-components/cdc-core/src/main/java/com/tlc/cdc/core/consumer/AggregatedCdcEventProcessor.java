package com.tlc.cdc.core.consumer;

import com.tlc.attachment.service.ObjectStorageService;
import com.tlc.attachment.service.provider.StorageServiceProvider;
import com.tlc.attachment.storage.model.ObjectDownloadRequest;
import com.tlc.attachment.storage.model.ObjectDownloadResponse;
import com.tlc.attachment.storage.model.ObjectVersionDeleteRequest;
import com.tlc.attachment.storage.model.StorageVariant;

import com.tlc.cdc.core.event.CdcEventHandler;
import com.tlc.cdc.core.event.CdcEventHandlerRegistry;
import com.tlc.cdc.core.models.AggregatedCdcEventRequest;

import com.tlc.commons.json.Json;
import com.tlc.commons.json.JsonObject;

import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Kafka Stream listener responsible for processing aggregated CDC (Change Data Capture) events.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 */
final class AggregatedCdcEventProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AggregatedCdcEventProcessor.class);

    private static final String REF_KEY = "refKey";
    private static final String OBJ_NAME = "objName";
    private static final String VERSION_ID = "versionId";

    private final CdcEventHandlerRegistry cdcEventHandlerRegistry;
    private final ObjectStorageService storageService;

    AggregatedCdcEventProcessor() {
        this.cdcEventHandlerRegistry = CdcEventHandlerRegistry.getInstance();
        this.storageService = StorageServiceProvider.get().getObjectStorageService();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Refers the stream message.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        try {
            final JsonObject rawData = Json.object(streamMessage.data());
            final String id = streamMessage.id();
            final StorageVariant storageVariant = StorageVariant.EVENT_STREAM;
            final AggregatedCdcEventRequest request;
            String objectName = null;

            if (rawData.containsKey(REF_KEY)) {
                final ObjectDownloadResponse response = storageService.downloadObject(
                        new ObjectDownloadRequest(rawData.getString(VERSION_ID), rawData.getString(OBJ_NAME), storageVariant)
                );

                request = new AggregatedCdcEventRequest(id, Json.object(response.inputStream()));
                objectName = rawData.getString(OBJ_NAME);
            } else {
                request = new AggregatedCdcEventRequest(id, rawData);
            }

            for (final CdcEventHandler cdcEventHandler : cdcEventHandlerRegistry.getAll()) {
                cdcEventHandler.handle(request);
            }

            if (objectName != null) {
                try {
                    storageService.deleteObjectVersion(new ObjectVersionDeleteRequest(objectName, null, storageVariant));
                } catch (final Exception exception) {
                    LOGGER.error("Aggregated CDC: Failed to delete processed CDC payload from storage. Obj: {}, ID: {}",
                            objectName, id, exception);
                }
            }
        } catch (Throwable throwable) {
            LOGGER.error("Aggregated CDC: Error occurred in the CDC event handler. ID: {}, Reason: {}",
                    streamMessage.id(), throwable.getMessage(), throwable);
        } finally {
            streamMessage.clean();
        }
    }
}
