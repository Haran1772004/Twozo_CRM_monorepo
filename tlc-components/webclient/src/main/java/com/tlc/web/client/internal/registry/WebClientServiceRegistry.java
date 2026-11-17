package com.tlc.web.client.internal.registry;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.web.client.internal.VertxClient;

import io.vertx.ext.web.client.WebClient;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the {@link VertxClient} into the registry.
 *
 * @author Sathish
 * @version 1.0
 */
public final class WebClientServiceRegistry implements ServiceRegistry<VertxClient> {

    private final AtomicReference<VertxClient> webClientService;

    private static final class Instance {
        private static final WebClientServiceRegistry WEB_CLIENT_SERVICE_REGISTRY = new WebClientServiceRegistry();
    }

    private WebClientServiceRegistry() {
        this.webClientService = new AtomicReference<>();
    }

    public static ServiceRegistry<VertxClient> getInstance() {
        return Instance.WEB_CLIENT_SERVICE_REGISTRY;
    }

    /**
     * {@inheritDoc}
     *
     * @param vertxClient The service to register.
     */
    @Override
    public void register(final VertxClient vertxClient) {
        if (!webClientService.compareAndSet(null, vertxClient)) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service already initialized");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public VertxClient get() {
        return webClientService.get();
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link WebClient}.
     */
    @Override
    public VertxClient unregister() {
        return webClientService.getAndSet(null);
    }
}
