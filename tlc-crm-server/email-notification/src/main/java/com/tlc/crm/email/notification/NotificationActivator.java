package com.tlc.crm.email.notification;

import com.tlc.commons.service.TlcActivator;

import com.tlc.crm.email.notification.initializer.EmailNotificationServiceInitializer;

import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Responsible for activating and managing notification services within the TLC framework.
 * </p>
 *
 * @author KowsalyaSP
 * @version 1.0
 * @see TlcActivator
 */
public class NotificationActivator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationActivator.class);
    private final EmailNotificationServiceInitializer emailNotificationServiceInitializer = EmailNotificationServiceInitializer.getInstance();

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) throws Exception {
        emailNotificationServiceInitializer.preInitialize(null);
        LOGGER.info("Notification module started");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) throws Exception {
        emailNotificationServiceInitializer.stop();
        LOGGER.info("Notification module stoped");
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
     * @throws Exception
     */
    @Override
    public void allBundlesLoaded(final BundleContext bundleContext) {
        emailNotificationServiceInitializer.postInitialize(null);
    }
}