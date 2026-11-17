package com.tlc.crm.core;

import com.tlc.commons.service.TlcActivator;

import com.tlc.commons.util.ConfHolder;
import com.tlc.crm.core.initializer.CoreServiceInitializer;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Abishek
 * @version 1.0
 */
public class Activator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private CoreServiceInitializer coreServiceInitializer;

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        LOGGER.info("Core module started");
        coreServiceInitializer = CoreServiceInitializer.getInstance();
        bundleContext.addBundleListener(new CoreBundleListener() {});

        coreServiceInitializer.preInitialize(null);
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) {
        coreServiceInitializer.stop();
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
        coreServiceInitializer.postInitialize(ConfHolder.getInstance().getProperties("core"));
    }
}
