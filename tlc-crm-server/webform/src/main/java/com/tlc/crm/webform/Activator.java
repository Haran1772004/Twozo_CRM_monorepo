package com.tlc.crm.webform;

import com.tlc.commons.service.TlcActivator;
import com.tlc.commons.util.ConfHolder;

import com.tlc.commons.service.initializer.ServiceInitializer;

import com.tlc.crm.webform.initializer.WebFormServiceInitializer;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator of WebForm Bundle.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class Activator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ServiceInitializer webFormServiceInitializer;

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        webFormServiceInitializer = WebFormServiceInitializer.getInstance();

        webFormServiceInitializer.preInitialize(ConfHolder.getInstance().getProperties("webform"));
        LOGGER.info("WebForm module started");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) {
        LOGGER.info("WebForm module stopped");
        webFormServiceInitializer.stop();
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void preRequisiteBundlesLoaded(final BundleContext bundleContext) {
        LOGGER.info("Prerequisite startup activities done {}", bundleContext.getBundle().getSymbolicName());
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void allBundlesLoaded(final BundleContext bundleContext) {
        LOGGER.info("Activities done after all bundles are loaded for {}", bundleContext.getBundle().getSymbolicName());
    }

}