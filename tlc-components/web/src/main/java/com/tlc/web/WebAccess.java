package com.tlc.web;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;
import com.tlc.web.internal.registry.WebServiceRegistry;
import com.tlc.web.undertow.UndertowService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Abishek
 * @version 1.0
 */
public final class WebAccess implements BundleActivator
{
    private final ServiceRegistry<WebService> webServiceRegistry = WebServiceRegistry.getInstance();

    private BundleListener bundleListener;

    @Override
    public void start(final BundleContext bundleContext)
    {
        final UndertowService webService = new UndertowService();
        final Configurations configurations = ConfHolder.getInstance().getProperties("web");

        webService.start(configurations);
        this.bundleListener = new BundleListener(webService);
        webServiceRegistry.register(webService);
        bundleContext.addBundleListener(bundleListener);
    }

    @Override
    public void stop(BundleContext bundleContext)
    {
        final UndertowService webService = (UndertowService) webServiceRegistry.unregister();
        webService.stop();
        bundleContext.removeBundleListener(bundleListener);
    }
}
