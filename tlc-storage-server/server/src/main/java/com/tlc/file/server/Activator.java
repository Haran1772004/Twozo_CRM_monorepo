package com.tlc.file.server;

import com.tlc.commons.service.TlcActivator;
import com.tlc.commons.util.ConfHolder;

import com.tlc.file.server.initializer.FileServerInitializer;

import org.osgi.framework.BundleContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Activator of file server bundle.
 *
 * @author ThalaimalaiPandiyanT
 * @version 1.0
 */
public class Activator extends TlcActivator {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private final FileServerInitializer fileServerInitializer = FileServerInitializer.getInstance();

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception
     */
    @Override
    public void bundleStart(final BundleContext bundleContext) throws Exception {
        fileServerInitializer.preInitialize(ConfHolder.getInstance().getProperties("upload-executor"));
        LOGGER.info("File server module started");
    }

    /**
     * {@inheritDoc}
     *
     * @param bundleContext The execution context of the bundle being started.
     * @throws Exception
     */
    @Override
    public void bundleStop(final BundleContext bundleContext) throws Exception {
        fileServerInitializer.stop();
        LOGGER.info("File server module stopped");
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
        fileServerInitializer.postInitialize(null);
    }

}
