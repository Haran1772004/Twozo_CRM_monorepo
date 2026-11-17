package com.tlc.cdc.core.consumer;

import com.tlc.cdc.core.models.CdcEventRecord;
import com.tlc.cdc.core.transaction.CdcTransactionProcessor;

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
final class CdcRequestProcessor implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdcRequestProcessor.class);

    private final CdcTransactionProcessor cdcTransactionProcessor;

    public CdcRequestProcessor() {
        this.cdcTransactionProcessor = CdcTransactionProcessor.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage Refers the stream message.
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        final JsonObject jsonObject = Json.object(streamMessage.data());

        final long transactionId = jsonObject.getJsonObject("source").getLong("txId");
        cdcTransactionProcessor.addEvent(new CdcEventRecord(transactionId, jsonObject));
        LOGGER.debug("CDC: Event consumed from the CDC. Transaction ID: {}", transactionId);
    }
}
