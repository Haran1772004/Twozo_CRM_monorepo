package com.tlc.commons;

import com.tlc.commons.util.ConfHolder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * <p>
 * Activates the bundle and provides the access to the commons service.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public final class CommonsAccess implements BundleActivator {

    /**
     * {@inheritDoc}
     *
     * @param bundleContext Represents the {@link BundleContext}
     */
    @Override
    public void start(final BundleContext bundleContext) {
        ConfHolder.getInstance().loadConfigFromFiles();
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext Represents the {@link BundleContext}
     */
    @Override
    public void stop(final BundleContext bundleContext) {
    }
}
