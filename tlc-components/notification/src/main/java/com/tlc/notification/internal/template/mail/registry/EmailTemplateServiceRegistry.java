package com.tlc.notification.internal.template.mail.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.notification.internal.template.mail.service.VelocityEmailTemplateService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 * Registers the {@link VelocityEmailTemplateService} into the registry
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public class EmailTemplateServiceRegistry implements ServiceRegistry<VelocityEmailTemplateService> {

    private final AtomicReference<VelocityEmailTemplateService> velocityEmailTemplateService;

    private EmailTemplateServiceRegistry() {
        velocityEmailTemplateService = new AtomicReference<>();
    }

    private static class InstanceHolder {
        private static final EmailTemplateServiceRegistry VELOCITY_TEMPLATE_SERVICE_REGISTRY = new EmailTemplateServiceRegistry();
    }

    /**
     * <p>
     * Gets the {@link EmailTemplateServiceRegistry} instance
     * </p>
     *
     * @return The {@link EmailTemplateServiceRegistry}
     */
    public static EmailTemplateServiceRegistry getInstance() {
        return EmailTemplateServiceRegistry.InstanceHolder.VELOCITY_TEMPLATE_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final VelocityEmailTemplateService service) {
        if (!velocityEmailTemplateService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Velocity template service not initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link VelocityEmailTemplateService}
     */
    @Override
    public VelocityEmailTemplateService get() {
        return velocityEmailTemplateService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link VelocityEmailTemplateService}
     */
    @Override
    public VelocityEmailTemplateService unregister() {
        return velocityEmailTemplateService.getAndSet(null);
    }
}
