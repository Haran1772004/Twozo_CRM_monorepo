package com.tlc.crm.messenger;

import com.tlc.crm.messenger.initializer.MessengerServiceInitializer;
import com.tlc.commons.service.TlcActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Messenger module activator.
 * </p>
 *
 * @author Ajay.
 * @version 1.0.
 */
public class MessengerActivator extends TlcActivator {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessengerActivator.class);

    private MessengerServiceInitializer messengerServiceInitializer;

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        messengerServiceInitializer = MessengerServiceInitializer.getInstance();

        messengerServiceInitializer.preInitialize(null);
        LOGGER.info("Messenger module started");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) {
        messengerServiceInitializer.stop();
        LOGGER.info("Messenger module stopped");
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
        messengerServiceInitializer.postInitialize(null);
        LOGGER.info("Activities done after all bundles are loaded for {}", bundleContext.getBundle().getSymbolicName());
    }
}
