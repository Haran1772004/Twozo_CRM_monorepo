package com.tlc.crm.server.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.crm.email.core.fields.EmailLinkerFields;
import com.tlc.crm.email.core.initializer.EmailCoreInitializer;

import com.tlc.email.common.service.EmailAddOnService;
import org.springframework.stereotype.Component;

/**
 * Initializes the email linker.
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractModuleInitializer
 */
@Component
public final class EmailLinkerInitializer extends AbstractModuleInitializer {

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(EmailLinkerFields.EMAIL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        if (isServiceEnabled()) {
            EmailCoreInitializer.getInstance().stop();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            EmailAddOnService.init(getProperties(EmailLinkerFields.EMAIL).getNestedConfigs(EmailLinkerFields.ADD_ON_SERVICES));
        }
        LOGGER.info("Application Initializer: Email-Core module initialization completed");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        if (isServiceEnabled()) {
            EmailCoreInitializer.getInstance().postInitialize(getProperties(EmailLinkerFields.EMAIL).getNestedConfigs(EmailLinkerFields.EXECUTOR));
        }
        LOGGER.info("Application Initializer: Email-Core module post initialization completed");
    }
}
