package com.tlc.components.starter.initializer.components;

import com.tlc.cdc.core.connect.ConnectOrchestrator;
import com.tlc.cdc.core.event.CdcEvent;
import com.tlc.cdc.core.initializer.CdcCoreServiceInitializer;
import com.tlc.cdc.core.registrar.CdcRegistrarData;
import com.tlc.cdc.core.registrar.CdcResourceRegistrar;
import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Responsible for triggering the initialization process of the CDC services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
public class CdcCoreInitializer extends AbstractModuleInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CdcCoreInitializer.class);

    @Override
    public void initialize() {
        if (isServiceEnabled(ComponentsStarterFields.KAFKA_CONNECT)) {
            final Configurations configurations = getProperties(ComponentsStarterFields.KAFKA_CONNECT);
            final ConnectOrchestrator connectOrchestrator = ConnectOrchestrator.getInstance();

            connectOrchestrator.start(configurations);
        }
        LOGGER.info("Application Initializer: Initialization of CDC core module success");
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception If an error occurs during the post-initialization process.
     */
    @Override
    public void postInitialize() {
        if (isServiceEnabled()) {
            final CdcResourceRegistrar cdcResourceRegistrar = CdcResourceRegistrar.getInstance();

            for (final Map.Entry<Modules, Collection<Class<?>>> registeredClassesByModule : fetchRegisteredClasses().entrySet()) {
                final CdcRegistrarData cdcRegistrarData = new CdcRegistrarData();
                final Modules modules = registeredClassesByModule.getKey();

                cdcRegistrarData.setModuleId(modules.getId());
                cdcRegistrarData.setModuleName(modules.getName());
                cdcRegistrarData.setClasses(registeredClassesByModule.getValue());
                cdcResourceRegistrar.register(cdcRegistrarData);
            }
            CdcCoreServiceInitializer.getInstance().postInitialize(null);

            LOGGER.info("Application Initializer: Post initialization of CDC core module success");
        }
    }

    /**
     * Gracefully shuts down the module, releasing any resources or stopping services that were started during initialization.
     * Subclasses should override this method to provide custom shutdown logic.
     */
    @Override
    public void shutdown() {
        if (isServiceEnabled()) {
            final CdcResourceRegistrar cdcResourceRegistrar = CdcResourceRegistrar.getInstance();

            for (final Map.Entry<Modules, Collection<Class<?>>> registeredClassesByModule : fetchRegisteredClasses().entrySet()) {
                final CdcRegistrarData cdcRegistrarData = new CdcRegistrarData();
                final Modules modules = registeredClassesByModule.getKey();

                cdcRegistrarData.setModuleId(modules.getId());
                cdcRegistrarData.setModuleName(modules.getName());
                cdcRegistrarData.setClasses(registeredClassesByModule.getValue());
                cdcResourceRegistrar.unregister(cdcRegistrarData);
            }
        }

        if (isServiceEnabled(ComponentsStarterFields.KAFKA_CONNECT)) {
            ConnectOrchestrator.getInstance().stop();
        }
        LOGGER.info("Application Initializer: Shutdown of CDC core module success");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.CDC_CORE);
    }

    /**
     * Fetches the {@link CdcEvent} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link CdcEvent} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", CdcEvent.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }
}
