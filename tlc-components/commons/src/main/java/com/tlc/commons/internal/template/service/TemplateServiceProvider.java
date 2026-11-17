package com.tlc.commons.internal.template.service;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import java.util.Objects;

/**
 * <p>
 * Provides the services that enable the {@link VelocityTemplateService} to execute its functions effectively
 * </p>
 *
 * @author Kishore J
 * @version 1.0
 */
public final class TemplateServiceProvider {

    private static final ServiceRegistry<VelocityTemplateService> VELOCITY_TEMPLATE_SERVICE_REGISTRY;

    static {
        VELOCITY_TEMPLATE_SERVICE_REGISTRY = VelocityTemplateServiceRegistry.getInstance();
    }

    /**
     * <p>
     * Fetches the {@link VelocityTemplateService}
     * </p>
     *
     * @return the {@link VelocityTemplateService}
     */
    public static VelocityTemplateService getVelocityTemplateService() {
        final VelocityTemplateService velocityTemplateService = VELOCITY_TEMPLATE_SERVICE_REGISTRY.get();

        if (Objects.isNull(velocityTemplateService)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Velocity template service not initialized");
        }
        return velocityTemplateService;
    }
}
