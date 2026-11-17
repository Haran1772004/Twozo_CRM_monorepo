package com.tlc.crm.icon;

import com.tlc.commons.service.TlcActivator;

import com.tlc.crm.icon.initializer.IconServiceInitializer;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Activates the icon-related functionalities and encapsulates the logic involved in managing currencies
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class IconActivator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(IconActivator.class);

    private IconServiceInitializer iconServiceInitializer;

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        LOGGER.info("Icon module started");
        iconServiceInitializer = IconServiceInitializer.getInstance();
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being stopped.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) {
        LOGGER.info("Icon module stopped");
        iconServiceInitializer.stop();
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started
     */
    @Override
    public void preRequisiteBundlesLoaded(final BundleContext bundleContext) {
        LOGGER.info("Prerequisite startup activities done {}", bundleContext.getBundle().getSymbolicName());
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started
     */
    @Override
    public void allBundlesLoaded(final BundleContext bundleContext) {
        LOGGER.info("Processes done after all bundles are loaded for {}", bundleContext.getBundle().getSymbolicName());
        iconServiceInitializer.postInitialize(null);
    }
}