package com.tlc.crm.entity;

import com.tlc.commons.service.TlcActivator;
import com.tlc.commons.util.ConfHolder;

import com.tlc.crm.entity.initializer.EntityServiceInitializer;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Abishek
 * @version 1.0
 */
public class Activator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private EntityServiceInitializer entityServiceInitializer;

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) {
        LOGGER.info("Fields activator initiated");
        entityServiceInitializer = EntityServiceInitializer.getInstance();

        bundleContext.addBundleListener(new EntityBundleListener());
        entityServiceInitializer.preInitialize(ConfHolder.getInstance().getProperties("upload-executor"));
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) {
        LOGGER.info("Clearing entity cache");
        entityServiceInitializer.stop();
        LOGGER.info("Entity bundle activator stopped");
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
        LOGGER.info("Activities done after all bundles are loaded for {}", bundleContext.getBundle().getSymbolicName());
        entityServiceInitializer.postInitialize(null);
    }

}
