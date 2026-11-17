package com.tlc.iam.common;

import com.tlc.commons.service.TlcActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An activator that activates the IAM common module
 *
 * @author Selvakumar
 * @version 1.0
 */
public class CommonActivator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonActivator.class);

    @Override
    public void bundleStart(final BundleContext bundleContext) throws Exception {
    }

    @Override
    public void bundleStop(final BundleContext bundleContext) throws Exception {
    }

    @Override
    public void preRequisiteBundlesLoaded(final BundleContext bundleContext) {

    }

    @Override
    public void allBundlesLoaded(final BundleContext bundleContext) {

    }
}
