package com.tlc.crm.server.starter.initializer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.util.ConfHolder;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;

import com.tlc.crm.common.fields.ApplicationFields;
import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;

import com.tlc.crm.core.action.service.annotation.CoreAction;
import com.tlc.crm.core.initializer.CoreServiceInitializer;
import com.tlc.crm.core.registrar.CoreResourceRegistrar;
import com.tlc.crm.entity.models.Entity;

import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Responsible for initiating the initialization process of core services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
final class CoreInitializer extends AbstractModuleInitializer {

    private static final String CONF_KEY = "core";

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        CoreResourceRegistrar.getInstance().unregisterAll();
        CoreServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled(ApplicationFields.ENTITY_URL)) {
            final ConfHolder confHolder = ConfHolder.getInstance();

            Entity.setURLS(
                    confHolder.getProperties(ApplicationFields.WEB)
                            .getConfig(ApplicationFields.DEFAULT_DOMAIN),
                    confHolder.getProperties(ApplicationFields.ENTITY_URL)
            );
        }
        CoreServiceInitializer.getInstance().preInitialize(null);
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void postInitialize() {
        final CoreResourceRegistrar coreRegistrar = CoreResourceRegistrar.getInstance();

        for (final Map.Entry<Modules, Collection<Class<?>>> registeredClassesByModule : fetchCoreActionRegisteredClasses().entrySet()) {
            final CrmServerRegistrarData crmServerRegistrarData = new CrmServerRegistrarData();
            final Modules modules = registeredClassesByModule.getKey();

            crmServerRegistrarData.setModuleId(modules.getId());
            crmServerRegistrarData.setModuleName(modules.getName());
            crmServerRegistrarData.setClasses(registeredClassesByModule.getValue());

            coreRegistrar.register(crmServerRegistrarData);
        }

        CoreServiceInitializer.getInstance().postInitialize(getProperties(CONF_KEY));
        LOGGER.info("Application Initializer: Core module is initialized successfully");
    }

    /**
     * Fetches the {@link CoreAction} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link CoreAction} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchCoreActionRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc", CoreAction.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }
}
