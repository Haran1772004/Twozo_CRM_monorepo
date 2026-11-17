package com.tlc.iam.client;

import com.tlc.commons.service.TlcActivator;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator of IAM client bundle
 *
 * @author Selvakumar
 * @version 1.0
 */
public class IamClientAccess extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(IamClientAccess.class);

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        LOGGER.info("IAM client module activated");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being stopped.
     */
    @Override
    public void bundleStop(BundleContext bundleContext) throws Exception {
        LOGGER.info("IAM client module stopped");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void preRequisiteBundlesLoaded(BundleContext bundleContext) {

    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void allBundlesLoaded(BundleContext bundleContext) {

    }

}
