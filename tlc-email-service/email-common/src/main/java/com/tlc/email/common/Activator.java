package com.tlc.email.common;

import com.tlc.commons.service.TlcActivator;

import com.tlc.email.common.initializer.CommonModuleInitializer;

import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Serves and enables for the configuration and initialization of specific components or services within the application.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see TlcActivator
 */
public class Activator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private final CommonModuleInitializer commonModuleInitializer = CommonModuleInitializer.getInstance();

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) throws Exception {
        LOGGER.info("Email common module started");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) throws Exception {
        commonModuleInitializer.stop();
        LOGGER.info("Email common module stopped");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void preRequisiteBundlesLoaded(final BundleContext bundleContext) {
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void allBundlesLoaded(final BundleContext bundleContext) {
        commonModuleInitializer.postInitialize(null);
    }

}
