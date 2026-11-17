package com.tlc.components.starter.initializer.components;

import com.tlc.components.starter.fields.ComponentsStarterFields;
import com.tlc.components.starter.initializer.AbstractModuleInitializer;

import com.tlc.identity.service.IdentityIntegrationInitializationService;

import com.tlc.commons.service.Service;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.identity.service.oidc.internal.registry.IdentityIntegrationInitializationServiceRegistry;

import org.springframework.stereotype.Component;

/**
 * <p>
 * Responsible for initiating the initialization process of external identity services.
 * </p>
 *
 * @author  Mohammed Asif J.
 * @version 1.0
 */
@Component
public class IdentityServiceInitializer extends AbstractModuleInitializer {

    private final ServiceRegistry<IdentityIntegrationInitializationService> serviceRegistry;

    public IdentityServiceInitializer() {
        this.serviceRegistry = IdentityIntegrationInitializationServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isServiceEnabled() {
        return isServiceEnabled(ComponentsStarterFields.EXTERNAL_IDENTITY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ServiceRegistry<? extends Service> getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() {
        if (isServiceEnabled()) {
            final IdentityIntegrationInitializationService service = IdentityIntegrationInitializationService.getInstance();

            service.start(getProperties(ComponentsStarterFields.EXTERNAL_IDENTITY));
            serviceRegistry.register(service);
        }
    }
}
