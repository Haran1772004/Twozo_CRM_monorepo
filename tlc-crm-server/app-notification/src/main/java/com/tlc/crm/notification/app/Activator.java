package com.tlc.crm.notification.app;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * <p>
 *     Activator of app notification features bundle
 * </p>
 * @author KarthickV
 * @version 1.0
 */
public class Activator implements BundleActivator {

    public void start(BundleContext context) {
        System.out.println("Starting the bundle");
    }

    public void stop(BundleContext context) {
        System.out.println("Stopping the bundle");
    }

}