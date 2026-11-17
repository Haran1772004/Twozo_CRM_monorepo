package com.tlc.email.starter.initializer;

import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.email.client.initializer.EmailClientServiceInitializer;

import com.tlc.email.server.handler.OutlookEmailTokenRefreshHandler;
import com.tlc.email.starter.fields.EmailStarterFields;
import com.tlc.sync.service.oauth.internal.RefreshTokenHandlerRegistry;
import com.tlc.sync.service.oauth.model.SyncServiceCategory;
import org.springframework.stereotype.Component;

/**
 * <p>
 * Initialization process is triggered during the application startup. Ensure that
 * all required properties and configurations for the email service are defined
 * before the application context is loaded to prevent initialization errors.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see AbstractModuleInitializer
 */
@Component
public class EmailServiceInitializer extends AbstractModuleInitializer {

    private static final String EMAIL_SERVICE = "email-service";

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        EmailClientServiceInitializer.getInstance().preInitialize(getProperties(EmailStarterFields.NOTIFICATION));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void postInitialize() {
        EmailClientServiceInitializer.getInstance().postInitialize(getProperties(EmailStarterFields.EMAIL));
        RefreshTokenHandlerRegistry.getInstance().put(SyncServiceCategory.EMAIL, OutlookEmailTokenRefreshHandler.getInstance());
    }

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
        EmailClientServiceInitializer.getInstance().stop();
    }
}
