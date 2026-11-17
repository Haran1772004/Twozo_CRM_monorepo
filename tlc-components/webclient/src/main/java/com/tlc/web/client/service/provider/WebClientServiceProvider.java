package com.tlc.web.client.service.provider;

import com.tlc.commons.code.ErrorCode;
import com.tlc.commons.code.ErrorCodes;
import com.tlc.commons.service.registry.ServiceRegistry;

import com.tlc.web.client.internal.VertxClient;
import com.tlc.web.client.internal.registry.WebClientServiceRegistry;

/**
 * Provides the services that enable the {@link VertxClient} to execute its functions effectively.
 *
 * @author Sathish
 * @version 1.0
 */
public final class WebClientServiceProvider {

    private static final ServiceRegistry<VertxClient> WEB_CLIENT_SERVICE_REGISTRY;

    static {
        WEB_CLIENT_SERVICE_REGISTRY = WebClientServiceRegistry.getInstance();
    }

    private WebClientServiceProvider() {
    }

    /**
     * {@inheritDoc}
     *
     * @return The {@link VertxClient}.
     */
    public static VertxClient get() {
        final VertxClient service = WEB_CLIENT_SERVICE_REGISTRY.get();

        if (service == null) {
            throw ErrorCode.get(ErrorCodes.INVALID_ACCESS, "Service not initialized");
        } else {
            return service;
        }
    }

    /**
     * Whether clustered event-bus traffic should be sent (excludes recovery/reboot windows).
     * Product probes and callers should use this gate instead of internal recovery flags.
     */
    public static boolean isClusterReadyForTraffic() {
        final VertxClient service = WEB_CLIENT_SERVICE_REGISTRY.get();
        return service != null && service.isClusterReadyForTraffic();
    }
}
