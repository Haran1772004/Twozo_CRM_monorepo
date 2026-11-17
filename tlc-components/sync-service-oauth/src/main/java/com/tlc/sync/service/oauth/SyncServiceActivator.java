package com.tlc.sync.service.oauth;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Activates the bundle and provides the access to the sync services.
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class SyncServiceActivator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncServiceActivator.class);

    /**
     * {@inheritDoc}
     *
     * @param context Represents the {@link BundleContext}
     */
    public void start(final BundleContext context) {
        LOGGER.info("Sync service: Starting the bundle");
    }

    /**
     * {@inheritDoc}
     *
     * @param context Represents the {@link BundleContext}
     */
    public void stop(final BundleContext context) {
        LOGGER.info("Sync service: Stopping the bundle");
    }

}