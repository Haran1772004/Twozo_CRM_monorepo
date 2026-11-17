package com.tlc.i18n.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.i18n.I18nService;
import com.tlc.i18n.internal.registry.I18nServiceRegistry;

/**
 * Provides the services that enable the {@link I18nService} to execute its functions effectively.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class I18nServiceProvider {

    private static final ServiceRegistry<I18nService> I18N_SERVICE_REGISTRY;

    static {
        I18N_SERVICE_REGISTRY = I18nServiceRegistry.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @return The instance of {@link I18nService}.
     */
    public static I18nService get() {
        final I18nService service = I18N_SERVICE_REGISTRY.get();

        if (service == null) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }
}
