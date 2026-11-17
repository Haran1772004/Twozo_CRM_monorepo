package com.tlc.crm.server.starter.initializer;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.components.starter.initializer.Modules;
import com.tlc.crm.common.registrar.model.CrmServerRegistrarData;

import com.tlc.crm.marketplace.app.handler.MarketplaceAppHandler;
import com.tlc.crm.marketplace.app.initializer.CrmMarketplaceServiceInitializer;
import com.tlc.crm.marketplace.app.registrar.MarketplaceResourceRegistrar;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Map;

/**
 * Responsible for initiating the initialization process of marketplace module.
 *
 * @author Muthu kumar V
 * @version 1.0
 */
@Component
public class MarketplaceInitializer extends AbstractModuleInitializer {

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
        MarketplaceResourceRegistrar.getInstance().unregisterAll();
        CrmMarketplaceServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws Exception {
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void postInitialize() {
        final MarketplaceResourceRegistrar marketplaceResourceRegistrar = MarketplaceResourceRegistrar.getInstance();

        for (final Map.Entry<Modules, Collection<Class<?>>> registeredClassesByModule : fetchMarketplaceInitiatorRegisteredClasses().entrySet()) {
            final CrmServerRegistrarData crmServerRegistrarData = new CrmServerRegistrarData();
            final Modules modules = registeredClassesByModule.getKey();

            crmServerRegistrarData.setModuleId(modules.getId());
            crmServerRegistrarData.setModuleName(modules.getName());
            crmServerRegistrarData.setClasses(registeredClassesByModule.getValue());

            marketplaceResourceRegistrar.register(crmServerRegistrarData);
        }
        CrmMarketplaceServiceInitializer.getInstance().postInitialize(null);
        LOGGER.info("Application Initializer: Marketplace module is initialized successfully");
    }

    /**
     * Fetches the {@link MarketplaceAppHandler} registered {@link Class} files and organizes them into a map ordered by module.
     *
     * @return {@link Map} of modules with its {@link MarketplaceAppHandler} registered classes.
     */
    private Map<Modules, Collection<Class<?>>> fetchMarketplaceInitiatorRegisteredClasses() {
        try {
            final Collection<Class<?>> classes = classScanner.findClassesWithAnnotation("com.tlc.crm", MarketplaceAppHandler.class);

            return segregateClassesByModule(classes);
        } catch (Exception exception) {
            throw ErrorCode.get(ErrorCodes.INVALID_DATA);
        }
    }
}