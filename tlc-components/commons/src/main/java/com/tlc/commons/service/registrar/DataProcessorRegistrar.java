package com.tlc.commons.service.registrar;

import com.tlc.commons.annotation.InstanceProvider;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.data.annotation.RequestProcessor;
import com.tlc.commons.service.registrar.model.DataProcessorRegistrarData;
import com.tlc.commons.data.DataProcessorContainer;
import com.tlc.commons.data.processor.DataProcessor;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * <p>
 * The registrar that registers the given request processor class.
 * </p>
 *
 * @author Sathish
 * @version 1.0
 * @see ResourceRegistrar
 */
public class DataProcessorRegistrar implements ResourceRegistrar<DataProcessorRegistrarData> {

    private final DataProcessorContainer dataProcessorContainer;

    private DataProcessorRegistrar() {
        dataProcessorContainer = DataProcessorContainer.getInstance();
    }

    private static final class InstanceHolder {
        private static final DataProcessorRegistrar DATA_PROCESSOR_REGISTRAR = new DataProcessorRegistrar();
    }

    public static DataProcessorRegistrar getInstance() {
        return InstanceHolder.DATA_PROCESSOR_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link DataProcessorRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final DataProcessorRegistrarData registrarData) {
        final String moduleName = registrarData.getModuleName();

        for (final Class<?> dispatcherClass : registrarData.getClasses()) {
            final RequestProcessor requestProcessor = dispatcherClass.getAnnotation(RequestProcessor.class);


            if (Objects.nonNull(requestProcessor)) {
                for (final Method method : dispatcherClass.getMethods()) {

                    try {
                        if (method.isAnnotationPresent(InstanceProvider.class)
                                && method.invoke(null) instanceof final DataProcessor<?> dataProcessor) {
                            dataProcessorContainer.addProcessor(moduleName, requestProcessor.name(), dataProcessor);
                        }
                    } catch (Exception exception) {
                        throw ErrorCode.get(ErrorCodes.FAILED_TO_REGISTER_RDATA_PROCESSOR, exception);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link DataProcessorRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final DataProcessorRegistrarData registrarData) {
        dataProcessorContainer.clear(registrarData.getModuleName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterAll() {
        dataProcessorContainer.clearAll();
    }
}
