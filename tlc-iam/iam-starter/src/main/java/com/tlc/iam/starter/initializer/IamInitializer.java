package com.tlc.iam.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.iam.server.initializer.IamServerInitializer;

import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for triggering the initialization process of the IAM services within the application.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
@Component
public final class IamInitializer extends AbstractModuleInitializer {

    private static final String IAM_SERVER_KEY = "iam-server";

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(IAM_SERVER_KEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (isServiceEnabled()) {
            IamServerInitializer.getInstance().stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            IamServerInitializer.getInstance().preInitialize(getProperties(IAM_SERVER_KEY));
        }
    }

    /**
     * {@inheritDoc}
     *
     */
    @Override
    public void postInitialize() {
        if (isServiceEnabled()) {
            IamServerInitializer.getInstance().postInitialize(null);
        }
    }
}
