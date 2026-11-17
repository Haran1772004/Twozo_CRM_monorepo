package com.tlc.crm.common;

import com.tlc.commons.service.TlcActivator;
import com.tlc.commons.util.ConfHolder;
import com.tlc.commons.util.Configurations;

import com.tlc.crm.common.initializer.CommonServiceInitializer;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Abishek
 * @version 1.0
 */
public class CommonActivator extends TlcActivator
{

    private static final String COMMON_CONFIG_FILE_NAME = "crm-common";
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonActivator.class);

    private CommonServiceInitializer commonServiceInitializer;

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        LOGGER.info("Common bundle activator started");
        bundleContext.addBundleListener(new BundleListener());
        final Configurations commonConfigs = ConfHolder.getInstance().getProperties(COMMON_CONFIG_FILE_NAME);
        commonServiceInitializer = CommonServiceInitializer.getInstance();

        commonServiceInitializer.preInitialize(commonConfigs);
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) {
        commonServiceInitializer.stop();
        LOGGER.info("Common bundle stopped");
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
        commonServiceInitializer.postInitialize(null);
    }
}