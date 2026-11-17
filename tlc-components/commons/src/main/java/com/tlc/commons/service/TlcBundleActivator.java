package com.tlc.commons.service;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Provides Activator and callBack services.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public interface TlcBundleActivator extends BundleActivator {

    /**
     * Activities to be done after all prerequisite bundles are started.
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    void preRequisiteBundlesLoaded(final BundleContext bundleContext);

    /**
     * Activities to be done after all bundles are started.
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    void allBundlesLoaded(final BundleContext bundleContext);
}
