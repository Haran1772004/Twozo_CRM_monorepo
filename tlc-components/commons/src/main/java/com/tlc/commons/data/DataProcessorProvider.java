package com.tlc.commons.data;

import com.tlc.commons.data.model.ConsumableData;
import com.tlc.commons.data.processor.DataProcessor;

/**
 * Provides centralized access to data processors by URI path.
 *
 * @author Sathish
 * @version 1.0
 */
public final class DataProcessorProvider {

    private static final DataProcessorContainer DATA_PROCESSOR_CONTAINER;

    static {
        DATA_PROCESSOR_CONTAINER = DataProcessorContainer.getInstance();
    }

    /**
     * Retrieves the registered request processor for a given request path.
     *
     * @param name The name used to resolve and select the data processor implementation.
     * @return The resolved {@link DataProcessor} instance, or null if not registered for the given name.
     */
    public static <T extends ConsumableData> DataProcessor<T> getProcessor(final String name) {
        return DATA_PROCESSOR_CONTAINER.getProcessor(name);
    }
}
