package com.tlc.cdc.core.registrar;

import com.tlc.cdc.core.event.CdcEventHandler;
import com.tlc.cdc.core.event.CdcEventHandlerRegistry;
import com.tlc.cdc.core.event.CdcEvent;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registrar.ResourceRegistrar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Collection;

/**
 * The registrar that registers the CDC events.
 *
 * @author Thalaimalai Pandiyan T
 * @version 1.0
 * @see ResourceRegistrar
 */
public final class CdcResourceRegistrar implements ResourceRegistrar<CdcRegistrarData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdcResourceRegistrar.class);

    private final CdcEventHandlerRegistry cdcEventHandlerRegistry;

    private CdcResourceRegistrar() {
        this.cdcEventHandlerRegistry = CdcEventHandlerRegistry.getInstance();
    }

    private static final class Instance {
        private static final CdcResourceRegistrar RESOURCE_REGISTRAR = new CdcResourceRegistrar();
    }

    public static CdcResourceRegistrar getInstance() {
        return Instance.RESOURCE_REGISTRAR;
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CdcRegistrarData} which is needed for registering the resources/actions.
     */
    @Override
    public void register(final CdcRegistrarData registrarData) {
        final Collection<Class<?>> classes = registrarData.getClasses();
        final String moduleName = registrarData.getModuleName();

        try {
            for (final Class<?> classObj : classes) {
                final String className = classObj.getName().replace('.', '/') + ".class";

                if (className.contains("/cdc/")) {
                    final CdcEvent annotation = classObj.getAnnotation(CdcEvent.class);

                    if (annotation != null) {
                        final Constructor<CdcEventHandler> constructor = (Constructor<CdcEventHandler>) classObj.getConstructor();

                        cdcEventHandlerRegistry.addAction(moduleName, annotation.event(), constructor.newInstance());
                        LOGGER.info("CDC: Registering CDC event listener: {}, Module : {}", annotation.event(), moduleName);
                    }
                }
            }
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA, exception);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param registrarData Holds the {@link CdcRegistrarData} which is needed for removing the resources/actions.
     */
    @Override
    public void unregister(final CdcRegistrarData registrarData) {
        cdcEventHandlerRegistry.remove(registrarData.getModuleName());
    }
}
