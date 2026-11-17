package com.tlc.commons.data.processor;

import com.tlc.commons.data.model.ConsumableData;
import com.tlc.commons.json.JsonObject;
import com.tlc.commons.data.consumer.DataConsumer;
import com.tlc.commons.data.model.RequestData;

/**
 * Interface representing a request processor for processing the request data.
 *
 * @param <T> Type of request data to process
 * @author Sathish
 * @version 1.0
 */
public interface DataProcessor<T extends ConsumableData> {

    /**
     * Processes the given request and performs the necessary operations using the provided data consumer.
     *
     * <p>This method serves as the main entry point for handling API requests. It receives a {@link RequestData}
     * object containing the request payload and metadata, and a {@link DataConsumer} that defines how the processed
     * data should be consumed or handled (e.g., stored, transformed, or passed downstream).</p>
     *
     * @param requestData  the request data object containing input details in JSON format
     * @param dataConsumer the consumer responsible for processing or handling the resulting data
     */
    void process(final RequestData<JsonObject> requestData, final DataConsumer<T> dataConsumer);
}
