package com.tlc.web.client;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.web.client.internal.BundleListener;
import com.tlc.web.client.internal.VertxClient;
import com.tlc.web.client.internal.registry.WebClientServiceRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Objects;

/**
 * An activator that is used to activates the web client access
 *
 * @author Selvakumar
 * @version 1.0
 */
public class WebClientAccess implements BundleActivator {

    private final ServiceRegistry<VertxClient> webClientServiceRegistry;

    public WebClientAccess() {
        webClientServiceRegistry = WebClientServiceRegistry.getInstance();
    }

    /**
     * Starts the web client
     *
     * @param bundleContext
     * @throws Exception
     */
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        final VertxClient webClientService = new VertxClient();

        webClientService.start(null);
        webClientServiceRegistry.register(webClientService);
        bundleContext.addBundleListener(new BundleListener());
    }

    /**
     * Stops the web client
     *
     * @param bundleContext
     * @throws Exception
     */
    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        final VertxClient webClientService = webClientServiceRegistry.unregister();

        if (Objects.nonNull(webClientService)) {
            webClientService.stop();
        }
    }

}
