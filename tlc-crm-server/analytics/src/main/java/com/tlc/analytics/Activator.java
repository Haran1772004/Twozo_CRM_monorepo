package com.tlc.analytics;

import com.tlc.analytics.initializer.AnalyticsServiceInitializer;
import com.tlc.commons.service.TlcActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Activator of analytics features bundle
 * </p>
 *
 * @author Aswath
 * @version 1.0
 */
public class Activator extends TlcActivator
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private AnalyticsServiceInitializer analyticsServiceInitializer;


    @Override
    public void bundleStart( final BundleContext bundleContext ) throws Exception
    {
        analyticsServiceInitializer = AnalyticsServiceInitializer.getInstance();

        analyticsServiceInitializer.preInitialize( null );
        LOGGER.info("Analytics: Activator - Module started");
    }

    @Override
    public void bundleStop( final BundleContext bundleContext ) throws Exception
    {
        analyticsServiceInitializer.stop();
        LOGGER.info("Analytics: Activator - Module stopped");
    }

    @Override
    public void preRequisiteBundlesLoaded( final BundleContext bundleContext )
    {
        LOGGER.info("Analytics: Activator - Prerequisite startup activities done: {}", bundleContext.getBundle().getSymbolicName());
    }

    @Override
    public void allBundlesLoaded( final BundleContext bundleContext )
    {
        analyticsServiceInitializer.postInitialize( null );
        LOGGER.info("Analytics: Activator - Activities done after all bundles loaded: {}", bundleContext.getBundle().getSymbolicName());
    }
}
