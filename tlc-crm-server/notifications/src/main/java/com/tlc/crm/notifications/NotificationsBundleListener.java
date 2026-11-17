package com.tlc.crm.notifications;

import com.tlc.commons.service.listener.BundleListener;

import org.osgi.framework.Bundle;

/**
 * <p>
 * Listener is responsible for handling {@link Bundle} events, including activation and deactivation,
 * for instances within the notification module
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class NotificationsBundleListener extends BundleListener {

    NotificationsBundleListener() {
    }

    /**
     * <p>
     * Registers the required actions which are present in the bundle
     * </p>
     *
     * @param bundle started bundle
     */
    @Override
    public void bundleStarted(final Bundle bundle) {
    }

    /**
     * <p>
     * Clears the actions which have been registered by the bundle
     * </p>
     *
     * @param bundle stopped bundle
     */
    @Override
    public void bundleStopped(final Bundle bundle) {
    }
}
