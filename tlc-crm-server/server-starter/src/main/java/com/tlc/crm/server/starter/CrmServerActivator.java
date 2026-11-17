package com.tlc.crm.server.starter;

import com.tlc.components.starter.ApplicationStarter;

import com.tlc.crm.server.starter.initializer.CrmServerInitializer;
import com.tlc.crm.server.starter.initializer.CrmServerModules;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Serves as the entry point for activating the CRM Server application by extending {@link ApplicationStarter}, ensuring
 * that the component services are loaded first before initiating the application startup process.
 *
 * @author ThalaimalaiPandiyanT
 * @author Sathish
 * @version 1.0
 */
class CrmServerActivator extends ApplicationStarter {

    @Autowired
    private CrmServerInitializer crmServerInitializer;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void register() {
        moduleRegistry.register(List.of(CrmServerModules.values()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() {
        crmServerInitializer.initialize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void postInit() throws Exception {
        crmServerInitializer.postInitialize();
    }
}
