package com.tlc.l10n;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Activator responsible for initializing and managing the localization
 * </p>
 *
 * @author Kishore
 * @version 1.0
 */
public class L10NAccess implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(L10NAccess.class);

    @Override
    public void start(final BundleContext context) {
        LOGGER.info("L10N module started");
    }

    @Override
    public void stop(final BundleContext context) {
        LOGGER.info("L10N module stopped");
    }
}
