package com.tlc.notification.internal.template.mail.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.notification.internal.template.mail.registry.EmailTemplateServiceRegistry;

import java.util.Objects;

/**
 * <p>
 * Provides the services that enable the {@link VelocityEmailTemplateService} to execute its functions effectively
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class VelocityEmailTemplateServiceProvider {

    private static final ServiceRegistry<VelocityEmailTemplateService> VELOCITY_TEMPLATE_SERVICE_REGISTRY;

    static {
        VELOCITY_TEMPLATE_SERVICE_REGISTRY = EmailTemplateServiceRegistry.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link VelocityEmailTemplateService}
     * </p>
     *
     * @return the {@link VelocityEmailTemplateService}
     */
    public static VelocityEmailTemplateService getVelocityMailTemplateService() {
        final VelocityEmailTemplateService velocityTemplateService = VELOCITY_TEMPLATE_SERVICE_REGISTRY.get();

        if (Objects.isNull(velocityTemplateService)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Velocity template service not initialized");
        }
        return velocityTemplateService;
    }
}
