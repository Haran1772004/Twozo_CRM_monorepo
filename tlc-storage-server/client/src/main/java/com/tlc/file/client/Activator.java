package com.tlc.file.client;

import com.tlc.commons.service.TlcActivator;
import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator of file client bundle.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class Activator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception Exception.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) throws Exception {
        LOGGER.info("File client module started");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception Exception.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) throws Exception {
        LOGGER.info("File client module stopped");
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
    }

}