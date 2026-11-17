package com.tlc.web.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.web.WebService;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link WebService} into the registry.
 *
 * @author Sathish
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public final class WebServiceRegistry implements ServiceRegistry<WebService> {

    private final AtomicReference<WebService> webService;

    private static final class Instance {
        private static final WebServiceRegistry WEB_SERVICE_REGISTRY = new WebServiceRegistry();
    }

    private WebServiceRegistry() {
        this.webService = new AtomicReference<>();
    }

    public static ServiceRegistry<WebService> getInstance() {
        return Instance.WEB_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param service The service to register.
     */
    @Override
    public void register(final WebService service) {
        if (!webService.compareAndSet(null, service)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public WebService get() {
        return webService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link WebService}.
     */
    @Override
    public WebService unregister() {
        return webService.getAndSet(null);
    }
}
