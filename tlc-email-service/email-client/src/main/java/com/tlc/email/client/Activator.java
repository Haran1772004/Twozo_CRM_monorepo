package com.tlc.email.client;

import com.tlc.commons.service.TlcActivator;

import com.tlc.email.client.initializer.EmailClientServiceInitializer;

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

    private final EmailClientServiceInitializer emailInitializer = EmailClientServiceInitializer.getInstance();

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception Exception.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) throws Exception {
        LOGGER.info("Email client module started");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception Exception.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) throws Exception {
        emailInitializer.stop();
        LOGGER.info("Email client module stopped");
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
        emailInitializer.postInitialize(null);
    }

}