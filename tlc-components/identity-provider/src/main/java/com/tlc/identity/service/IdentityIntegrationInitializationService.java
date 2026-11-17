package com.tlc.identity.service;

import com.tlc.commons.service.Service;
import com.tlc.identity.service.oidc.fields.OIDCFields;
import com.tlc.identity.service.oidc.provider.OIDCIdentityServiceProvider;
import com.tlc.commons.util.Configurations;

/**
 * <p>
 * Service responsible for initializing and configuring identity providers
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
public class IdentityIntegrationInitializationService implements Service {

    private IdentityIntegrationInitializationService() {
    }

    private static class InstanceHolder {
        private static final IdentityIntegrationInitializationService INSTANCE = new IdentityIntegrationInitializationService();
    }

    public static IdentityIntegrationInitializationService getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * {@inheritDoc}
     *
     * @param configurations Represents the app configurations
     */
    @Override
    public void start(final Configurations configurations) {
        OIDCIdentityServiceProvider.getInstance()
                .setProperties(configurations.getNestedConfigs(OIDCFields.OIDC));
    }

    @Override
    public void stop() {
        // No shutdown actions required
    }
}
