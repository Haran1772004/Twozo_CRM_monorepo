package com.tlc.web.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.web.WebService;
import com.tlc.web.internal.registry.WebServiceRegistry;

/**
 * Provides the services that enable the {@link WebService} to execute its functions effectively.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WebServiceProvider {

    private static final ServiceRegistry<WebService> WEB_SERVICE_REGISTRY;

    static {
        WEB_SERVICE_REGISTRY = WebServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link WebService}.
     */
    public static WebService get() {
        final WebService service = WEB_SERVICE_REGISTRY.get();

        if (service == null) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }
}
