package com.tlc.components.starter.initializer.components;

import com.tlc.sync.service.oauth.internal.SyncInitializationServiceImpl;
import com.tlc.sync.service.oauth.internal.controller.TokenRefreshService;
import com.tlc.sync.service.oauth.internal.registry.SyncServiceRegistry;
import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.Configurations;
import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;
import com.tlc.sync.service.oauth.service.SyncInitializationService;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Responsible for initiating the initialization process of sync services.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
@Component
public class SyncServiceInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<SyncInitializationService> syncServiceRegistry;

    public SyncServiceInitializer() {
        this.syncServiceRegistry = SyncServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.SYNC_SERVICE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return syncServiceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final SyncInitializationService syncInitializationService = new SyncInitializationServiceImpl();
            final Configurations configurations = getProperties(ComponentsStarterFields.SYNC_SERVICE);

            syncInitializationService.start(configurations);
            syncServiceRegistry.register(syncInitializationService);
        }

        if (isServiceEnabled(ComponentsStarterFields.TOKEN_REFRESH)) {
            TokenRefreshService.getInstance().start(null);
        }
    }
}
