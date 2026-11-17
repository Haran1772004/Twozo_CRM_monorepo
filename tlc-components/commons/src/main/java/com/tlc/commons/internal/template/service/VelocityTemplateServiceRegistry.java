package com.tlc.commons.internal.template.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Registers the {@link VelocityTemplateService} into the registry
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class VelocityTemplateServiceRegistry implements ServiceRegistry<VelocityTemplateService> {

    private final AtomicReference<VelocityTemplateService> velocityTemplateService;

    private VelocityTemplateServiceRegistry() {
        velocityTemplateService = new AtomicReference<>();
    }

    private static class InstanceHolder {
        private static final VelocityTemplateServiceRegistry VELOCITY_TEMPLATE_SERVICE_REGISTRY = new VelocityTemplateServiceRegistry();
    }

    /**
     * <p>
     * Gets the {@link VelocityTemplateServiceRegistry} instance
     * </p>
     *
     * @return The {@link VelocityTemplateServiceRegistry}
     */
    public static VelocityTemplateServiceRegistry getInstance() {
        return InstanceHolder.VELOCITY_TEMPLATE_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final VelocityTemplateService service) {
        if (!velocityTemplateService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Velocity template service not initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link VelocityTemplateService}
     */
    @Override
    public VelocityTemplateService get() {
        return velocityTemplateService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link VelocityTemplateService}
     */
    @Override
    public VelocityTemplateService unregister() {
        return velocityTemplateService.getAndSet(null);
    }
}
