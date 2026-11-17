package com.tlc.commons.service;

import com.tlc.commons.internal.BundleContainerImpl;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * Manages the {@link Bundle} startup activities.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public abstract class TlcActivator implements TlcBundleActivator {

    private final BundleContainerImpl bundleContainer;

    public TlcActivator() {
        this.bundleContainer = BundleContainerImpl.getInstance();
    }

    /**
     * Called when {@link Bundle} is started so the Framework can perform the
     * bundle-specific activities necessary to start this bundle.
     *
     * @param bundleContext the execution context of the bundle being started.
     * @throws Exception Exception.
     */
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        bundleContainer.setBundleStore(bundleContext, this);
        bundleStart(bundleContext);
    }

    /**
     * Called when {@link Bundle} is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception Exception.
     */
    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        bundleStop(bundleContext);
    }

    /**
     * Performs the {@link Bundle} startup activities.
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception Exception.
     */
    public abstract void bundleStart(BundleContext bundleContext) throws Exception;

    /**
     * Performs the activities when the {@link Bundle} is being stopped.
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception Exception.
     */
    public abstract void bundleStop(BundleContext bundleContext) throws Exception;

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    public abstract void preRequisiteBundlesLoaded(final BundleContext bundleContext);

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    public abstract void allBundlesLoaded(final BundleContext bundleContext);
}
