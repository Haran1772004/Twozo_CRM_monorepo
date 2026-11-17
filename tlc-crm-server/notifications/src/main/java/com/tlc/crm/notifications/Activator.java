package com.tlc.crm.notifications;

import com.tlc.commons.service.TlcActivator;

import com.tlc.crm.notifications.initializer.NotificationServiceInitializer;

import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Activator of CRM notification features bundle
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class Activator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);
    private NotificationServiceInitializer notificationServiceInitializer;

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        notificationServiceInitializer = NotificationServiceInitializer.getInstance();

        bundleContext.addBundleListener(new NotificationsBundleListener());
        notificationServiceInitializer.preInitialize(null);
        LOGGER.info("notification module started");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) {
        notificationServiceInitializer.stop();
        LOGGER.info("notification module stopped");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void preRequisiteBundlesLoaded(final BundleContext bundleContext) {
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void allBundlesLoaded(final BundleContext bundleContext) {
        notificationServiceInitializer.postInitialize(null);
    }
}