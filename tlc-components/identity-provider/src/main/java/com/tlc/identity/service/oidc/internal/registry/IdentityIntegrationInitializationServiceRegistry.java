package com.tlc.identity.service.oidc.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.identity.service.IdentityIntegrationInitializationService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link IdentityIntegrationInitializationService} into the registry.
 *
 * @author Sathish
 * @version 1.0
 */
public final class IdentityIntegrationInitializationServiceRegistry implements ServiceRegistry<IdentityIntegrationInitializationService> {

    private final AtomicReference<IdentityIntegrationInitializationService> serviceAtomicReference;

    private static final class Instance {
        private static final IdentityIntegrationInitializationServiceRegistry SERVICE_REGISTRY = new IdentityIntegrationInitializationServiceRegistry();
    }

    private IdentityIntegrationInitializationServiceRegistry() {
        this.serviceAtomicReference = new AtomicReference<>();
    }

    public static ServiceRegistry<IdentityIntegrationInitializationService> getInstance() {
        return Instance.SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final IdentityIntegrationInitializationService service) {
        if (!serviceAtomicReference.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public IdentityIntegrationInitializationService get() {
        return serviceAtomicReference.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link IdentityIntegrationInitializationService}.
     */
    @Override
    public IdentityIntegrationInitializationService unregister() {
        return serviceAtomicReference.getAndSet(null);
    }
}
