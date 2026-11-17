package com.tlc.crm.core.internal.async.bulk.processor;

import com.tlc.commons.json.Json;
import com.tlc.crm.common.config.manager.AbstractConfigManager;
import com.tlc.crm.common.handler.EventDataHandler;
import com.tlc.crm.core.internal.async.bulk.processor.model.AsyncActionRequest;
import com.tlc.queue.consumer.StreamListener;
import com.tlc.queue.consumer.StreamMessage;
import com.tlc.validator.TlcModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

/**
 * <p>
 * Abstract class for handling asynchronous operations on models
 * </p>
 *
 * @param <T> The type of model that extends {@link TlcModel}
 * @author Kishore J
 * @version 1.0
 */
public abstract class AsyncActionProcessor<T extends TlcModel> implements StreamListener<byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AsyncActionProcessor.class);

    protected final EventDataHandler eventDataHandler;

    protected AsyncActionProcessor() {
        eventDataHandler = EventDataHandler.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param streamMessage the stream message containing update data
     */
    @Override
    public void listen(final StreamMessage<byte[]> streamMessage) {
        LOGGER.info("Bulk Action: The consumer has started processing the messages...");
        final String[] decodedKeyParts = new String(Base64.getDecoder().decode(streamMessage.id().getBytes())).split(":");

        process(new AsyncActionRequest(Long.parseLong(decodedKeyParts[0]), Long.parseLong(decodedKeyParts[1]),
                Long.parseLong(decodedKeyParts[2]), Json.object(streamMessage.data())));
        LOGGER.info("Bulk Action: The consumer has finished processing all the messages...");
    }

    /**
     * <p>
     * Processes the asynchronous action request
     * </p>
     *
     * @param asyncActionRequest Represents the {@link AsyncActionRequest}
     */
    protected abstract void process(final AsyncActionRequest asyncActionRequest);

    /**
     * <p>
     * Fetches the {@link AbstractConfigManager} instance
     * </p>
     *
     * @return The {@link AbstractConfigManager} instance
     */
    protected abstract AbstractConfigManager<T> getAbstractConfigManager();
}
