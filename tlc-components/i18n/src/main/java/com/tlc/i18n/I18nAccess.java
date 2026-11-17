package com.tlc.i18n;

import com.tlc.commons.service.registry.ServiceRegistry;
import com.tlc.i18n.internal.I18nServiceImpl;
import com.tlc.i18n.internal.registry.I18nServiceRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * @author Abishek
 * @version 1.0
 */
public final class I18nAccess implements BundleActivator
{
    private final ServiceRegistry<I18nService> i18nServiceRegistry = I18nServiceRegistry.getInstance();

    private I18nResResolver i18nResResolver;

    @Override
    public void start(BundleContext bundleContext) throws Exception
    {
        final I18nServiceImpl i18nService = new I18nServiceImpl();

        i18nService.start(null);
        this.i18nResResolver = new I18nResResolver(i18nService);
        bundleContext.addBundleListener(i18nResResolver);
        i18nServiceRegistry.register(i18nService);
    }

    @Override
    public void stop(BundleContext bundleContext)
    {
        bundleContext.removeBundleListener(i18nResResolver);
        final I18nServiceImpl i18nService = (I18nServiceImpl) i18nServiceRegistry.unregister();
        i18nService.stop();
    }
}
