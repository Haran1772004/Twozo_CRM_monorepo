package com.tlc.commons.data;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.data.model.ConsumableData;
import com.tlc.commons.data.processor.DataProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Container class for managing and registering webhook processors.
 * Supports multiple groups of webhook processors indexed by group and processor names.
 * Provides thread-safe methods for adding, retrieving, and clearing processors.
 *
 * @author Sathish
 * @version 1.0
 */
public class DataProcessorContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataProcessorContainer.class);
    private final Map<String, Map<String, DataProcessor<?>>> dataProcessorMap;

    private DataProcessorContainer() {
        dataProcessorMap = new ConcurrentHashMap<>();
    }

    private static class Instance {
        private static final DataProcessorContainer INSTANCE = new DataProcessorContainer();
    }

    public static DataProcessorContainer getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Registers a request processor under the specified group and name.
     *
     * @param groupName      the group name under which to register the request processor
     * @param processorName unique name of the request processor
     * @param requestProcessor the request processor instance
     * @throws ErrorCode if a request processor with the same name already exists in the group
     */
    public final void addProcessor(final String groupName, final String processorName, final DataProcessor<?> requestProcessor) {
        final Map<String, DataProcessor<?>> dataProcessorMap = this.dataProcessorMap.computeIfAbsent(groupName, group -> new ConcurrentHashMap<>());
        final DataProcessor<?> dataProcessor = dataProcessorMap.get(processorName);

        if (Objects.isNull(dataProcessor)) {
            LOGGER.info("Registering the request data processor {}", processorName);
            dataProcessorMap.put(processorName, requestProcessor);
        } else {
            throw ErrorCode.get(ErrorCodes.DATA_PROCESSOR_ALREADY_REGISTERED, " Request data processor name : " + processorName);
        }
    }

    /**
     * Retrieves a request processor by its unique name across all groups.
     *
     * @param name the name of the processor to retrieve
     * @param <T>  the type of ConsumableData expected
     * @return the request processor instance if found
     */
    public <T extends ConsumableData> DataProcessor<T> getProcessor(final String name) {
        final Collection<Map<String, DataProcessor<?>>> dataProcessors = this.dataProcessorMap.values();

        for (final Map<String, DataProcessor<?>> dataProcessorMap : dataProcessors) {
            if (dataProcessorMap.containsKey(name)) {
                return (DataProcessor<T>) dataProcessorMap.get(name);
            }
        }
        throw ErrorCode.get(ErrorCodes.DATA_PROCESSOR_NOT_FOUND, " Request data processor name : " + name);
    }

    /**
     * Removes all request processor registered under the specified group.
     *
     * @param groupName the group name whose processors are to be removed
     */
    public void clear(final String groupName) {
        dataProcessorMap.remove(groupName);
    }

    /**
     * Removes all registered request processors across all groups.
     */
    public void clearAll() {
        dataProcessorMap.clear();
    }
}