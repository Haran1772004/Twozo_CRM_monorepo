package com.tlc.crm.hospital;

import com.tlc.commons.service.TlcActivator;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages lifecycle logging for the hospital OSGi bundle.
 *
 * @author Haris
 * @version 1.0
 */
public class HospitalActivator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HospitalActivator.class);

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        LOGGER.info("Hospital module started");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being stopped.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) {
        LOGGER.info("Hospital module stopped");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void preRequisiteBundlesLoaded(final BundleContext bundleContext) {
        LOGGER.info("Prerequisite startup activities done for {}", bundleContext.getBundle().getSymbolicName());
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void allBundlesLoaded(final BundleContext bundleContext) {
        LOGGER.info("Processes done after all bundles are loaded for {}", bundleContext.getBundle().getSymbolicName());
    }
}
