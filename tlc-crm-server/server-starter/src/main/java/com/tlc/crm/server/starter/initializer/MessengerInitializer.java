package com.tlc.crm.server.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.crm.messenger.fields.MessengerFields;
import com.tlc.crm.messenger.initializer.MessengerServiceInitializer;

import org.springframework.stereotype.Component;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

/**
 * Responsible for initiating the initialization process of messenger module
 *
 * @author Maharaja S
 * @version 1.0
 */
@Component
public class MessengerInitializer extends AbstractModuleInitializer {

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
        MessengerServiceInitializer.getInstance().stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        MessengerServiceInitializer.getInstance().preInitialize(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        MessengerServiceInitializer.getInstance().postInitialize(getProperties(MessengerFields.MESSENGER_EXECUTOR));
        LOGGER.info("Application Initializer: Messenger module is initialized successfully");
    }
}