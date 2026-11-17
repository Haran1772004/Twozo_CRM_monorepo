package com.tlc.crm.fields;

import com.tlc.commons.service.TlcActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 *     Activator of fields features bundle
 * </p>
 * @author KarthickV
 * @version 1.0
 */
public class Activator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    @Override
    public void bundleStart(final BundleContext bundleContext) throws Exception {
        LOGGER.info("fields module started");
    }

    @Override
    public void bundleStop(final BundleContext bundleContext) throws Exception {
        LOGGER.info("fields module stopped");
    }

    @Override
    public void preRequisiteBundlesLoaded(final BundleContext bundleContext) {

    }

    @Override
    public void allBundlesLoaded(BundleContext bundleContext) {

    }

}