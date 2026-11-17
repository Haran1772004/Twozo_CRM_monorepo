package com.tlc.crm.email.core;

import com.tlc.crm.email.core.initializer.EmailCoreInitializer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Activator responsible for initializing and managing the email core activator
 * </p>
 *
 * @author NandiniRakAS
 * @version 1.0
 */
public class EmailCoreActivator implements BundleActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailCoreActivator.class);

    private EmailCoreInitializer emailCoreInitializer;

    public void start(final BundleContext context) {
        LOGGER.info("Email core service bundle started");
        emailCoreInitializer = EmailCoreInitializer.getInstance();

        emailCoreInitializer.preInitialize(null);
    }

    public void stop(final BundleContext context) {
        emailCoreInitializer.stop();
        LOGGER.info("Email core service bundle stopped");
    }

}