package com.tlc.commons.data.consumer;

import com.tlc.commons.data.model.ConsumableData;

/**
 * Functional interface representing a consumer for request data.
 * Defines a single method to consume request data of a specific type.
 *
 * @param <T> The type of the request data to consume
 * @author Sathish
 * @version 1.0
 */
@FunctionalInterface
public interface DataConsumer<T extends ConsumableData> {

    /**
     * Consumes processed request data.
     *
     * @param data the request data to be consumed
     */
    void consume(final T data);
}
