package com.tlc.i18n.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.i18n.I18nService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link I18nService} into the registry.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class I18nServiceRegistry implements ServiceRegistry<I18nService> {

    private final AtomicReference<I18nService> i18nService;

    private static final class Instance {
        private static final I18nServiceRegistry I18N_SERVICE_REGISTRY = new I18nServiceRegistry();
    }

    private I18nServiceRegistry() {
        this.i18nService = new AtomicReference<>();
    }

    public static ServiceRegistry<I18nService> getInstance() {
        return Instance.I18N_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final I18nService service) {
        if (!i18nService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public I18nService get() {
        return i18nService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link I18nService}.
     */
    @Override
    public I18nService unregister() {
        return i18nService.getAndSet(null);
    }
}
