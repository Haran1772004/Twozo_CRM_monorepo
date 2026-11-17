package com.tlc.iam.starter;

import com.tlc.components.starter.ApplicationStarter;

import com.tlc.iam.starter.initializer.IAMModules;
import com.tlc.iam.starter.initializer.IamInitializer;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Serves as the entry point for activating the IAM application by extending {@link ApplicationStarter}, ensuring
 * that the component services are loaded first before initiating the IAM application startup process.
 *
 * @author ThalaimalaiPandiyanT
 * @author Sathish
 * @version 1.0
 */
class IAMActivator extends ApplicationStarter {

    @Autowired
    private IamInitializer iamInitializer;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void register() {
        moduleRegistry.register(List.of(IAMModules.values()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void init() {
        iamInitializer.initialize();
    }

    /**
     * {@inheritDoc}
     *
     * @throws Exception
     */
    @Override
    protected void postInit() throws Exception {
        iamInitializer.postInitialize();
    }
}
