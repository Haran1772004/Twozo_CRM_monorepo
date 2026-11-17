package com.tlc.crm.hospital;

import com.tlc.commons.service.TlcActivator;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator of the hospital bundle.
 *
 * @author Haris
 * @version 1.0
 */
/*
 * Yes. If the hospital module needs setup or cleanup, it can do that when it
 * starts or stops—for example, register a listener at startup and unregister it
 * at shutdown.
 * 
 * Normal work, like creating a patient or appointment, should happen when the
 * app receives a request, not in the activator. Also, since we haven’t wired
 * the module into the server, this activator won’t run yet.
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
